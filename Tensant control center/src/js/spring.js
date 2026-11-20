/* ---------------------------------------------------------------------------
 * spring.js — tiny damped-harmonic-oscillator animation engine (no deps).
 *
 * Physics: m·x'' = -k·(x - target) - c·x'
 *   k = stiffness, c = 2·ζ·√(k·m)   (ζ = damping ratio, 1 = critically damped)
 *
 * Any value the UI animates (scale, translate, opacity, rotation, progress)
 * is driven by a real spring, so motion accelerates, overshoots and settles
 * instead of following a canned cubic-bezier curve.
 * ------------------------------------------------------------------------- */
(function () {
  'use strict';

  const DEFAULTS = {
    stiffness: 220, // k
    damping: 14,    // c  (lower => more bounce)
    mass: 1,        // m
    precision: 0.0008,
    velocity: 0,    // initial velocity (units / s)
  };

  function prefersReducedMotion() {
    return window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  /**
   * Animate a single scalar from `from` to `to` with spring physics.
   * Returns a promise that resolves when the spring has settled.
   */
  function animate(from, to, options, onUpdate) {
    const opts = Object.assign({}, DEFAULTS, options || {});
    if (prefersReducedMotion()) {
      onUpdate(to, { done: true, value: to });
      return Promise.resolve(to);
    }

    let x = Number(from) || 0;
    const target = Number(to) || 0;
    let v = opts.velocity;
    let raf = 0;
    let last = performance.now();
    const k = opts.stiffness;
    const c = opts.damping;
    const m = opts.mass;

    return new Promise((resolve) => {
      function step(now) {
        // Fixed-ish step keeps the simulation stable on slow frames.
        let dt = Math.min((now - last) / 1000, 1 / 30);
        last = now;

        // Sub-step for numerical stability at high stiffness.
        const sub = 2;
        const h = dt / sub;
        for (let i = 0; i < sub; i++) {
          const a = (-k * (x - target) - c * v) / m;
          v += a * h;
          x += v * h;
        }

        const settled = Math.abs(v) < 0.02 && Math.abs(target - x) < opts.precision;
        onUpdate(settled ? target : x, { done: settled, value: x });

        if (settled) {
          cancelAnimationFrame(raf);
          resolve(target);
          return;
        }
        raf = requestAnimationFrame(step);
      }
      raf = requestAnimationFrame(step);
    });
  }

  /** Animate several independent channels in one rAF loop. */
  function timeline(channels) {
    return Promise.all(
      channels.map((ch) =>
        animate(ch.from, ch.to, ch.options, (value, meta) =>
          ch.onUpdate(value, meta)
        )
      )
    );
  }

  /** Spring-eased progress 0..1 mapped through a bounce profile (CSS helper). */
  function cssSpring(durationMs, springOptions) {
    const frames = [];
    const steps = 24;
    const o = Object.assign({}, DEFAULTS, springOptions || {});
    // Simulate once to build a keyframe list for CSS animations.
    let x = 0, v = 0;
    const h = durationMs / 1000 / steps;
    for (let i = 0; i <= steps; i++) {
      const a = (-o.stiffness * (x - 1) - o.damping * v) / o.mass;
      v += a * h;
      x += v * h;
      frames.push(Math.round(Math.max(0, x) * 1000) / 1000);
    }
    return frames;
  }

  window.Spring = { animate, timeline, cssSpring, prefersReducedMotion, DEFAULTS };
})();
