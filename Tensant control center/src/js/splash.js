/* ---------------------------------------------------------------------------
 * splash.js — TENSANTS GROUP splash choreography.
 *
 * Every channel below is driven by the spring engine (js/spring.js):
 * the emblem drops in with real overshoot, the gold arc springs open,
 * the wordmark settles with a light bounce and the progress bar eases
 * out like it has mass.
 * ------------------------------------------------------------------------- */
(function () {
  'use strict';

  const S = window.Spring;
  const $ = (id) => document.getElementById(id);

  const card = $('card');
  const emblem = $('emblem');
  const logo = $('logo');
  const shine = document.querySelector('.emblem__shine');
  const arc = $('arc');
  const arcHead = $('arcHead');
  const ring = document.querySelector('.ring');
  const mainWord = document.querySelector('.wordmark__main');
  const subWord = document.querySelector('.wordmark__sub');
  const tagline = $('tagline');
  const progressWrap = $('progressWrap');
  const fill = $('progressFill');
  const percent = $('percent');
  const status = $('status');

  const ARC_LENGTH = 653; // matches r=104 circumference (2πr ≈ 653.4)
  const STATUS = [
    [0.05, 'Initializing secure session'],
    [0.34, 'Verifying application health'],
    [0.62, 'Loading administration modules'],
    [0.88, 'Preparing workspace'],
  ];

  function setStatus(p) {
    let label = STATUS[0][1];
    for (const [threshold, text] of STATUS) if (p >= threshold) label = text;
    if (status.textContent !== label) status.textContent = label;
    percent.textContent = Math.round(p * 100) + '%';
  }

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  async function play() {
    // 1. Card materialises — soft, slightly under-damped so it feels physical.
    S.animate(0.94, 1, { stiffness: 170, damping: 22 }, (v) => {
      card.style.transform = `scale(${v})`;
    });
    S.animate(0, 1, { stiffness: 150, damping: 30 }, (v) => {
      card.style.opacity = v;
    });

    await sleep(90);

    // 2. Emblem drops from above with a bouncy landing (ζ ≈ 0.33).
    S.timeline([
      {
        from: -80, to: 0,
        options: { stiffness: 260, damping: 10.5, mass: 1 },
        onUpdate: (v) => { emblem.dataset.y = v; },
      },
      {
        from: 0.55, to: 1,
        options: { stiffness: 300, damping: 9.5, mass: 1 },
        onUpdate: (v) => { emblem.dataset.scale = v; },
      },
      {
        from: 0, to: 1,
        options: { stiffness: 210, damping: 34 },
        onUpdate: (v) => { emblem.style.opacity = v; },
      },
    ]).then(() => {
      // settle exactly on target, then the sheen pass
      emblem.style.transform = 'translate3d(0,0,0) scale(1)';
      emblem.style.opacity = '1';
      sweepShine();
    });

    // The transform channels share one compositor write per frame.
    S.animate(0, 1, { stiffness: 260, damping: 14, precision: 1 }, () => {
      const y = Number(emblem.dataset.y || 0);
      const sc = Number(emblem.dataset.scale || 0.55);
      emblem.style.transform = `translate3d(0, ${y}px, 0) scale(${sc})`;
    });

    // 3. Gold arc springs open, ring gently fades up.
    S.animate(0, 1, { stiffness: 90, damping: 18 }, (v) => {
      ring.style.opacity = v;
      arc.style.strokeDashoffset = String(ARC_LENGTH * (1 - v));
    });
    S.animate(0, 1, { stiffness: 120, damping: 20 }, (v) => {
      arcHead.style.opacity = v;
    });
    // slow orbital drift once open
    S.animate(0, 360, { stiffness: 30, damping: 22 }, (v) => {
      ring.style.transform = `translate(-50%, -60%) rotate(${v}deg)`;
    });

    // 4. Wordmark rises — word first, "GROUP" a beat later, tagline last.
    await sleep(340);
    S.timeline([
      {
        from: 22, to: 0,
        options: { stiffness: 240, damping: 13 },
        onUpdate: (v) => {
          mainWord.style.transform = `translateY(${v}px)`;
        },
      },
      {
        from: 0, to: 1,
        options: { stiffness: 200, damping: 28 },
        onUpdate: (v) => { mainWord.style.opacity = v; },
      },
    ]);

    await sleep(90);
    S.timeline([
      {
        from: 22, to: 0,
        options: { stiffness: 260, damping: 12 },
        onUpdate: (v) => {
          subWord.style.transform = `translateY(${v}px)`;
        },
      },
      {
        from: 0, to: 1,
        options: { stiffness: 200, damping: 28 },
        onUpdate: (v) => { subWord.style.opacity = v; },
      },
    ]);

    await sleep(80);
    S.timeline([
      {
        from: 12, to: 0,
        options: { stiffness: 220, damping: 16 },
        onUpdate: (v) => { tagline.style.transform = `translateY(${v}px)`; },
      },
      {
        from: 0, to: 1,
        options: { stiffness: 180, damping: 30 },
        onUpdate: (v) => { tagline.style.opacity = v; },
      },
      {
        from: 0, to: 1,
        options: { stiffness: 180, damping: 30 },
        onUpdate: (v) => { progressWrap.style.opacity = v; },
      },
    ]);

    // 5. Progress: heavy spring, then a short second push to 100%.
    S.animate(0, 0.86, { stiffness: 55, damping: 16 }, (v) => {
      fill.style.width = (v * 100).toFixed(2) + '%';
      setStatus(Math.min(v, 0.999));
    }).then(() => {
      S.animate(0.86, 1, { stiffness: 120, damping: 19 }, (v) => {
        fill.style.width = (v * 100).toFixed(2) + '%';
        setStatus(v);
      });
    });

    // 6. Leave the stage.
    await sleep(2050);
    card.classList.add('card--leaving');
  }

  /** Diagonal light sweep across the logo — "glass" highlight. */
  function sweepShine() {
    S.animate(0, 1, { stiffness: 80, damping: 24 }, (v) => {
      shine.style.opacity = String(Math.min(v, 1) * 0.55);
      shine.style.backgroundPosition = `${130 - v * 260}% 0`;
    }).then(() => { shine.style.opacity = '0'; });
  }

  // Logo asset can fail only if the build is broken — never leave a blank card.
  logo.addEventListener('error', () => {
    logo.replaceWith(
      Object.assign(document.createElement('div'), {
        textContent: 'TG',
        style:
          'width:100%;height:100%;display:grid;place-items:center;' +
          'font:800 92px "Segoe UI";color:#fff;' +
          'background:linear-gradient(160deg,#1657D6,#0A2466);border-radius:26%;',
      })
    );
  });

  play();
})();
