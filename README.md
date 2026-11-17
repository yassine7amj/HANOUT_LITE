# HANOUT LITE — منصة متعددة التجار

تطبيق ويب (PWA) لكتالوج الحانوت: تصفّح المنتوجات والعروض، سلة، وإرسال الطلب مباشرة عبر واتساب.
هاد النسخة ولّات **منصة مركزية** كتسيّر **مجموعة تجار**، وكل حانوت عندو **رابط خاص** و**تطبيق أندرويد** بالاسم ديالو.

## الصفحات

| الصفحة | الوصف |
| --- | --- |
| `index.html` | واجهة التعريف بمنصة HANWEB + تجربة وتحميل التطبيق (نسخة الزبون ونسخة التاجر) |
| `store.html` | واجهة الزبون (الهاتف) — `store.html?shop=<slug>` كتحدد الحانوت |
| `web.html` | واجهة الزبون (نسخة الويب المكتبية) |
| `merchant.html` | فضاء التاجر (محمي بحساب): منتوجات، أصناف، طلبات، رابط + QR |
| `login.html` | دخول/تسجيل التجار والمسؤولين |
| `admin.html` | منصة المدير العام: تحكم فجميع التجار، الاشتراكات، الإحصائيات |

## الملفات

| الملف | الوصف |
| --- | --- |
| `app.js` | المنطق المشترك لواجهة الزبون + فضاء التاجر |
| `store.js` | طبقة البيانات الموحّدة: Firebase (cloud) أو localStorage (local) |
| `firebase-config.js` | إعدادات السيرفر + إيميلات المدراء العامين |
| `manifest.webmanifest` | بيانات التثبيت (PWA — أندرويد و iOS) |
| `service-worker.js` | تخزين للعمل بدون إنترنت |
| `twa/` | إعداد تطبيق أندرويد (Bubblewrap/TWA) + مولّد `twa-manifest.json` |
| `.github/workflows/android-apk.yml` | بناء APK أندرويد من GitHub Actions |
| `.nojekyll` | يمنع معالجة الملفات عبر Jekyll على GitHub Pages |

## تشغيل السيرفر (اختياري — للعمل بلا سيرفر كيخدم محلياً)

بلا `projectId`، التطبيق كيخدم فالوضع التجريبي المحلي (البيانات فالمتصفح فقط).
باش تفعّل المزامنة بين جميع الأجهزة:

1. خلق مشروع مجاني على <https://console.firebase.google.com>.
2. فعّل **Authentication → Email/Password** و **Firestore Database**.
3. Project settings → Your apps → Web app → انسخ القيم فـ `firebase-config.js`.
4. زيد إيميلات المدراء العامين فـ `adminEmails` (دول كيدخلو لـ `admin.html`).

## النشر على GitHub Pages

1. من صفحة الريبو: **Settings → Pages** → **Build and deployment → Source** = **Deploy from a branch** → branch `main` / folder `/ (root)`.
2. الرابط: `https://<USERNAME>.github.io/<REPO>/`.

> بيانات المنصة (Firestore) كتحتاج قواعد أمان. بلا قواعد مناسبة، Firestore كيرفض القراءة/الكتابة.

## توزيع التطبيقات

### أندرويد (APK)

البناء كيتم فالسحابة عبر GitHub Actions (بلا حاجة لـ Java/Android SDK عندك):

1. GitHub → **Actions** → **بناء تطبيق أندرويد (APK)** → **Run workflow**.
2. عمّر **اسم التطبيق**، و**slug الحانوت** إلا بغيت تطبيق خاص بتاجر واحد (خليه خاوي للتطبيق العام).
3. حمّل `app-release-signed.apk` من **Artifacts** ولا من **Releases**.

باش التحديثات يخدمو (وباش Google Play يقبل التطبيق)، خاص **مفتاح توقيع ثابت** — شوف التعليقات فوق فـ [android-apk.yml](.github/workflows/android-apk.yml).

محلياً (إلا كان عندك JDK 17 + Android SDK):

```bash
cd twa
bubblewrap update --skipVersionUpgrade
bubblewrap build --skipPwaValidation
```

**ملاحظة على شريط العنوان:** باش التطبيق يتحل بلا شريط URL، خاص ملف
`assetlinks.json` يكون فجذر الدومين: `https://<USERNAME>.github.io/.well-known/assetlinks.json`.
هادا كيحتاج ريبو سميتو `<USERNAME>.github.io`. بلا هادشي التطبيق كيخدم عادي ولكن كيبان شريط العنوان.

### آيفون / iOS

آيفون ما فيهش APK. الحل هو **PWA**: من **Safari** → زر المشاركة → **إضافة إلى الشاشة الرئيسية**.
الأيقونات والواجهة (`apple-touch-icon` + `apple-mobile-web-app-*`) واجدة من قبل، والتطبيق كيخدم بوضع standalone.

## ملاحظات

- بدّل روابط `og:image` / `twitter:image` فـ `index.html` و`store.html` و`web.html` إلى الرابط المطلق ديال الصور.
