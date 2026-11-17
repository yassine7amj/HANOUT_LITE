/* HANOUT LITE — طبقة البيانات للمنصة (متعددة التجار)
 * ------------------------------------------------------------------
 * كتخدم بجوج أوضاع:
 *   • cloud : Firebase (Firestore + Auth) ملي يكون projectId معبّى فـ firebase-config.js
 *   • local : وضع تجريبي محلي فالمتصفح (localStorage) — جهاز واحد، بلا مزامنة
 *
 * الواجهة (API) الموحّدة:
 *   HanoutStore.ready()                      -> Promise<boolean> (كاين سيرفر ولا لا)
 *   HanoutStore.mode                         -> 'cloud' | 'local'
 *   HanoutStore.slugify(text)                -> string
 *   HanoutStore.newOrderId()                 -> string
 *   HanoutStore.onAuth(cb) / currentUser() / waitAuth()
 *   HanoutStore.signIn(email,pass) / signUp(email,pass) / signOut()
 *   HanoutStore.getProfile() / setSlug(slug) / isAdminEmail(email)
 *   HanoutStore.getMerchant(slug) / listMerchants() / saveMerchant(m) / deleteMerchant(slug)
 *   HanoutStore.loadShop(slug) / saveCatalog(slug,{products,categories})
 *   HanoutStore.addOrder(slug,order) / listOrders(slug) / updateOrder(slug,id,patch) / deleteOrder(slug,id)
 *   HanoutStore.platformStats()              -> {merchants,active,orders,newOrders,byMerchant}
 */
(function () {
  'use strict';

  var LS = 'hanout_platform_v1';
  var SESSION = 'hanout_session_v1';
  var FBV = '10.12.5';
  var FB = 'https://www.gstatic.com/firebasejs/' + FBV + '/';
  var cfg = window.HANOUT_FB || {};
  var ADMINS = (cfg.adminEmails || []).map(function (e) { return String(e).toLowerCase(); });

  var db = null, auth = null, bootPromise = null, cloudReady = false;
  var currentUser = null, cbs = [], authResolvers = [], authArrived = false;

  /* ---------------- أدوات ---------------- */
  function lsGet() { try { return JSON.parse(localStorage.getItem(LS)) || {}; } catch (e) { return {}; } }
  function lsSet(o) { try { localStorage.setItem(LS, JSON.stringify(o)); } catch (e) { console.warn('[store] تخزين ممتلئ', e); } }
  function local() {
    var o = lsGet();
    o.merchants = o.merchants || {}; o.catalogs = o.catalogs || {};
    o.orders = o.orders || {}; o.accounts = o.accounts || {};
    return o;
  }
  function slugify(s) {
    var x = String(s || '').trim().toLowerCase()
      .replace(/[\s_]+/g, '-')
      .replace(/[^a-z0-9\u0600-\u06FF-]/g, '')
      .replace(/-+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40);
    return x || ('shop-' + Date.now().toString(36));
  }
  function nid(p) { return p + Date.now().toString(36) + Math.random().toString(36).slice(2, 6); }
  function isAdminEmail(e) { return ADMINS.indexOf(String(e || '').toLowerCase()) !== -1; }
  function fire() { cbs.slice().forEach(function (cb) { try { cb(currentUser); } catch (e) {} }); }
  function role(email) { return isAdminEmail(email) ? 'admin' : 'merchant'; }

  /* ---------------- تشغيل Firebase ---------------- */
  function script(src) {
    return new Promise(function (res, rej) {
      var s = document.createElement('script');
      s.src = src; s.async = false; s.onload = res; s.onerror = function () { rej(new Error('فشل تحميل ' + src)); };
      document.head.appendChild(s);
    });
  }
  function boot() {
    if (bootPromise) return bootPromise;
    bootPromise = new Promise(function (resolve) {
      if (!cfg.projectId) { resolve(false); return; }
      Promise.all([
        script(FB + 'firebase-app-compat.js'),
        script(FB + 'firebase-auth-compat.js'),
        script(FB + 'firebase-firestore-compat.js')
      ]).then(function () {
        try {
          if (!window.firebase.apps.length) window.firebase.initializeApp(cfg);
          auth = window.firebase.auth();
          db = window.firebase.firestore();
          // المشروع كيستعمل قاعدة بيانات مسمّاة ماشي «(default)».
          // SDK ديال compat ما كيقبلش نختارو القاعدة، إذن كنبدّلو المعرّف مباشرة
          // قبل أي عملية (الاتصال كيتصاوب أول مرة غير ملي نقراو/نكتبو).
          if (cfg.databaseId && db && db._delegate && db._delegate._databaseId) {
            db._delegate._databaseId.database = cfg.databaseId;
          }
          auth.onAuthStateChanged(function (u) {
            currentUser = u || null;
            if (!authArrived) { authArrived = true; authResolvers.splice(0).forEach(function (f) { f(); }); }
            fire();
          });
          cloudReady = true;
          resolve(true);
        } catch (e) { console.error('[store] init', e); resolve(false); }
      }).catch(function (e) { console.error('[store] sdk', e); resolve(false); });
    });
    return bootPromise;
  }

  /* ---------------- الحساب والدخول ---------------- */
  function signIn(email, pass) {
    email = String(email || '').trim();
    if (!email) return Promise.reject(new Error('البريد الإلكتروني مطلوب'));
    if (cfg.projectId && cloudReady) return auth.signInWithEmailAndPassword(email, pass);
    return new Promise(function (res) {
      var o = local(), uid = 'u-' + slugify(email);
      if (!o.accounts[uid]) o.accounts[uid] = { uid: uid, email: email, role: role(email), slug: '' };
      lsSet(o);
      currentUser = { uid: uid, email: email, local: true };
      try { localStorage.setItem(SESSION, JSON.stringify({ uid: uid, email: email })); } catch (e) {}
      fire(); res(currentUser);
    });
  }
  function signUp(email, pass) {
    email = String(email || '').trim();
    if (!email) return Promise.reject(new Error('البريد الإلكتروني مطلوب'));
    if (cfg.projectId && cloudReady) {
      return auth.createUserWithEmailAndPassword(email, pass).then(function (c) {
        return db.collection('users').doc(c.user.uid).set({ email: email, role: role(email), slug: '', createdAt: Date.now() }, { merge: true }).then(function () { return c.user; });
      });
    }
    return signIn(email, pass);
  }
  function signOut() {
    if (cfg.projectId && cloudReady) return auth.signOut();
    currentUser = null;
    try { localStorage.removeItem(SESSION); } catch (e) {}
    fire(); return Promise.resolve();
  }
  function onAuth(cb) { cbs.push(cb); cb(currentUser); }
  function waitAuth() {
    if (!cfg.projectId) return Promise.resolve(currentUser);
    if (authArrived) return Promise.resolve(currentUser);
    return new Promise(function (r) {
      authResolvers.push(function () { r(currentUser); });
      setTimeout(function () { r(currentUser); }, 4000);
    });
  }
  function getProfile() {
    if (!currentUser) return Promise.resolve(null);
    if (cfg.projectId && cloudReady) {
      return db.collection('users').doc(currentUser.uid).get().then(function (d) {
        var p = d.exists ? d.data() : {};
        return { email: currentUser.email, role: isAdminEmail(currentUser.email) ? 'admin' : (p.role || 'merchant'), slug: p.slug || '' };
      });
    }
    var o = local();
    var a = o.accounts[currentUser.uid] || {};
    return Promise.resolve({ email: currentUser.email, role: isAdminEmail(currentUser.email) ? 'admin' : (a.role || 'merchant'), slug: a.slug || '' });
  }
  function setSlug(slug) {
    if (!currentUser) return Promise.resolve();
    if (cfg.projectId && cloudReady) return db.collection('users').doc(currentUser.uid).set({ slug: slug, email: currentUser.email, role: 'merchant' }, { merge: true });
    var o = local();
    o.accounts[currentUser.uid] = o.accounts[currentUser.uid] || { uid: currentUser.uid, email: currentUser.email };
    o.accounts[currentUser.uid].slug = slug;
    lsSet(o);
    return Promise.resolve();
  }

  /* ---------------- التجار ---------------- */
  function getMerchant(slug) {
    if (!slug) return Promise.resolve(null);
    if (cfg.projectId && cloudReady) return db.collection('merchants').doc(slug).get().then(function (d) { return d.exists ? Object.assign({ slug: d.id }, d.data()) : null; });
    var o = local();
    return Promise.resolve(o.merchants[slug] || null);
  }
  function listMerchants() {
    if (cfg.projectId && cloudReady) {
      return db.collection('merchants').get().then(function (q) {
        var a = []; q.forEach(function (d) { a.push(Object.assign({ slug: d.id }, d.data())); });
        a.sort(function (x, y) { return (y.createdAt || 0) - (x.createdAt || 0); });
        return a;
      });
    }
    var o = local();
    var a = Object.keys(o.merchants).map(function (k) { return o.merchants[k]; });
    a.sort(function (x, y) { return (y.createdAt || 0) - (x.createdAt || 0); });
    return Promise.resolve(a);
  }
  function saveMerchant(m) {
    m = m || {};
    if (!m.slug) m.slug = slugify(m.name || 'shop');
    if (!m.createdAt) m.createdAt = Date.now();
    if (cfg.projectId && cloudReady) return db.collection('merchants').doc(m.slug).set(m, { merge: true });
    var o = local(); o.merchants[m.slug] = Object.assign({}, o.merchants[m.slug], m); lsSet(o);
    return Promise.resolve(m);
  }
  function deleteMerchant(slug) {
    if (cfg.projectId && cloudReady) {
      var ref = db.collection('merchants').doc(slug);
      return Promise.all([
        ref.collection('orders').get().then(function (q) { return Promise.all(q.docs.map(function (d) { return d.ref.delete(); })); }),
        ref.collection('data').get().then(function (q) { return Promise.all(q.docs.map(function (d) { return d.ref.delete(); })); })
      ]).then(function () { return ref.delete(); });
    }
    var o = local(); delete o.merchants[slug]; delete o.catalogs[slug]; delete o.orders[slug]; lsSet(o);
    return Promise.resolve();
  }
  function firstActiveSlug() {
    return listMerchants().then(function (list) {
      var a = list.filter(function (m) { return m.active !== false; });
      return a.length ? a[0].slug : null;
    });
  }

  /* ---------------- الكتالوغ ---------------- */
  function loadShop(slug) {
    return Promise.all([getMerchant(slug), loadCatalog(slug)]).then(function (r) {
      var m = r[0], c = r[1];
      if (!m && !c) return null;
      var products = (c && c.products) || [];
      var categories = (c && c.categories) || [];
      return {
        slug: slug,
        name: (m && m.name) || (c && c.name) || 'حانوت',
        phone: (m && m.phone) || (c && c.phone) || '',
        color: (m && m.color) || '',
        logo: (m && m.logo) || '',
        plan: (m && m.plan) || 'free',
        active: !m || m.active !== false,
        expiresAt: (m && m.expiresAt) || null,
        products: products,
        categories: categories
      };
    });
  }
  function loadCatalog(slug) {
    if (!slug) return Promise.resolve(null);
    if (cfg.projectId && cloudReady) {
      return db.collection('merchants').doc(slug).collection('data').doc('catalog').get()
        .then(function (d) { return d.exists ? d.data() : null; });
    }
    var o = local();
    return Promise.resolve(o.catalogs[slug] || null);
  }
  function saveCatalog(slug, cat) {
    if (!slug) return Promise.resolve();
    cat = cat || {};
    cat.updatedAt = Date.now();
    if (cfg.projectId && cloudReady) return db.collection('merchants').doc(slug).collection('data').doc('catalog').set(cat, { merge: true });
    var o = local(); o.catalogs[slug] = Object.assign({}, o.catalogs[slug], cat); lsSet(o);
    return Promise.resolve();
  }

  /* ---------------- الطلبات ---------------- */
  function orderCol(slug) { return db.collection('merchants').doc(slug).collection('orders'); }
  function addOrder(slug, order) {
    if (!slug || !order) return Promise.resolve();
    if (!order.id) order.id = nid('o');
    if (!order.ts) order.ts = Date.now();
    if (cfg.projectId && cloudReady) return orderCol(slug).doc(order.id).set(order);
    var o = local(); o.orders[slug] = o.orders[slug] || []; o.orders[slug].unshift(order); lsSet(o);
    return Promise.resolve(order);
  }
  function listOrders(slug) {
    if (!slug) return Promise.resolve([]);
    if (cfg.projectId && cloudReady) {
      return orderCol(slug).get().then(function (q) {
        var a = []; q.forEach(function (d) { a.push(Object.assign({ id: d.id }, d.data())); });
        a.sort(function (x, y) { return (y.ts || 0) - (x.ts || 0); });
        return a;
      });
    }
    var o = local();
    return Promise.resolve((o.orders[slug] || []).slice());
  }
  function updateOrder(slug, id, patch) {
    if (cfg.projectId && cloudReady) return orderCol(slug).doc(id).set(patch, { merge: true });
    var o = local(); var list = o.orders[slug] || [];
    list.forEach(function (x) { if (x.id === id) Object.assign(x, patch); });
    lsSet(o); return Promise.resolve();
  }
  function deleteOrder(slug, id) {
    if (cfg.projectId && cloudReady) return orderCol(slug).doc(id).delete();
    var o = local(); o.orders[slug] = (o.orders[slug] || []).filter(function (x) { return x.id !== id; }); lsSet(o);
    return Promise.resolve();
  }

  /* ---------------- إحصائيات المنصة ---------------- */
  function platformStats() {
    return listMerchants().then(function (list) {
      var active = list.filter(function (m) { return m.active !== false; }).length;
      var byMerchant = {};
      var chain = Promise.resolve();
      list.forEach(function (m) {
        chain = chain.then(function () {
          return listOrders(m.slug).then(function (os) {
            byMerchant[m.slug] = { total: os.length, new: os.filter(function (o) { return o.status === 'جديد'; }).length };
          });
        });
      });
      return chain.then(function () {
        var orders = 0, newOrders = 0;
        Object.keys(byMerchant).forEach(function (k) { orders += byMerchant[k].total; newOrders += byMerchant[k].new; });
        return { merchants: list.length, active: active, orders: orders, newOrders: newOrders, byMerchant: byMerchant };
      });
    });
  }

  window.HanoutStore = {
    get mode() { return (cfg.projectId && cloudReady) ? 'cloud' : 'local'; },
    get configured() { return !!cfg.projectId; },
    ready: function () { return boot(); },
    slugify: slugify,
    newOrderId: function () { return nid('o'); },
    isAdminEmail: isAdminEmail,
    onAuth: onAuth, currentUser: function () { return currentUser; }, waitAuth: waitAuth,
    signIn: signIn, signUp: signUp, signOut: signOut,
    getProfile: getProfile, setSlug: setSlug,
    getMerchant: getMerchant, listMerchants: listMerchants, saveMerchant: saveMerchant, deleteMerchant: deleteMerchant,
    firstActiveSlug: firstActiveSlug,
    loadShop: loadShop, loadCatalog: loadCatalog, saveCatalog: saveCatalog,
    addOrder: addOrder, listOrders: listOrders, updateOrder: updateOrder, deleteOrder: deleteOrder,
    platformStats: platformStats
  };

  /* رسميا: نحاولو نشغلو Firebase بكري */
  if (!cfg.projectId) {
    try { var _s = JSON.parse(localStorage.getItem(SESSION)); if (_s) currentUser = { uid: _s.uid, email: _s.email, local: true }; } catch (e) {}
  }
  boot();
})();
