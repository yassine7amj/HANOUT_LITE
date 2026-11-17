#!/usr/bin/env node
/*
 * HANOUT LITE — مولّد ملف twa-manifest.json
 * ------------------------------------------------------------------
 * كيولّد إعداد تطبيق أندرويد (TWA) واحد:
 *   • بلا --shop : التطبيق العام ديال المنصة (كيعرض أول حانوت نشط).
 *   • بـ --shop  : تطبيق خاص بحانوت واحد (بالاسم ديالو + رابط الحانوت).
 *
 * الاستعمال:
 *   node twa/make-manifest.mjs --base "https://USER.github.io/HANOUT_LITE" \
 *        [--name "حانوت النخيل"] [--shop hanout-nakhil] [--package ma.hanout.nakhil]
 *
 *   --base     رابط الموقع كامل، بلا سلاش فالآخر (إلزامي)
 *   --name     اسم التطبيق لي كيبان تحت الأيقونة (الافتراضي: HANOUT LITE)
 *   --shop     slug الحانوت، كيتزاد فالرابط: store.html?shop=<slug>
 *   --package  معرّف الحزمة Android (إلا تخلا كيتولّد أوتوماتيكيا)
 *
 * كيكتب النتيجة فـ twa/twa-manifest.json (بحال ما كيتسنّى bubblewrap).
 */
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));

/* كيقرا الجوج صيغات: --key=value  و  --key value */
const raw = process.argv.slice(2);
const argv = {};
for (let i = 0; i < raw.length; i++) {
  const eq = /^--([^=\s]+)=([\s\S]*)$/.exec(raw[i]);
  if (eq) { argv[eq[1]] = eq[2]; continue; }
  const bare = /^--([^=\s]+)$/.exec(raw[i]);
  if (!bare) continue;
  const next = raw[i + 1];
  if (next !== undefined && !/^--/.test(next)) { argv[bare[1]] = next; i++; }
  else argv[bare[1]] = '';
}

const base = String(argv.base || '').replace(/\/+$/, '');
if (!/^https?:\/\/[^/\s]+/.test(base)) {
  console.error('× --base إلزامي. مثال: --base https://user.github.io/HANOUT_LITE');
  process.exit(1);
}
const host = new URL(base).host;
const prefix = new URL(base).pathname.replace(/\/+$/, ''); // '' ولا '/HANOUT_LITE'

const shop = String(argv.shop || '').trim();
const name = String(argv.name || 'HANOUT LITE').trim() || 'HANOUT LITE';

/* معرّف الحزمة خاصو يكون ASCII بحروف صغيرة. إلا كان الاسم/الـslug بالعربية،
   كنولّدو لاحقة من hash باش كل تاجر ياخد معرّف ثابت ومختلف. */
function ascii(s) { return String(s).toLowerCase().replace(/[^a-z0-9]+/g, ''); }
function hash36(s) {
  let h = 0;
  for (const ch of String(s)) h = (h * 31 + ch.codePointAt(0)) >>> 0;
  return h.toString(36);
}
let tail = 'lite'; // التطبيق العام ديال المنصة
if (shop) {
  tail = ascii(shop) || ascii(name);
  if (!tail) tail = 's' + hash36(shop || name);
}
if (/^[0-9]/.test(tail)) tail = 's' + tail;
const packageId = String(argv.package || '').trim() || ('ma.hanout.' + tail.slice(0, 24));

const startUrl = shop
  ? prefix + '/store.html?shop=' + encodeURIComponent(shop)
  : prefix + '/store.html';

const manifest = {
  packageId,
  host,
  name,
  launcherName: name.slice(0, 30),
  display: 'standalone',
  orientation: 'portrait',
  /* androidbrowserhelper 2.7.x كيتطلّب minSdkVersion 24 */
  minSdkVersion: 24,
  themeColor: '#003DA5',
  themeColorDark: '#002A73',
  navigationColor: '#003DA5',
  navigationColorDark: '#002A73',
  navigationDividerColor: '#003DA5',
  navigationDividerColorDark: '#002A73',
  backgroundColor: '#F5F8FD',
  splashScreenFadeOutDuration: 300,
  startUrl,
  iconUrl: base + '/icon-512.png',
  maskableIconUrl: base + '/icon-512.png',
  webManifestUrl: base + '/manifest.webmanifest',
  enableNotifications: false,
  enableSiteSettingsShortcut: true,
  fallbackType: 'customtabs',
  features: {},
  shortcuts: [],
  signingKey: { path: './android.keystore', alias: 'android' },
  fingerprints: []
};

const out = join(HERE, 'twa-manifest.json');
writeFileSync(out, JSON.stringify(manifest, null, 2) + '\n');
console.log('✔ تولّد ' + out);
console.log('  packageId : ' + packageId);
console.log('  name      : ' + name);
console.log('  startUrl  : ' + startUrl);
