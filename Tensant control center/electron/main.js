'use strict';

const { app, BrowserWindow, ipcMain, shell, nativeTheme } = require('electron');
const path = require('path');
const { autoUpdater } = require('electron-updater');
const centralClient = require('./central-client');

const IS_DEV = process.argv.includes('--dev');
const ASSETS = path.join(__dirname, '..', 'assets');
const SRC = path.join(__dirname, '..', 'src');

// The splash owns the brand moment: the main window is only revealed once the
// spring choreography has landed (logo bounce -> wordmark -> progress).
const SPLASH_MIN_MS = 2900;
const startedAt = Date.now();

let splashWindow = null;
let mainWindow = null;

app.setAppUserModelId('group.tensants.controlcenter');

// Single instance: focus the running window instead of opening a second copy.
if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });
}

function createSplash() {
  splashWindow = new BrowserWindow({
    width: 520,
    height: 620,
    frame: false,
    transparent: true,
    resizable: false,
    movable: true,
    center: true,
    show: false,
    skipTaskbar: true,
    alwaysOnTop: true,
    backgroundColor: '#00000000',
    icon: path.join(ASSETS, 'app.ico'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  splashWindow.loadFile(path.join(SRC, 'splash.html'));
  splashWindow.once('ready-to-show', () => splashWindow.show());
  splashWindow.on('closed', () => {
    splashWindow = null;
  });
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1500,
    height: 940,
    minWidth: 1180,
    minHeight: 720,
    show: false,
    backgroundColor: nativeTheme.shouldUseDarkColors ? '#0A1430' : '#F4F6FB',
    icon: path.join(ASSETS, 'app.ico'),
    title: 'TENSANTS GROUP CONTROL CENTER',
    titleBarStyle: 'hidden',
    titleBarOverlay: false,
    frame: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      spellcheck: false,
    },
  });

  mainWindow.loadFile(path.join(SRC, 'index.html'));

  mainWindow.once('ready-to-show', () => {
    const wait = Math.max(0, SPLASH_MIN_MS - (Date.now() - startedAt));
    setTimeout(() => {
      if (!mainWindow || mainWindow.isDestroyed()) return;
      mainWindow.show();
      mainWindow.focus();
      if (splashWindow) {
        splashWindow.close();
        splashWindow = null;
      }
      if (IS_DEV) mainWindow.webContents.openDevTools({ mode: 'detach' });
    }, wait);
  });

  mainWindow.on('maximize', () => mainWindow.webContents.send('window:state', { maximized: true }));
  mainWindow.on('unmaximize', () => mainWindow.webContents.send('window:state', { maximized: false }));

  // External links open in the user's browser, never inside the app shell.
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:\/\//i.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  // The signed-in session lives in the per-user profile, not in the install
  // directory, so an update never signs the operator out.
  centralClient.configure({ sessionFile: path.join(app.getPath('userData'), 'session.json') });

  createSplash();
  // The splash owns the brand moment; the main window is prepared behind it.
  createMainWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createSplash();
      createMainWindow();
    }
  });

  // Silent background update checks start once the shell is up.
  startUpdates();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

/* ---------------------------- IPC: window chrome --------------------------- */

ipcMain.on('window:minimize', () => mainWindow && mainWindow.minimize());
ipcMain.on('window:toggle-maximize', () => {
  if (!mainWindow) return;
  if (mainWindow.isMaximized()) mainWindow.unmaximize();
  else mainWindow.maximize();
});
ipcMain.on('window:close', () => mainWindow && mainWindow.close());

ipcMain.handle('app:info', () => ({
  version: app.getVersion(),
  electron: process.versions.electron,
  chrome: process.versions.chrome,
  platform: process.platform,
  darkMode: nativeTheme.shouldUseDarkColors,
}));

ipcMain.on('app:open-external', (_event, url) => {
  if (typeof url === 'string' && /^https?:\/\//i.test(url)) shell.openExternal(url);
});

/* --------------------------- IPC: central data ---------------------------- */
// The renderer cannot reach the network (CSP connect-src 'self'); the main
// process is the single client of the central services.

ipcMain.handle('data:health', () => centralClient.health());
ipcMain.handle('data:overview', () => centralClient.overview());
ipcMain.handle('data:createMerchant', (_event, merchant) => centralClient.createMerchant(merchant));

/* ---------------------------- IPC: operator sign-in ------------------------ */
// The operator signs in with the same account the website and the mobile
// applications use. Credentials never reach the renderer's own storage: they
// are handed to the main process, exchanged for a session, and only the
// session's owner identity travels back.

ipcMain.handle('auth:state', () => centralClient.authState());
ipcMain.handle('auth:login', (_event, credentials) =>
  centralClient.signIn(credentials && credentials.email, credentials && credentials.password));
ipcMain.handle('auth:logout', () => centralClient.signOut());

/* -------------------------- IPC: application updates ----------------------- */
// Updates are silent in the background: the check and the download both happen
// on their own, then the renderer is told the build is ready and the operator
// confirms the install. Only the NSIS (installed) build can auto-update.

// The feed is a rolling release tag so that Android releases in the same
// repository can never take over GitHub's "latest" pointer.
const UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000;
const UPDATE_FIRST_CHECK_MS = 10 * 1000;

let updateState = { status: 'idle', version: null, percent: 0, message: null };

function publishUpdateState(patch) {
  updateState = { ...updateState, ...patch };
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('update:status', updateState);
  }
  return updateState;
}

// `autoDownload` keeps the download off the operator's screen; the only
// decision they are asked to make is when to restart.
function wireUpdaterEvents() {
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('checking-for-update', () => publishUpdateState({ status: 'checking', percent: 0 }));
  autoUpdater.on('update-available', (info) => publishUpdateState({ status: 'downloading', version: info && info.version, percent: 0 }));
  autoUpdater.on('download-progress', (p) => publishUpdateState({ status: 'downloading', percent: Math.round((p && p.percent) || 0) }));
  autoUpdater.on('update-not-available', () => publishUpdateState({ status: 'up-to-date', percent: 0 }));
  autoUpdater.on('update-downloaded', (info) => publishUpdateState({ status: 'ready', version: info && info.version, percent: 100 }));
  autoUpdater.on('error', (err) => publishUpdateState({ status: 'error', message: String((err && err.message) || err) }));
}

async function checkForUpdates() {
  if (!app.isPackaged) {
    return publishUpdateState({ status: 'dev', message: 'Updates are disabled in development.' });
  }
  if (updateState.status === 'downloading' || updateState.status === 'ready') return updateState;
  try {
    await autoUpdater.checkForUpdates();
  } catch (err) {
    publishUpdateState({ status: 'error', message: String((err && err.message) || err) });
  }
  return updateState;
}

function installUpdate() {
  if (updateState.status !== 'ready') return false;
  // Defer so the IPC reply reaches the renderer before the app goes down.
  setImmediate(() => autoUpdater.quitAndInstall(true, true));
  return true;
}

function startUpdates() {
  wireUpdaterEvents();
  setTimeout(() => checkForUpdates(), UPDATE_FIRST_CHECK_MS);
  setInterval(() => checkForUpdates(), UPDATE_CHECK_INTERVAL_MS);
}

ipcMain.handle('update:check', () => checkForUpdates());
ipcMain.handle('update:install', () => installUpdate());
ipcMain.handle('update:state', () => updateState);
