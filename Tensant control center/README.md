# TENSANTS GROUP CONTROL CENTER

Enterprise Windows desktop administration & system control for the TENSANTS GROUP
ecosystem — deep-navy / gold brand system, Fluent-inspired surfaces, Arabic RTL,
French and English, dark and light themes, and a spring-physics splash screen.

> **Status:** UI shell — v0.1.0. Navigation, theming, i18n, window chrome and the
> motion system are complete and runnable. Data is DEMO data (`src/js/data.js`);
> the app shows a permanent “Demo data — backend not connected” badge until a real
> authorized health check passes. No production number is hardcoded as truth.

---

## Run it (Windows 10/11)

```bash
# Node.js 20+ required
cd control-center
npm install
npm start          # splash (spring) -> main window
npm run dev        # same, with DevTools
```

Package a real Windows installer / portable exe:

```bash
npm run dist       # NSIS installer + portable, both x64, release/
```

The assisted NSIS setup uses a TENSANTS GROUP-branded welcome/finish sidebar,
offers all-users versus current-user installation, lets users choose the desktop
shortcut and launch-after-install options, and keeps the installation folder
selectable. The Windows icon, installer icon and taskbar icon all come from
`assets/app.ico` (generated from the approved TENSANTS GROUP logo).

---

## What is inside

```
control-center/
├── electron/
│   ├── main.js        # frameless splash window + main window, IPC, single instance
│   └── preload.js     # minimal contextBridge surface (no node in renderer)
├── src/
│   ├── splash.html    # brand splash, driven by spring physics
│   ├── index.html     # app shell (title bar, sidebar, content)
│   ├── styles/
│   │   ├── app.css    # design tokens, dark/light, RTL, layout, components
│   │   └── splash.css # splash stage, emblem, gold arc, progress
│   └── js/
│       ├── spring.js  # damped harmonic oscillator engine (no dependencies)
│       ├── i18n.js    # EN / FR / AR dictionaries
│       ├── data.js    # DEMO dataset (replace with authorized services)
│       └── app.js     # navigation, views, charts, theming, spring entrance
├── assets/            # logo + generated app.ico / icon sizes
├── ARCHITECTURE.md    # stack decision (ADR-001) and structure
└── package.json
```

## The logo, everywhere it belongs

| Surface | Asset |
|---|---|
| Splash screen | `assets/logo.png` (300 px, spring drop-in) |
| Title bar | `assets/logo-64.png` |
| Sidebar brand block | `assets/logo.png` in a 46 px rounded tile |
| Window / taskbar / installer | `assets/app.ico` (16→256 px, multi-size) |

The official artwork is used as supplied — nothing redrawn or substituted. A text
fallback (`TG` monogram) renders only if the image itself fails to load.

## Splash — Bounce / Spring Physics

The splash is not a CSS easing curve. `src/js/spring.js` integrates
`m·x″ = −k·(x − target) − c·x′` every frame, so every channel accelerates,
overshoots and settles:

| Channel | Behaviour |
|---|---|
| Emblem | drops from −80 px, `k=260 · c=10.5` → clear bounce, lands and settles |
| Emblem scale | `0.55 → 1`, `k=300 · c=9.5` → elastic pop with overshoot |
| Gold arc | dash-offset springs open (`k=90`), then the ring drifts 360° |
| Wordmark | “CONTROL” then “CENTER” rise with a late, damped settle |
| Progress | `k=55` heavy spring to 86 %, then a second push to 100 % |
| Sheen | single diagonal light sweep after the emblem lands |

Respects `prefers-reduced-motion: reduce` (skips to final state). The main window
is only revealed after the choreography lands (~2.9 s), so the splash never
“flashes”.

## Keyboard / UX notes

- `EN / FR / AR` cycles from the title bar; Arabic flips the whole layout to RTL.
- Theme toggle (dark/light) persists in `localStorage`, same as the language.
- The splash window and the main window are frameless; drag anywhere on the
  title bar, or the splash card itself.
- Close / minimize / maximize are handled in the main process, never in the
  renderer.

## Wiring the real backend (next step)

1. Replace `src/js/data.js` with a service layer that calls the authorized
   central business services only.
2. Health checks: a service is only shown **online** after a successful check;
   otherwise `unknown` — never optimistic.
3. Keep the demo badge until the first successful authorized check.
4. Do not place this project inside HANOUT LITE or HANCLIENT; it stays
   independent in `control-center/`.

## Credits

Designed & developed by **FIPCOM Network** · TENSANTS GROUP — *Control · Manage · Grow*
