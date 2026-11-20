/* ---------------------------------------------------------------------------
 * app.js — Control Center shell: navigation, i18n, theming, views.
 *
 * Motion is spring-driven (js/spring.js), so KPI cards, counters and the
 * navigation indicator settle with weight instead of linear easing.
 * ------------------------------------------------------------------------- */
(function () {
  'use strict';

  const S = window.Spring;
  const D = window.DATA; // the single data model, filled from the central API
  const T = (key, fallback) => window.I18N.t(state.lang, key, fallback);
  const $ = (sel, root) => (root || document).querySelector(sel);
  const $$ = (sel, root) => Array.from((root || document).querySelectorAll(sel));

  const state = {
    view: 'dashboard',
    lang: localStorage.getItem('tg.lang') || 'en',
    theme: localStorage.getItem('tg.theme') || 'dark',
    query: '',
  };

  // Last update snapshot pushed from the main process, kept so the settings
  // view can repaint itself the moment it is rendered.
  let lastUpdateState = null;

  // Who the app is signed in as, as far as the main process is concerned.
  let authState = { signedIn: false, email: null, source: null };

  const ICONS = {
    dashboard: '<path d="M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-4H4zM14 8h6V4h-6z"/>',
    merchants: '<path d="M4 9.5 5.6 4h12.8L20 9.5"/><path d="M4 9.5a2.2 2.2 0 0 0 4.4 0 2.2 2.2 0 0 0 4.4 0 2.2 2.2 0 0 0 4.4 0 2.2 2.2 0 0 0 2.8 0"/><path d="M5.5 12v8h13v-8"/><path d="M10 20v-4h4v4"/>',
    customers: '<circle cx="9" cy="8" r="3.4"/><path d="M3 20a6 6 0 0 1 12 0"/><path d="M16.5 5.4a3 3 0 0 1 0 5.6"/><path d="M17.5 14.6A5.4 5.4 0 0 1 21 19.6"/>',
    orders: '<path d="M4 6h16l-1.4 9.2a2 2 0 0 1-2 1.8H7.4a2 2 0 0 1-2-1.8L4 6Z"/><path d="M9 6V4.8A2.8 2.8 0 0 1 11.8 2h.4A2.8 2.8 0 0 1 15 4.8V6"/>',
    subscriptions: '<rect x="3" y="5.5" width="18" height="13" rx="3"/><path d="M3 10h18"/><path d="M7 14.5h4"/>',
    apps: '<rect x="3" y="4" width="7" height="7" rx="2"/><rect x="14" y="4" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><path d="M14.5 17.5h6M17.5 14.5v6"/>',
    notifications: '<path d="M18 15.2V11a6 6 0 1 0-12 0v4.2L4.6 17.4A.8.8 0 0 0 5.3 18.8h13.4a.8.8 0 0 0 .7-1.4L18 15.2Z"/><path d="M9.8 20.6a2.4 2.4 0 0 0 4.4 0"/>',
    reports: '<path d="M6 3h8l4 4v14a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z"/><path d="M14 3v4h4"/><path d="M8.5 13h7M8.5 17h4.5"/>',
    audit: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
    settings: '<circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z"/>',
  };

  const NAV = [
    { group: 'nav.overview', items: [
      { id: 'dashboard', label: 'nav.dashboard', icon: 'dashboard' },
      { id: 'merchants', label: 'nav.merchants', icon: 'merchants' },
      { id: 'customers', label: 'nav.customers', icon: 'customers' },
    ]},
    { group: 'nav.sales', items: [
      { id: 'orders', label: 'nav.orders', icon: 'orders' },
      { id: 'subscriptions', label: 'nav.subscriptions', icon: 'subscriptions' },
    ]},
    { group: 'nav.system', items: [
      { id: 'apps', label: 'nav.apps', icon: 'apps' },
      { id: 'notifications', label: 'nav.notifications', icon: 'notifications', badge: 4 },
      { id: 'reports', label: 'nav.reports', icon: 'reports', badge: 7 },
      { id: 'audit', label: 'nav.audit', icon: 'audit' },
      { id: 'settings', label: 'nav.settings', icon: 'settings' },
    ]},
  ];

  const fmt = (n) => new Intl.NumberFormat(state.lang === 'ar' ? 'ar-MA' : state.lang === 'fr' ? 'fr-FR' : 'en-US').format(n);

  /* ------------------------------- chrome -------------------------------- */

  function applyChrome() {
    const html = document.documentElement;
    html.lang = state.lang;
    html.dir = window.I18N.dir(state.lang);
    html.dataset.dir = html.dir;
    html.dataset.theme = state.theme;
    $('#langLabel').textContent = state.lang.toUpperCase();
    $('#searchInput').placeholder = T('search');
    $$('[data-i18n]').forEach((el) => {
      el.textContent = T(el.dataset.i18n, el.textContent);
    });
    const connText = $('#connText');
    if (connText) connText.textContent = T(D.connected ? 'connected' : 'disconnected');
  }

  function renderNav() {
    const nav = $('#nav');
    nav.innerHTML = NAV.map((g) => `
      <div class="nav__group">${T(g.group)}</div>
      ${g.items.map((it) => `
        <button class="nav__item ${it.id === state.view ? 'is-active' : ''}" data-view="${it.id}">
          <svg viewBox="0 0 24 24">${ICONS[it.icon]}</svg>
          <span>${T(it.label)}</span>
          ${it.badge ? `<em class="nav__badge">${it.badge}</em>` : ''}
        </button>`).join('')}
    `).join('');

    $$('.nav__item', nav).forEach((btn) => {
      btn.addEventListener('click', () => {
        setView(btn.dataset.view);
      });
    });
  }

  function setView(id) {
    if (state.view === id) return;
    state.view = id;
    $$('.nav__item').forEach((b) => b.classList.toggle('is-active', b.dataset.view === id));
    renderView(true);
  }

  function toast(msg) {
    const el = $('#toast');
    el.textContent = msg;
    el.classList.add('is-on');
    clearTimeout(toast._t);
    toast._t = setTimeout(() => el.classList.remove('is-on'), 2200);
  }

  /* --------------------------------- views -------------------------------- */

  function pageHead() {
    const [title, sub] = T('page.' + state.view, '') || [];
    $('#pageTitle').textContent = title || T('nav.' + state.view);
    $('#pageSub').textContent = sub || '';
  }

  function kpiCard({ label, value, suffix, icon, tone, trend, trendUp, foot }) {
    return `
      <div class="card kpi ${tone ? 'kpi--' + tone : ''}">
        <div class="kpi__top">
          <span class="kpi__label">${label}</span>
          <span class="kpi__icon"><svg viewBox="0 0 24 24">${ICONS[icon]}</svg></span>
        </div>
        <div class="kpi__value"><span data-count="${value}">0</span>${suffix ? `<small>${suffix}</small>` : ''}</div>
        <div class="kpi__foot">
          ${trend ? `<span class="trend trend--${trendUp ? 'up' : 'down'}">${trendUp ? '▲' : '▼'} ${trend}</span>` : ''}
          <span>${foot || ''}</span>
        </div>
      </div>`;
  }

  function chartSvg(series) {
    const w = 640, h = 200, pad = { t: 14, r: 10, b: 24, l: 34 };
    const max = Math.max(...series.map((p) => p.v)) * 1.12;
    const iw = w - pad.l - pad.r;
    const ih = h - pad.t - pad.b;
    const step = iw / series.length;
    const x = (i) => pad.l + i * step + step / 2;
    const y = (v) => pad.t + ih - (v / max) * ih;

    const bars = series.map((p, i) => {
      const bh = (p.v / max) * ih;
      const bw = Math.min(step * 0.46, 22);
      return `<rect class="bar" x="${(x(i) - bw / 2).toFixed(1)}" y="${(pad.t + ih - bh).toFixed(1)}"
                width="${bw.toFixed(1)}" height="${bh.toFixed(1)}" rx="4"><title>${p.d}: ${p.v}</title></rect>`;
    }).join('');

    const linePts = series.map((p, i) => `${x(i).toFixed(1)},${y(p.v).toFixed(1)}`).join(' ');
    const grid = [0, 0.25, 0.5, 0.75, 1].map((f) => {
      const yy = (pad.t + ih - f * ih).toFixed(1);
      return `<line class="grid-line" x1="${pad.l}" y1="${yy}" x2="${w - pad.r}" y2="${yy}"/>
              <text class="axis-text" x="${pad.l - 8}" y="${(Number(yy) + 3).toFixed(1)}" text-anchor="end">${Math.round(max * f)}</text>`;
    }).join('');

    const labels = series.map((p, i) =>
      `<text class="axis-text" x="${x(i).toFixed(1)}" y="${h - 6}" text-anchor="middle">${p.d}</text>`).join('');

    return `
      <svg class="chart" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none" role="img">
        <defs>
          <linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#5B93FF"/><stop offset="100%" stop-color="#1657D6"/>
          </linearGradient>
          <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="rgba(245,161,28,.30)"/><stop offset="100%" stop-color="rgba(245,161,28,0)"/>
          </linearGradient>
        </defs>
        ${grid}
        ${bars}
        <polyline class="line-gold" points="${linePts}"/>
        <polyline class="line" points="${linePts}"/>
        ${labels}
      </svg>`;
  }

  function donutSvg(parts, total) {
    const r = 44, c = 2 * Math.PI * r;
    let offset = 0;
    const arcs = parts.map((p) => {
      const len = (p.value / total) * c;
      const el = `<circle cx="62" cy="62" r="${r}" fill="none" stroke="${p.color}" stroke-width="12"
        stroke-dasharray="${len - 3} ${c - len + 3}" stroke-dashoffset="${-offset}"
        transform="rotate(-90 62 62)" stroke-linecap="round"/>`;
      offset += len;
      return el;
    }).join('');
    return `<svg class="donut" viewBox="0 0 124 124">
      <circle cx="62" cy="62" r="${r}" fill="none" stroke="var(--surface-3)" stroke-width="12"/>
      ${arcs}
      <g class="donut__center"><text x="62" y="60" text-anchor="middle" fill="var(--text)"
        style="font:800 21px var(--font-display)">${total}</text>
        <text x="62" y="76" text-anchor="middle" fill="var(--muted)" style="font:600 8px var(--font);letter-spacing:.1em">TOTAL</text></g>
    </svg>`;
  }

  function healthRows(limit) {
    const rows = D.services.slice(0, limit || D.services.length);
    if (!rows.length) return emptyRow(T('empty'));
    return rows.map((s) => `
      <div class="health__row">
        <i class="health__dot dot--${s.status}"></i>
        <div class="health__name">
          <b>${s.name}</b>
          <span>${s.kind} · ${T('health.version')} ${s.version}</span>
        </div>
        <span class="health__lat" dir="ltr">${s.status === 'error' || s.status === 'offline' ? '—' : s.latency + ' ms'}</span>
        <span class="chip chip--${s.status}"><i></i>${T('health.' + s.status)}</span>
      </div>`).join('');
  }

  /** Shared placeholder so every list degrades to the same empty state. */
  function emptyRow(text) {
    return `<div class="empty">${text || T('empty')}</div>`;
  }

  function dashboardView() {
    const k = D.kpis;
    const totalSubs = D.subscriptions.reduce((a, b) => a + b.value, 0);
    const orders14 = D.series.reduce((a, b) => a + b.v, 0);

    return `
      <div class="grid grid--kpi">
        ${kpiCard({ label: T('kpi.merchants'), value: k.merchants, icon: 'merchants', foot: `${fmt(k.active)} ${T('status.active')}` })}
        ${kpiCard({ label: T('kpi.customers'), value: k.customers, icon: 'merchants', tone: 'green', foot: 'HANOUT LITE + HANCLIENT' })}
        ${kpiCard({ label: T('kpi.orders'), value: k.orders, icon: 'orders' })}
        ${kpiCard({ label: T('kpi.subscriptions'), value: k.subscriptions, icon: 'subscriptions', tone: 'gold', foot: `${totalSubs} ${T('section.split').toLowerCase()}` })}
      </div>

      <div class="grid grid--2">
        <section class="card">
          <div class="card__head">
            <div>
              <div class="card__title">${T('section.traffic')}</div>
              <div class="card__hint">${fmt(orders14)} ${T('kpi.orders').toLowerCase()}</div>
            </div>
            <span class="chip chip--${D.connected ? 'online' : 'offline'}"><i></i>${D.connected ? T('connected') : T('disconnected')}</span>
          </div>
          ${D.series.length ? chartSvg(D.series) : emptyRow(T('empty'))}
          <div class="legend">
            <span><i style="background:#2B6BEC"></i>${T('kpi.orders')}</span>
            <span><i style="background:#F5A11C"></i>Trend</span>
            ${D.generatedAt ? `<span style="margin-inline-start:auto;opacity:.7">${T('health.checked')}: ${new Date(D.generatedAt).toLocaleTimeString()}</span>` : ''}
          </div>
        </section>

        <section class="card">
          <div class="card__head">
            <div class="card__title">${T('section.health')}</div>
            <a class="card__hint" style="color:var(--accent);cursor:pointer" data-view-link="apps">${T('nav.apps')} →</a>
          </div>
          <div class="health">${healthRows(6)}</div>
        </section>
      </div>

      <div class="grid grid--2">
        <section class="card">
          <div class="card__head">
            <div class="card__title">${T('section.activity')}</div>
            <span class="card__hint">${T('nav.audit')}</span>
          </div>
          <table class="tbl">
            <thead><tr>
              <th>${T('table.time')}</th><th>${T('table.actor')}</th>
              <th>${T('table.action')}</th><th>${T('table.target')}</th>
            </tr></thead>
            <tbody>
              ${D.activity.length ? D.activity.map((a) => `<tr>
                <td class="num">${a.t}</td>
                <td class="strong">${a.actor}</td>
                <td>${a.action}</td>
                <td>${a.target}</td>
              </tr>`).join('') : `<tr><td colspan="4">${emptyRow(T('empty'))}</td></tr>`}
            </tbody>
          </table>
        </section>

        <section class="card">
          <div class="card__head">
            <div class="card__title">${T('section.split')}</div>
            <span class="card__hint">${D.services.length} ${T('section.health').toLowerCase()}</span>
          </div>
          <div class="donut-wrap">
            ${totalSubs ? donutSvg(D.subscriptions, totalSubs) : emptyRow(T('empty'))}
            <div class="donut__legend">
              ${D.subscriptions.map((s) => `<div>
                <i style="background:${s.color}"></i>${T('status.' + s.key)}<b>${s.value}</b>
              </div>`).join('')}
            </div>
          </div>
        </section>
      </div>

      <div class="grid grid--3">
        ${kpiCard({ label: T('kpi.products'), value: k.products, icon: 'orders', foot: T('kpi.active').toLowerCase() })}
        ${kpiCard({ label: T('kpi.notifications'), value: k.notifications, icon: 'notifications', foot: 'Push + e-mail' })}
        ${kpiCard({ label: T('kpi.sync'), value: k.sync, icon: 'reports', tone: 'red', foot: T('kpi.sync') })}
      </div>`;
  }

  function merchantsView() {
    const q = state.query.trim().toLowerCase();
    const rows = D.merchants.filter((m) =>
      !q || (m.name + m.city + m.plan + m.status).toLowerCase().includes(q));

    return `
      <div class="grid grid--kpi">
        ${kpiCard({ label: T('kpi.merchants'), value: D.kpis.merchants, icon: 'merchants' })}
        ${kpiCard({ label: T('kpi.active'), value: D.kpis.active, icon: 'merchants', tone: 'green' })}
        ${kpiCard({ label: T('status.suspended'), value: D.merchants.filter((m) => m.status === 'suspended').length, icon: 'merchants', tone: 'red' })}
        ${kpiCard({ label: T('status.trial'), value: D.merchants.filter((m) => m.status === 'trial').length, icon: 'merchants', tone: 'gold' })}
      </div>

      <section class="card">
        <div class="card__head">
          <div class="card__title">${T('page.merchants')[0]}</div>
          <div style="display:flex;gap:8px">
            <button class="btn">${T('actions.filter')}</button>
            <button class="btn btn--primary" id="newMerchantBtn">${T('actions.newMerchant')}</button>
          </div>
        </div>
        <table class="tbl">
          <thead><tr>
            <th>${T('table.merchant')}</th><th>${T('table.city')}</th><th>${T('table.plan')}</th>
            <th>${T('table.status')}</th><th>${T('table.orders')}</th><th>${T('table.revenue')} (MAD)</th>
            <th>${T('table.joined')}</th>
          </tr></thead>
          <tbody>
            ${rows.length ? rows.map((m) => `<tr>
              <td><div class="tbl__person">
                <span class="avatar">${m.name.slice(0, 2).toUpperCase()}</span>
                <span class="strong">${m.name}</span>
              </div></td>
              <td>${m.city}</td>
              <td>${m.plan}</td>
              <td><span class="chip chip--${m.status}"><i></i>${T('status.' + m.status)}</span></td>
              <td class="num">${fmt(m.orders)}</td>
              <td class="num strong">${m.revenue}</td>
              <td class="num">${m.joined}</td>
            </tr>`).join('') : `<tr><td colspan="7">${emptyRow(T('empty'))}</td></tr>`}
          </tbody>
        </table>
      </section>`;
  }

  function customersView() {
    const q = state.query.trim().toLowerCase();
    const rows = D.customers.filter((c) =>
      !q || (c.name + c.email + c.phone).toLowerCase().includes(q));

    const monthAgo = Date.now() - 30 * 864e5;
    const recent = D.customers.filter((c) => {
      const t = Date.parse(c.joined);
      return !Number.isNaN(t) && t >= monthAgo;
    }).length;

    return `
      <div class="grid grid--kpi">
        ${kpiCard({ label: T('kpi.customers'), value: D.kpis.customers, icon: 'customers' })}
        ${kpiCard({ label: T('customers.withPhone'), value: D.customers.filter((c) => c.phone && c.phone !== '—').length, icon: 'customers', tone: 'green' })}
        ${kpiCard({ label: T('customers.recent'), value: recent, icon: 'customers', tone: 'gold' })}
      </div>

      <section class="card">
        <div class="card__head">
          <div class="card__title">${T('page.customers')[0]}</div>
          <span class="card__hint">${T('customers.note')}</span>
        </div>
        <table class="tbl">
          <thead><tr>
            <th>${T('table.customer')}</th><th>${T('table.email')}</th>
            <th>${T('table.phone')}</th><th>${T('table.language')}</th><th>${T('table.joined')}</th>
          </tr></thead>
          <tbody>
            ${rows.length ? rows.map((c) => `<tr>
              <td><div class="tbl__person">
                <span class="avatar">${String(c.name).slice(0, 2).toUpperCase()}</span>
                <span class="strong">${c.name}</span>
              </div></td>
              <td>${c.email}</td>
              <td class="num">${c.phone}</td>
              <td>${String(c.locale).toUpperCase()}</td>
              <td class="num">${c.joined}</td>
            </tr>`).join('') : `<tr><td colspan="5">${emptyRow(T('empty'))}</td></tr>`}
          </tbody>
        </table>
      </section>`;
  }

  function appsView() {
    return `
      <div class="grid grid--kpi">
        ${['online', 'degraded', 'error', 'offline'].map((st) => {
          const n = D.services.filter((s) => s.status === st).length;
          return kpiCard({
            label: T('health.' + st), value: n, icon: 'apps',
            tone: st === 'online' ? 'green' : st === 'error' ? 'red' : st === 'degraded' ? 'gold' : '',
          });
        }).join('')}
      </div>

      <section class="card">
        <div class="card__head">
          <div class="card__title">${T('section.health')}</div>
          ${D.generatedAt ? `<span class="card__hint">${T('health.checked')}: ${new Date(D.generatedAt).toLocaleTimeString()}</span>` : ''}
        </div>
        <div class="health">${healthRows()}</div>
      </section>`;
  }

  function subscriptionsView() {
    const total = D.subscriptions.reduce((a, b) => a + b.value, 0);
    const rows = D.merchants.filter((m) => m.plan && m.plan !== '—');
    const free = rows.filter((m) => m.freeUntil && m.freeUntil !== '—' && Date.parse(m.freeUntil) > Date.now());

    return `
      <div class="grid grid--kpi">
        ${kpiCard({ label: T('kpi.subscriptions'), value: D.kpis.subscriptions, icon: 'subscriptions', tone: 'green' })}
        ${kpiCard({ label: T('status.trial'), value: rows.filter((m) => m.status === 'trial').length, icon: 'subscriptions', tone: 'gold' })}
        ${kpiCard({ label: T('subscriptions.free'), value: free.length, icon: 'subscriptions' })}
      </div>

      <div class="grid grid--2">
        <section class="card">
          <div class="card__head"><div class="card__title">${T('section.split')}</div></div>
          <div class="donut-wrap">
            ${total ? donutSvg(D.subscriptions, total) : emptyRow(T('empty'))}
            <div class="donut__legend">
              ${D.subscriptions.map((s) => `<div>
                <i style="background:${s.color}"></i>${T('status.' + s.key)}<b>${s.value}</b>
              </div>`).join('')}
            </div>
          </div>
        </section>
        <section class="card">
          <div class="card__head">
            <div class="card__title">${T('nav.subscriptions')}</div>
            <span class="card__hint">${T('subscriptions.note')}</span>
          </div>
          <table class="tbl">
            <thead><tr>
              <th>${T('table.customer')}</th><th>${T('table.plan')}</th>
              <th>${T('table.price')}</th><th>${T('table.freeUntil')}</th><th>${T('table.status')}</th>
            </tr></thead>
            <tbody>
              ${rows.length ? rows.map((m) => `<tr>
                <td class="strong">${m.name}</td>
                <td>${m.plan}</td>
                <td class="num">${m.price === null || m.price === undefined ? '—' : fmt(m.price) + ' MAD'}</td>
                <td class="num">${m.freeUntil || '—'}</td>
                <td><span class="chip chip--${m.status}"><i></i>${T('status.' + m.status)}</span></td>
              </tr>`).join('') : `<tr><td colspan="5">${emptyRow(T('empty'))}</td></tr>`}
            </tbody>
          </table>
        </section>
      </div>`;
  }

  /** Updates card: silent background checks, one explicit install decision. */
  function updateCard() {
    return `
        <section class="card">
          <div class="card__head"><div class="card__title">${T('settings.updates')}</div></div>
          <div class="about">
            <div class="about__row"><span>${T('update.installed')}</span><b id="updCurrent" dir="ltr">—</b></div>
            <div class="about__row"><span>${T('update.available')}</span><b id="updVersion" dir="ltr">—</b></div>
            <div class="about__row"><span>${T('update.status')}</span><b id="updStatus">${T('update.idle')}</b></div>
          </div>
          <div class="upd">
            <div class="upd__track" id="updTrack" hidden><div class="upd__bar" id="updBar"></div></div>
            <div class="upd__btns">
              <button class="btn" id="updCheck">${T('update.check')}</button>
              <button class="btn btn--primary" id="updInstall" hidden>${T('update.install')}</button>
            </div>
          </div>
        </section>`;
  }

  function settingsView() {
    return `
      <div class="settings">
        <section class="card field">
          <div class="field__label">${T('settings.appearance')}</div>
          <div class="seg" id="themeSeg">
            <button data-theme-set="dark" class="${state.theme === 'dark' ? 'is-on' : ''}">${T('settings.dark')}</button>
            <button data-theme-set="light" class="${state.theme === 'light' ? 'is-on' : ''}">${T('settings.light')}</button>
          </div>
          <div class="field__label" style="margin-top:8px">${T('settings.language')}</div>
          <div class="seg" id="langSeg">
            ${window.I18N.langs.map((l) => `<button data-lang-set="${l}" class="${state.lang === l ? 'is-on' : ''}">${window.I18N.labels[l]}</button>`).join('')}
          </div>
        </section>

        <section class="card">
          <div class="card__head"><div class="card__title">${T('settings.about')}</div></div>
          <div class="about">
            <div class="about__row"><span>${T('appName')}</span><b>TENSANTS GROUP CONTROL CENTER</b></div>
            <div class="about__row"><span>${T('settings.version')}</span><b id="aboutVer">0.1.0</b></div>
            <div class="about__row"><span>${T('settings.stack')}</span><b id="aboutStack">Electron · Chromium</b></div>
            <div class="about__row"><span>Brand</span><b>Control · Manage · Grow</b></div>
            <div class="about__row"><span>Credit</span><b>FIPCOM Network</b></div>
          </div>
        </section>

        <section class="card">
          <div class="card__head"><div class="card__title">${T('auth.account')}</div></div>
          <div class="about">
            <div class="about__row"><span>${T('auth.signedInAs')}</span><b id="authEmail" dir="ltr">—</b></div>
          </div>
          <div class="upd__btns">
            <button class="btn" id="signOutBtn">${T('auth.signOut')}</button>
          </div>
        </section>

        ${updateCard()}
      </div>`;
  }

  function scaffoldView() {
    const [title, sub] = T('page.' + state.view);
    return `
      <section class="card">
        <div class="scaffold">
          <div class="scaffold__icon"><svg viewBox="0 0 24 24">${ICONS[state.view] || ICONS.dashboard}</svg></div>
          <h3>${title} — ${T('scaffold.title')}</h3>
          <p>${T('scaffold.body')}</p>
        </div>
      </section>`;
  }

  function renderView(animate) {
    pageHead();
    const map = {
      dashboard: dashboardView,
      merchants: merchantsView,
      customers: customersView,
      apps: appsView,
      subscriptions: subscriptionsView,
      settings: settingsView,
      orders: scaffoldView,
      notifications: scaffoldView,
      reports: scaffoldView,
      audit: scaffoldView,
    };
    const view = $('#view');
    view.innerHTML = (map[state.view] || scaffoldView)();
    bindViewControls();
    countUp();
    if (animate !== false) entrance();
  }

  function bindViewControls() {
    $$('[data-view-link]').forEach((el) =>
      el.addEventListener('click', () => setView(el.dataset.viewLink)));

    $$('[data-theme-set]').forEach((b) =>
      b.addEventListener('click', () => { setTheme(b.dataset.themeSet); renderView(false); }));

    $$('[data-lang-set]').forEach((b) =>
      b.addEventListener('click', () => { setLang(b.dataset.langSet); renderView(false); }));

    if (window.controlCenter && window.controlCenter.info) {
      window.controlCenter.info().then((info) => {
        const v = $('#aboutVer');
        const s = $('#aboutStack');
        const cur = $('#updCurrent');
        if (v) v.textContent = info.version;
        if (s) s.textContent = `Electron ${info.electron} · Chromium ${info.chrome}`;
        if (cur) cur.textContent = info.version;
      });
    }

    const checkBtn = $('#updCheck');
    if (checkBtn) {
      checkBtn.addEventListener('click', async () => {
        if (!window.controlCenter || !window.controlCenter.updates) return;
        renderUpdate({ status: 'checking', percent: 0 });
        lastUpdateState = await window.controlCenter.updates.check();
        renderUpdate(lastUpdateState);
      });
    }

    const installBtn = $('#updInstall');
    if (installBtn) {
      installBtn.addEventListener('click', () => {
        if (window.controlCenter && window.controlCenter.updates) {
          window.controlCenter.updates.install();
        }
      });
    }

    renderUpdate(lastUpdateState);
    renderAuth();

    const newMerchantBtn = $('#newMerchantBtn');
    if (newMerchantBtn) newMerchantBtn.addEventListener('click', openMerchantModal);

    const signOutBtn = $('#signOutBtn');
    if (signOutBtn) {
      signOutBtn.addEventListener('click', async () => {
        const bridge = window.controlCenter;
        if (!bridge || !bridge.auth) return;
        await bridge.auth.logout();
        await syncAuth();
        renderView(false);
        setConnection(false, 'unauthenticated');
        showGate(true);
      });
    }
  }

  /* ------------------------------ updates --------------------------------- */

  const UPDATE_LABELS = {
    idle: 'update.idle',
    checking: 'update.checking',
    downloading: 'update.downloading',
    ready: 'update.ready',
    'up-to-date': 'update.upToDate',
    error: 'update.error',
    dev: 'update.dev',
  };

  /** Repaint the settings-updates card; a no-op unless that view is on screen. */
  function renderUpdate(snapshot) {
    const statusEl = $('#updStatus');
    if (!statusEl) return;

    const status = (snapshot && snapshot.status) || 'idle';
    statusEl.textContent = T(UPDATE_LABELS[status] || 'update.idle');

    const versionEl = $('#updVersion');
    if (versionEl) versionEl.textContent = (snapshot && snapshot.version) || '—';

    const busy = status === 'checking' || status === 'downloading';
    const track = $('#updTrack');
    if (track) track.hidden = status !== 'downloading';
    const bar = $('#updBar');
    if (bar) {
      const pct = Math.max(0, Math.min(100, (snapshot && snapshot.percent) || 0));
      bar.style.width = pct + '%';
    }

    const installBtn = $('#updInstall');
    if (installBtn) installBtn.hidden = status !== 'ready';
    const checkBtn = $('#updCheck');
    if (checkBtn) checkBtn.disabled = busy;
  }

  /* ------------------------------ motion ---------------------------------- */

  /** Spring count-up for every KPI value currently on screen. */
  function countUp() {
    $$('#view [data-count]').forEach((el) => {
      const target = Number(el.dataset.count) || 0;
      S.animate(0, target, { stiffness: 95, damping: 24, precision: 0.6 }, (v) => {
        el.textContent = fmt(Math.max(0, Math.round(v)));
      }).then(() => { el.textContent = fmt(target); });
    });
  }

  function entrance() {
    // Cards fade up with a stagger; the rise is a real spring, so each card
    // settles instead of stopping dead on a cubic-bezier curve.
    $$('#view .card').forEach((card, i) => {
      card.style.opacity = '0';
      card.style.transform = 'translateY(16px)';
      setTimeout(() => {
        S.animate(0, 1, { stiffness: 200, damping: 26, mass: 1 }, (v) => {
          card.style.opacity = String(Math.min(v, 1));
        });
        S.animate(16, 0, { stiffness: 240, damping: 21, mass: 1 }, (v) => {
          card.style.transform = `translateY(${Math.max(v, 0)}px)`;
        }).then(() => { card.style.transform = 'translateY(0)'; });
      }, i * 55);
    });

    // chart bars grow from the baseline with a light bounce
    $$('#view .chart .bar').forEach((bar) => {
      const h = Number(bar.getAttribute('height'));
      const y = Number(bar.getAttribute('y'));
      const base = y + h;
      bar.setAttribute('height', '0');
      bar.setAttribute('y', String(base));
      S.animate(0, 1, { stiffness: 200, damping: 19, mass: 1 }, (v) => {
        const vv = Math.max(0, Math.min(v, 1.05));
        const hh = h * vv;
        bar.setAttribute('height', hh.toFixed(1));
        bar.setAttribute('y', (base - hh).toFixed(1));
      }).then(() => {
        bar.setAttribute('height', String(h));
        bar.setAttribute('y', String(y));
      });
    });
  }

  /* ------------------------------- theme/lang ------------------------------ */

  function setTheme(theme) {
    state.theme = theme;
    localStorage.setItem('tg.theme', theme);
    document.documentElement.dataset.theme = theme;
  }

  function setLang(lang) {
    state.lang = lang;
    localStorage.setItem('tg.lang', lang);
    applyChrome();
    renderNav();
    renderView(false);
  }

  /* ---------------------------- central data ------------------------------ */

  // Reflect the real connection state in the two chrome affordances: the
  // sidebar pill and the top banner (shown only while the API is unreachable).
  // A refusal is not an outage, so the banner says which one it is.
  function setConnection(connected, reason) {
    D.connected = connected;

    const pill = $('#connPill');
    if (pill) pill.classList.toggle('conn--on', connected);
    const text = $('#connText');
    if (text) text.textContent = T(connected ? 'connected' : 'disconnected');

    const banner = $('#connBanner');
    if (banner) banner.classList.toggle('conn-banner--on', connected);
    const label = banner && banner.querySelector('span');
    if (label) label.textContent = T(reason === 'unauthenticated' ? 'auth.banner' : 'disconnected');
  }

  /* -------------------------- operator sign-in ---------------------------- */

  /** The gate is the only screen a signed-out operator sees. */
  function showGate(show) {
    const gate = $('#loginGate');
    if (!gate) return;
    gate.hidden = !show;
    if (show) {
      const email = $('#loginEmail');
      if (email && !email.value) email.focus();
    }
  }

  function gateMessage(result) {
    const error = $('#loginError');
    if (!error) return;
    const code = (result && result.code) || 'sign_in_failed';
    error.textContent = T('auth.error.' + code, T('auth.error.sign_in_failed'));
    error.hidden = false;
  }

  function renderAuth() {
    const emailEl = $('#authEmail');
    if (emailEl) {
      emailEl.textContent = authState.email || (authState.source === 'token' ? T('auth.serviceToken') : '—');
    }
    // A token supplied by the environment is not the app's to revoke.
    const signOutBtn = $('#signOutBtn');
    if (signOutBtn) signOutBtn.hidden = authState.source === 'token';
  }

  async function syncAuth() {
    const bridge = window.controlCenter;
    if (!bridge || !bridge.auth) return authState;
    try {
      const state = await bridge.auth.state();
      if (state) authState = state;
    } catch {
      /* the API answer decides instead */
    }
    return authState;
  }

  function wireGate() {
    const form = $('#loginForm');
    if (form) {
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const bridge = window.controlCenter;
        if (!bridge || !bridge.auth) return;

        const email = $('#loginEmail').value.trim();
        const password = $('#loginPassword').value;
        const errorEl = $('#loginError');
        const submit = $('#loginSubmit');

        if (errorEl) errorEl.hidden = true;
        if (submit) {
          submit.disabled = true;
          submit.textContent = T('auth.submitting');
        }

        let result = null;
        try {
          result = await bridge.auth.login(email, password);
        } catch {
          result = { ok: false, code: 'unreachable' };
        }

        if (submit) {
          submit.disabled = false;
          submit.textContent = T('auth.submit');
        }

        if (!result || !result.ok) {
          gateMessage(result);
          $('#loginPassword').value = '';
          return;
        }

        $('#loginPassword').value = '';
        await syncAuth();
        showGate(false);
        renderAuth();
        await connect();
      });
    }

    const retry = $('#loginRetry');
    if (retry) {
      retry.addEventListener('click', async () => {
        const bridge = window.controlCenter;
        const label = $('#loginApi');
        if (!bridge || !bridge.data) return;
        if (label) label.textContent = T('auth.retry') + '…';
        const probe = await bridge.data.health();
        if (label) label.textContent = probe && probe.ok ? T('auth.apiReachable') : (probe && probe.error) || '';
      });
    }
  }

  /* ---------------------------- new merchant ------------------------------ */

  function openMerchantModal() {
    const modal = $('#merchantModal');
    if (!modal) return;
    modalError(null);
    modal.hidden = false;
    const name = $('#mchName');
    if (name) name.focus();
  }

  function closeMerchantModal() {
    const modal = $('#merchantModal');
    if (!modal) return;
    modal.hidden = true;
    const form = $('#merchantForm');
    if (form) form.reset();
    modalError(null);
  }

  function modalError(code) {
    const el = $('#mchError');
    if (!el) return;
    el.hidden = !code;
    if (code) el.textContent = T('merchant.error.' + code, T('merchant.error.failed'));
  }

  function wireMerchantModal() {
    const form = $('#merchantForm');
    if (!form) return;

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const bridge = window.controlCenter;
      if (!bridge || !bridge.data || !bridge.data.createMerchant) return;

      const payload = {
        name: $('#mchName').value.trim(),
        slug: $('#mchSlug').value.trim(),
        city: $('#mchCity').value.trim(),
        phone: $('#mchPhone').value.trim(),
        product_key: $('#mchProduct').value,
      };
      if (!payload.name) {
        modalError('name_required');
        return;
      }

      modalError(null);
      const submit = $('#mchSubmit');
      if (submit) {
        submit.disabled = true;
        submit.textContent = T('merchant.creating');
      }

      let result = null;
      try {
        result = await bridge.data.createMerchant(payload);
      } catch {
        result = null;
      }

      if (submit) {
        submit.disabled = false;
        submit.textContent = T('merchant.create');
      }

      if (!result || !result.ok) {
        modalError((result && result.code) || 'failed');
        return;
      }

      closeMerchantModal();
      toast(T('merchant.created') + ' · ' + (result.slug || payload.name));
      await connect();
    });

    const cancel = $('#mchCancel');
    if (cancel) cancel.addEventListener('click', closeMerchantModal);

    const modal = $('#merchantModal');
    if (modal) {
      modal.addEventListener('click', (event) => {
        if (event.target === modal) closeMerchantModal();
      });
    }
  }

  // Pull the live dashboard payload through the main process and repaint.
  // The shell shows its empty states until the central API answers.
  async function connect() {
    const bridge = window.controlCenter;
    if (!bridge || !bridge.data) {
      setConnection(false);
      return;
    }

    await syncAuth();

    let result = null;
    try {
      result = await bridge.data.overview();
    } catch (err) {
      result = null;
    }

    if (!result || !result.connected || !result.data) {
      setConnection(false, result && result.reason);
      // The API answered and refused this caller: ask for the account.
      showGate(Boolean(result && result.reason === 'unauthenticated'));
      if (result && result.reason === 'not_staff') toast(T('auth.error.not_staff'));
      return;
    }

    showGate(false);
    Object.assign(D, result.data, { connected: true });
    setConnection(true);
    renderView(false);
  }

  /* --------------------------------- boot --------------------------------- */

  function boot() {
    applyChrome();
    renderNav();
    renderView(true);
    wireGate();
    wireMerchantModal();

    $('#themeBtn').addEventListener('click', () => {
      setTheme(state.theme === 'dark' ? 'light' : 'dark');
      renderView(false);
    });

    $('#langBtn').addEventListener('click', () => {
      const i = window.I18N.langs.indexOf(state.lang);
      setLang(window.I18N.langs[(i + 1) % window.I18N.langs.length]);
    });

    $('#bellBtn').addEventListener('click', () => setView('notifications'));
    $('#refreshBtn').addEventListener('click', () => {
      renderView(true);
      connect();
      toast(T('actions.refresh') + ' ✓');
    });

    $('#searchInput').addEventListener('input', (e) => {
      state.query = e.target.value;
      if (state.view !== 'merchants' && state.view !== 'customers') setView('merchants');
      else renderView(false);
    });

    if (window.controlCenter) {
      $('#minBtn').addEventListener('click', () => window.controlCenter.minimize());
      $('#maxBtn').addEventListener('click', () => window.controlCenter.toggleMaximize());
      $('#closeBtn').addEventListener('click', () => window.controlCenter.close());
      window.controlCenter.info().then((info) => {
        $('#verText').textContent = 'v' + info.version + ' · ' + info.platform;
      });
      if (window.controlCenter.updates) {
        window.controlCenter.updates.onStatus((s) => { lastUpdateState = s; renderUpdate(s); });
        window.controlCenter.updates.state().then((s) => { lastUpdateState = s; renderUpdate(s); });
      }
    } else {
      // Running in a plain browser (design review) — hide chrome buttons.
      $('.winctl').style.display = 'none';
    }

    connect();
  }

  document.addEventListener('DOMContentLoaded', boot);
})();
