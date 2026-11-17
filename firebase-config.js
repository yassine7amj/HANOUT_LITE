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
  apiKey: "AIzaSyAYGVHK-caah07WVxdN7LdJzJ8edM6Ht9Y",
  authDomain: "hanout-lite.firebaseapp.com",
  projectId: "hanout-lite",
  storageBucket: "hanout-lite.firebasestorage.app",
  messagingSenderId: "593726572210",
  appId: "1:593726572210:web:4bf1bdae76587ab03babab",

  /* اسم قاعدة بيانات Firestore. المشروع كيستعمل قاعدة مسمّاة «default»
     (ماشي «(default)»)، لهذا كنمرّروها هنا. */
  databaseId: "default",

  /* الإيميلات ديال المدراء العامين (Super Admins).
     هاد الناس كيدخلو لمنصة التحكم admin.html وكيسيّرو جميع التجار. */
  adminEmails: ["yasskimadon@gmail.com"]
};
