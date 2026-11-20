'use strict';

/*
 * central-client.js — the only door between the Control Center and the
 * TENSANTS GROUP central services.
 *
 * The renderer runs behind a strict CSP (connect-src 'self'), so it can never
 * reach the network itself. Every call therefore goes
 * main process -> central API and comes back through IPC.
 *
 * The shell bundles no sample data: when the central API is unreachable the
 * calls fail cleanly and the renderer shows its empty state.
 *
 * Reading the platform overview is a privileged act, so the two halves of the
 * connection live here together: sign-in against the Supabase project (which
 * also serves HANWEB HUB and the mobile applications) and the call itself,
 * carrying the token that sign-in returned. The session survives restarts and
 * is refreshed silently before it expires, so the operator signs in once.
 */

const fs = require('fs');
const path = require('path');

// The unified TENSANTS central API — the Supabase Edge Function tier shared by
// HANCLIENT, HANOUT LITE, HANWEB HUB and this Control Center. Deployments
// override it with TENSANTS_API_URL; a local stack is reachable at
// http://localhost:3001/api.
const DEFAULT_API_URL = 'https://rxwrhhllbkmqvhbfbuwt.supabase.co/functions/v1/api';

// The Supabase project behind that API. The anon key is public by design — the
// website already ships it to every browser and row level security, not the
// key, decides what a caller may read.
const DEFAULT_SUPABASE_URL = 'https://rxwrhhllbkmqvhbfbuwt.supabase.co';
const DEFAULT_SUPABASE_ANON_KEY =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ4d3JoaGxsYmttcXZoYmZidXd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUwMzk2NDAsImV4cCI6MjExMDYxNTY0MH0.FiGGS4GgExobo8nQnGD1hHgY7EvIjNy07tU5BDziOT8';

const REQUEST_TIMEOUT_MS = 8000;
// A token this close to its expiry is renewed before it is used, so a call
// never departs carrying a credential that dies in flight.
const REFRESH_MARGIN_MS = 60 * 1000;

// Where the signed-in session is kept between launches. main.js points this at
// the per-user application data directory; the fallback only matters when the
// module is exercised outside Electron.
let sessionFile = path.join(process.cwd(), '.tensants-session.json');
let session = null;

/*
 * Local configuration is read from `.env` next to package.json (never
 * committed). Deployments set the variables in the real environment instead,
 * and those always win.
 */
function loadEnvFile() {
  try {
    const text = fs.readFileSync(path.join(__dirname, '..', '.env'), 'utf8');
    for (const line of text.split(/\r?\n/)) {
      const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/i);
      if (!match) continue;
      const value = match[2].replace(/^["']|["']$/g, '');
      if (process.env[match[1]] === undefined) process.env[match[1]] = value;
    }
  } catch {
    /* no local .env — the process environment is the only source */
  }
}
loadEnvFile();

function baseUrl() {
  return (process.env.TENSANTS_API_URL || DEFAULT_API_URL).replace(/\/+$/, '');
}

function supabaseUrl() {
  return (process.env.TENSANTS_SUPABASE_URL || DEFAULT_SUPABASE_URL).replace(/\/+$/, '');
}

function anonKey() {
  return process.env.TENSANTS_SUPABASE_ANON_KEY || DEFAULT_SUPABASE_ANON_KEY;
}

/* ----------------------------------------------------------------- responses */

async function toResult(response) {
  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  return { ok: response.ok, status: response.status, data };
}

/* ------------------------------------------------------------------- session */

function readSession() {
  try {
    const parsed = JSON.parse(fs.readFileSync(sessionFile, 'utf8'));
    return parsed && typeof parsed.refresh_token === 'string' ? parsed : null;
  } catch {
    return null;
  }
}

function writeSession(next) {
  session = next;
  try {
    if (next) {
      fs.mkdirSync(path.dirname(sessionFile), { recursive: true });
      fs.writeFileSync(sessionFile, JSON.stringify(next, null, 2), { mode: 0o600 });
    } else {
      fs.rmSync(sessionFile, { force: true });
    }
  } catch {
    /* an unwritable profile only costs the next launch one sign-in */
  }
  // Returning the session lets signIn() report the address it just adopted and
  // lets refreshSession() hand the renewed token straight back.
  return next;
}

/** Points the client at its session file and loads whatever was stored. */
function configure(options) {
  const file = options && options.sessionFile;
  if (typeof file === 'string' && file) sessionFile = file;
  session = readSession();
  return authState();
}

function adopt(data) {
  const expiresIn = Number(data && data.expires_in) || 3600;
  return writeSession({
    access_token: data.access_token,
    refresh_token: data.refresh_token,
    expires_at: Date.now() + expiresIn * 1000,
    email: (data.user && data.user.email) || null,
  });
}

/** What the renderer may know: who is signed in, never the tokens themselves. */
function authState() {
  // A service token in the environment is used as-is (automation, CI, a
  // locked-down workstation) and needs no interactive sign-in.
  if (process.env.TENSANTS_API_TOKEN) {
    return { signedIn: true, email: null, source: 'token', expiresAt: null };
  }
  if (!session || !session.refresh_token) {
    return { signedIn: false, email: null, source: null, expiresAt: null };
  }
  return {
    signedIn: true,
    email: session.email || null,
    source: 'session',
    expiresAt: session.expires_at || null,
  };
}

async function authRequest(pathname, body) {
  const response = await fetch(`${supabaseUrl()}/auth/v1/${pathname}`, {
    method: 'POST',
    headers: { apikey: anonKey(), 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
  return toResult(response);
}

/** Signs an operator in with the same account the website and apps use. */
async function signIn(email, password) {
  if (typeof email !== 'string' || typeof password !== 'string' || !email.trim() || !password) {
    return { ok: false, code: 'credentials_required' };
  }

  let result;
  try {
    result = await authRequest('token?grant_type=password', {
      email: email.trim().toLowerCase(),
      password,
    });
  } catch (error) {
    return { ok: false, code: 'unreachable', message: String((error && error.message) || error) };
  }

  if (!result.ok) {
    // GoTrue answers 400 for a wrong password, 422 for a malformed address.
    const code = result.status === 400 ? 'invalid_credentials' : 'sign_in_failed';
    return { ok: false, code, status: result.status };
  }

  const adopted = adopt(result.data);
  return { ok: true, email: adopted.email };
}

async function signOut() {
  const token = session && session.access_token;
  writeSession(null);
  if (!token) return { ok: true };
  // Revoking server-side is best effort: the local session is gone regardless.
  try {
    await fetch(`${supabaseUrl()}/auth/v1/logout`, {
      method: 'POST',
      headers: { apikey: anonKey(), Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch {
    /* offline sign-out still signs out locally */
  }
  return { ok: true };
}

async function refreshSession() {
  if (!session || !session.refresh_token) return null;
  const result = await authRequest('token?grant_type=refresh_token', {
    refresh_token: session.refresh_token,
  });
  if (!result.ok) {
    writeSession(null);
    return null;
  }
  return adopt(result.data);
}

/** The bearer token for the next API call, refreshed if it is about to die. */
async function accessToken() {
  if (process.env.TENSANTS_API_TOKEN) return process.env.TENSANTS_API_TOKEN;
  if (!session) return null;
  if (Date.now() < (session.expires_at || 0) - REFRESH_MARGIN_MS) return session.access_token;

  const refreshed = await refreshSession().catch(() => null);
  return refreshed ? refreshed.access_token : null;
}

/* ------------------------------------------------------------------- requests */

async function request(path, options) {
  const isPublic = Boolean(options && options.public);
  const headers = { Accept: 'application/json', apikey: anonKey() };

  if (!isPublic) {
    const token = await accessToken();
    if (!token) {
      const error = new Error('Sign in to reach the platform services.');
      error.status = 401;
      error.code = 'AUTH_REQUIRED';
      throw error;
    }
    headers.Authorization = `Bearer ${token}`;
  }

  const payload = options && options.body !== undefined ? JSON.stringify(options.body) : undefined;
  if (payload) headers['Content-Type'] = 'application/json';

  const startedAt = Date.now();
  const response = await fetch(baseUrl() + path, {
    method: (options && options.method) || 'GET',
    headers,
    body: payload,
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  });
  const result = await toResult(response);

  if (!result.ok) {
    const error = new Error(
      (result.data && (result.data.message || result.data.error)) ||
        `HTTP ${result.status} ${response.statusText}`,
    );
    error.status = result.status;
    error.code = (result.data && result.data.error) || null;
    throw error;
  }

  return { data: result.data, latencyMs: Date.now() - startedAt };
}

function get(path, options) {
  return request(path, options);
}

/** Reachability probe. Drives the "connected" badge and never throws. */
async function health() {
  try {
    const { latencyMs } = await get('/v1/health', { public: true });
    return { ok: true, url: baseUrl(), latencyMs };
  } catch (error) {
    return { ok: false, url: baseUrl(), error: String(error.message || error) };
  }
}

/**
 * The dashboard payload — kpis, series, merchants, services, activity and
 * subscriptions, in the exact shape src/js/data.js already renders.
 *
 * A refusal is reported, not thrown: the renderer has to tell "the operator
 * still has to sign in" apart from "the API is down".
 */
async function overview() {
  try {
    const { data, latencyMs } = await get('/v1/overview');
    return { connected: true, data, latencyMs };
  } catch (error) {
    const status = typeof error.status === 'number' ? error.status : null;
    return {
      connected: false,
      url: baseUrl(),
      status,
      reason: status === 401 ? 'unauthenticated' : status === 403 ? 'not_staff' : 'unreachable',
      error: String(error.message || error),
    };
  }
}

/**
 * Opens a tenant for a merchant. The refusal is reported the same way the
 * overview reports it, so the renderer can name the reason it got back.
 */
async function createMerchant(merchant) {
  try {
    const { data } = await request('/v1/merchants', { method: 'POST', body: merchant });
    return { ok: true, merchant_id: data && data.merchant_id, slug: data && data.slug };
  } catch (error) {
    const status = typeof error.status === 'number' ? error.status : null;
    return {
      ok: false,
      status,
      code: error.code || (status === 401 ? 'unauthenticated' : status === 403 ? 'not_staff' : 'failed'),
      error: String(error.message || error),
    };
  }
}

module.exports = {
  baseUrl,
  health,
  overview,
  createMerchant,
  configure,
  authState,
  signIn,
  signOut,
};
