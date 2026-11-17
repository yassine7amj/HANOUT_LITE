# HANOUT LITE — حانوت حومتك فتيليفونك

تطبيق ويب (صفحة واحدة) لكتالوج الحانوت: تصفّح المنتوجات والعروض، سلة، وإرسال الطلب مباشرة عبر واتساب. يخدم بلا إنترنت ويتثبّت على الهاتف (PWA).

## الملفات

| الملف | الوصف |
| --- | --- |
| `index.html` | التطبيق كامل (HTML + CSS + JS) |
| `manifest.webmanifest` | بيانات تثبيت التطبيق (PWA) |
| `service-worker.js` | تخزين للعمل بدون إنترنت |
| `icon.svg` / `icon-512.jpg` / `logo.png` | الأيقونات والشعار |
| `.nojekyll` | يمنع معالجة الملفات عبر Jekyll على GitHub Pages |
| `.github/workflows/pages.yml` | نشر تلقائي على GitHub Pages مع كل push |

## النشر على دومين GitHub المجاني

الريبو الجذر هو **هذا المجلد** (الذي فيه `index.html`).

1) أنشئ ريبو جديد على GitHub باسم مثلاً `hanout-lite` (public).

2) من داخل هذا المجلد نفّذ:

```bash
git init
git add .
git commit -m "HANOUT LITE v0.5"
git branch -M main
git remote add origin https://github.com/<USERNAME>/hanout-lite.git
git push -u origin main
```

3) من صفحة الريبو على GitHub: **Settings → Pages**، وفي **Build and deployment → Source** اختر **GitHub Actions**.

4) انتظر دقيقة حتى يكمل الـ workflow (تابع من تبويب **Actions**). الرابط النهائي يكون:

```
https://<USERNAME>.github.io/hanout-lite/
```

> ملاحظة: إذا كان ريبو الجذر عندك هو المجلد الأب (الذي يحوي مجلد `HANOUT_LITE`)، بدّل في `pages.yml` السطر `path: .` إلى `path: HANOUT_LITE`.

## بعد النشر

- افتح الرابط من الهاتف ثم «إضافة إلى الشاشة الرئيسية» (أو زر **تثبيت التطبيق** داخله).
- بدّل روابط `og:image` و`twitter:image` في `index.html` إلى الرابط المطلق للصورة لمعاينة أفضل عند المشاركة.
- البيانات (المنتوجات، الطلبات) تُحفظ محلياً في متصفح التاجر فقط (localStorage)، ولا تُزامن بين الأجهزة بعد.
