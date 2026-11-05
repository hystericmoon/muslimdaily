// Deterministic motion helpers. Every value is a pure function of time t (seconds), so a frame
// renders identically every time and the renderer can step frame by frame.
(function () {
  function bezier(x1, y1, x2, y2) {
    const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx;
    const cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
    const sx = (t) => ((ax * t + bx) * t + cx) * t;
    const sy = (t) => ((ay * t + by) * t + cy) * t;
    const dx = (t) => (3 * ax * t + 2 * bx) * t + cx;
    return (x) => {
      if (x <= 0) return 0;
      if (x >= 1) return 1;
      let t = x;
      for (let i = 0; i < 8; i++) {
        const e = sx(t) - x, d = dx(t);
        if (Math.abs(e) < 1e-6) break;
        if (Math.abs(d) < 1e-6) break;
        t -= e / d;
      }
      let lo = 0, hi = 1;
      if (Math.abs(sx(t) - x) > 1e-5) {
        t = x;
        for (let i = 0; i < 40; i++) { const v = sx(t); if (v < x) lo = t; else hi = t; t = (lo + hi) / 2; }
      }
      return sy(t);
    };
  }
  // Curves from the animate skill's tables; never hand-rolled.
  const E = {
    out: bezier(0.23, 1, 0.32, 1),
    inOut: bezier(0.77, 0, 0.175, 1),
    drawer: bezier(0.32, 0.72, 0, 1),
    linear: (x) => Math.min(1, Math.max(0, x)),
  };
  const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
  const prog = (t, t0, dur) => clamp((t - t0) / dur);
  const mix = (a, b, k) => a + (b - a) * k;
  const tw = (t, t0, dur, a, b, ease = E.out) => mix(a, b, ease(prog(t, t0, dur)));
  // Apple-style spring: duration (perceptual) and bounce 0..1, settles at 1.
  function spring(t, t0, duration = 0.5, bounce = 0.2) {
    const x = t - t0;
    if (x <= 0) return 0;
    const zeta = 1 - bounce, w = (2 * Math.PI) / duration;
    if (zeta >= 1) return 1 - Math.exp(-w * x) * (1 + w * x);
    const wd = w * Math.sqrt(1 - zeta * zeta);
    return 1 - Math.exp(-zeta * w * x) * (Math.cos(wd * x) + ((zeta * w) / wd) * Math.sin(wd * x));
  }
  // In-then-out envelope: rises over `inD`, holds, falls over `outD` ending at t1.
  const env = (t, t0, t1, inD = 0.4, outD = 0.4, ease = E.out) =>
    Math.min(ease(prog(t, t0, inD)), 1 - E.inOut(prog(t, t1 - outD, outD)));
  // Seeded noise for film grain (mulberry32).
  function rng(seed) {
    return () => {
      seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
      let r = Math.imul(seed ^ (seed >>> 15), 1 | seed);
      r = (r + Math.imul(r ^ (r >>> 7), 61 | r)) ^ r;
      return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
    };
  }
  window.M = { bezier, E, clamp, prog, mix, tw, spring, env, rng };
})();
