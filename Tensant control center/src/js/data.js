/* ---------------------------------------------------------------------------
 * data.js — the Control Center data model.
 *
 * Empty by design: the shell holds no bundled sample values. Every figure is
 * pulled from the central API through the Electron main process
 * (electron/central-client.js) and merged into this object by app.js once a
 * real health check passes. Until then the UI renders its empty states.
 * ------------------------------------------------------------------------- */
(function () {
  'use strict';

  window.DATA = {
    connected: false,
    generatedAt: null,
    kpis: {
      merchants: 0,
      active: 0,
      customers: 0,
      products: 0,
      orders: 0,
      subscriptions: 0,
      notifications: 0,
      reports: 0,
      sync: 0,
    },
    series: [],
    merchants: [],
    services: [],
    activity: [],
    subscriptions: [],
  };
})();
