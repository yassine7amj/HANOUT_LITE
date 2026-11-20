'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// Minimal, explicit surface. No node access leaks into the renderer.
contextBridge.exposeInMainWorld('controlCenter', {
  minimize: () => ipcRenderer.send('window:minimize'),
  toggleMaximize: () => ipcRenderer.send('window:toggle-maximize'),
  close: () => ipcRenderer.send('window:close'),
  onWindowState: (cb) =>
    ipcRenderer.on('window:state', (_e, payload) => cb(payload)),
  info: () => ipcRenderer.invoke('app:info'),
  openExternal: (url) => ipcRenderer.send('app:open-external', url),
  data: {
    health: () => ipcRenderer.invoke('data:health'),
    overview: () => ipcRenderer.invoke('data:overview'),
    createMerchant: (merchant) => ipcRenderer.invoke('data:createMerchant', merchant),
  },
  auth: {
    state: () => ipcRenderer.invoke('auth:state'),
    login: (email, password) => ipcRenderer.invoke('auth:login', { email, password }),
    logout: () => ipcRenderer.invoke('auth:logout'),
  },
  updates: {
    check: () => ipcRenderer.invoke('update:check'),
    install: () => ipcRenderer.invoke('update:install'),
    state: () => ipcRenderer.invoke('update:state'),
    onStatus: (cb) =>
      ipcRenderer.on('update:status', (_e, payload) => cb(payload)),
  },
});
