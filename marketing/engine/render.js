// Render an HTML scene to video (or a still) frame by frame.
//
//   node marketing/engine/render.js <scene.html> <out.mp4> [--query fmt=916&lang=id] [--fps 30] [--sub 2]
//   node marketing/engine/render.js <scene.html> <out.png> --still 12.5 [--query ...]
//
// The scene must define window.SCENE = { width, height, duration, renderAt(t) } (renderAt may be
// async) and may define window.SCENE.captions = [{start, end, text}] for the subtitle file.
// --sub N renders N sub-frames per output frame and averages them (motion blur).
const { chromium } = require('playwright');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

function args() {
  const a = process.argv.slice(2);
  const o = { scene: a[0], out: a[1], fps: 30, sub: 2, query: '', still: null };
  for (let i = 2; i < a.length; i += 2) {
    const k = a[i].replace(/^--/, '');
    o[k] = k === 'query' ? a[i + 1] : Number(a[i + 1]);
  }
  return o;
}

function srtTime(s) {
  const ms = Math.round(s * 1000);
  const p = (n, w = 2) => String(n).padStart(w, '0');
  return `${p(Math.floor(ms / 3600000))}:${p(Math.floor(ms / 60000) % 60)}:${p(Math.floor(ms / 1000) % 60)},${p(ms % 1000, 3)}`;
}

(async () => {
  const o = args();
  const browser = await chromium.launch();
  const url = 'file://' + path.resolve(o.scene) + (o.query ? '?' + o.query : '');
  const probe = await browser.newPage();
  await probe.goto(url);
  await probe.waitForFunction(() => window.SCENE && window.SCENE.ready === true, null, { timeout: 30000 });
  const { width, height, duration } = await probe.evaluate(() => ({
    width: SCENE.width, height: SCENE.height, duration: SCENE.duration }));
  await probe.close();

  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });
  await page.goto(url);
  await page.waitForFunction(() => window.SCENE && window.SCENE.ready === true, null, { timeout: 30000 });
  await page.evaluate(() => document.fonts.ready);

  if (o.still !== null && !Number.isNaN(o.still)) {
    await page.evaluate((t) => SCENE.renderAt(t), o.still);
    await page.screenshot({ path: o.out, type: o.out.endsWith('.jpg') ? 'jpeg' : 'png', quality: o.out.endsWith('.jpg') ? 92 : undefined });
    await browser.close();
    console.log('still', o.out, width + 'x' + height, 't=' + o.still);
    return;
  }

  const rate = o.fps * o.sub;
  const n = Math.round(duration * rate);
  const vf = o.sub > 1 ? `tmix=frames=${o.sub},framestep=${o.sub},format=yuv420p` : 'format=yuv420p';
  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(rate),
    '-c:v', 'png', '-i', '-', '-vf', vf, '-r', String(o.fps), '-c:v', 'libx264', '-preset', 'slow',
    '-crf', '16', '-profile:v', 'high', '-movflags', '+faststart', o.out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const t0 = Date.now();
  for (let i = 0; i < n; i++) {
    await page.evaluate((t) => SCENE.renderAt(t), i / rate);
    const buf = await page.screenshot({ type: 'png' });
    if (!ff.stdin.write(buf)) await new Promise((r) => ff.stdin.once('drain', r));
    if (i % (rate * 2) === 0) process.stdout.write(`\r${o.out}: ${((i / n) * 100).toFixed(0)}%`);
  }
  ff.stdin.end();
  await new Promise((r) => ff.on('close', r));
  const caps = await page.evaluate(() => SCENE.captions || []);
  if (caps.length) {
    const srt = caps.map((c, i) => `${i + 1}\n${srtTime(c.start)} --> ${srtTime(c.end)}\n${c.text}\n`).join('\n');
    fs.writeFileSync(o.out.replace(/\.mp4$/, '.srt'), srt);
  }
  await browser.close();
  console.log(`\r${o.out}: ${n} frames at ${rate} fps -> ${o.fps} fps in ${((Date.now() - t0) / 1000).toFixed(0)} s`);
})().catch((e) => { console.error(e); process.exit(1); });
