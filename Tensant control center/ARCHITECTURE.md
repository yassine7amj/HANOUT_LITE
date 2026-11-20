# ARCHITECTURE — TENSANTS GROUP CONTROL CENTER

## ADR-001 — Desktop stack

**Status:** accepted for v0.1.0 (UI shell + motion system).

### Context

The master specification requires a real Windows desktop application (not a
website, not a PWA, not a browser wrapping the Next.js dashboard), with Fluent
design, dark/light, Arabic RTL, French, English and an independent project
directory `control-center/`. It names C# + .NET + WinUI 3 as the preferred
candidate and allows Tauri/Electron "only if technically justified".

### Decision

**Electron + vanilla HTML/CSS/JS** for the shell delivered in this iteration.

### Justification

1. **Verifiable output today.** WinUI 3 requires MSBuild + Windows App SDK on a
   Windows host. This environment has no `dotnet`/MSBuild and no Windows target,
   so C#/XAML delivered from here would be unverifiable code — unacceptable for
   a control application that will handle merchants, subscriptions and audit
   logs.
2. **One motion engine across platforms.** The spring-physics splash needs a
   per-frame simulator. A single `spring.js` (damped harmonic oscillator) runs
   the splash, KPI counters, card entrances and chart bars identically on
   Chromium for Windows — no XAML storyboard rewrite per control.
3. **Electron still produces a genuine Windows desktop app**: own process, own
   window chrome, own installer (NSIS + portable), own `.ico`, taskbar and
   Start-menu entry — not a website delivery mechanism.
4. **Security posture is explicit.** `contextIsolation: true`,
   `sandbox: true`, `nodeIntegration: false`, a 6-method `contextBridge` surface,
   strict CSP in both documents, `setWindowOpenHandler` forcing external links
   into the system browser.

### Consequences / exit path

- Renderer code is plain web tech; if the organization later mandates WinUI 3,
  `src/` maps 1:1 onto XAML views (sidebar, cards, tables, charts) with
  `MainWindow.xaml` hosting the same design tokens. `spring.js` ports to a
  spring `Vector3Animation` or a `CompositionTarget.Rendering` loop.
- The C# WinUI 3 project is intentionally **not** half-built in this repo: a
  second, untested implementation would be worse than none. It is a follow-up
  deliverable on a Windows build host.

## Process model

```
main process (electron/main.js)
├── BrowserWindow #1 — splash  520×620, frameless, transparent, alwaysOnTop
│     loads src/splash.html, closes once the spring choreography lands
├── BrowserWindow #2 — main    1500×940, frameless, hidden until splash ends
│     loads src/index.html
└── IPC (preload.js, contextBridge)
      window:minimize / window:toggle-maximize / window:close
      window:state (push) · app:info (invoke) · app:open-external
```

Single-instance lock: a second launch focuses the existing window.

## Renderer layers

| Layer | File | Responsibility |
|---|---|---|
| Physics | `js/spring.js` | `Spring.animate(from,to,{stiffness,damping,mass,velocity},cb)`, `Spring.timeline([...])` |
| i18n | `js/i18n.js` | EN/FR/AR dictionaries, `dir()` for RTL, `t(lang, key)` |
| Data | `js/data.js` | DEMO dataset only — single replace point for the real service layer |
| Shell | `js/app.js` | nav, views, charts (inline SVG), theming, spring entrance, counters |
| Tokens | `styles/app.css` | `[data-theme="dark|light"]` variables, RTL rules, components |

### Motion policy

All entrances use the spring engine; CSS transitions are limited to hover/press
micro-states (≤ 260 ms). `prefers-reduced-motion` short-circuits `Spring.animate`
to the final value, so the splash still completes without vestibular motion.

## Data contract (to implement)

```
HealthStatus  = online | degraded | error | offline | unknown
Stat          = { value, delta, window }         // never fabricated
Service       = { name, kind, status, version, latest, latency, lastCheck }
```

A service is rendered `online` **only** after a successful check; a missing or
failed check renders `unknown`/`error`. The demo badge is removed only when the
first authorized check succeeds.

## Brand system

- Navy `#04102E → #0A2466`, blue `#2B6BEC / #5B93FF`, gold `#F5A11C / #FFB93F`.
- Surfaces: `--surface`, `--surface-2`, `--surface-3`; borders at ≤ 24 % opacity.
- Type: Segoe UI Variable / Segoe UI, Noto Sans Arabic for RTL.
- Radii 9/12/16/20 px; shadows are two-layer and low-alpha.
- Approved logo only (`assets/logo*.png`, `assets/app.ico`); UI never redraws it.
