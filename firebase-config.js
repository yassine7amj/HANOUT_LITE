/* HANOUT LITE — إعدادات السيرفر (Firebase)
 * ------------------------------------------------------------------
 * 1) خلق مشروع مجاني على https://console.firebase.google.com
 * 2) فعّل Authentication (Email/Password) + Firestore Database
 * 3) Project settings → Your apps → Web app → انسخ القيم هنا.
 *
 * إلا خليتي projectId خاوي، التطبيق كيخدم فالوضع التجريبي المحلي
 * (البيانات كتبقى فالمتصفح فقط، بلا مزامنة بين الأجهزة).
 */
window.HANOUT_FB = {
  apiKey: "",
  authDomain: "",
  projectId: "",
  storageBucket: "",
  messagingSenderId: "",
  appId: "",

  /* الإيميلات ديال المدراء العامين (Super Admins).
     هاد الناس كيدخلو لمنصة التحكم admin.html وكيسيّرو جميع التجار. */
  adminEmails: ["admin@hanout.ma"]
};
