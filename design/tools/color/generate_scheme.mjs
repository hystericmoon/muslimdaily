// Generates the haho-design colour palettes from their seed colours using Material Color
// Utilities (tonal-spot variant, neutral for grey seeds), applies the parchment / soft-dark
// surface overrides, and checks WCAG contrast for every pairing the UI relies on, in every
// palette. Custom seeds at runtime go through the same steps in SeedScheme.kt.
//
//   cd tools/color && npm ci && node generate_scheme.mjs
//
// Writes src/main/java/id/haho/design/theme/Palettes.kt and design-tokens.md (both in design/). Exits non-zero if a required contrast pair fails.

import {
  argbFromHex, hexFromArgb, Hct, SchemeTonalSpot, SchemeNeutral, MaterialDynamicColors, TonalPalette,
} from '@material/material-color-utilities';
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

// The first palette is the default. Teal keeps the original parchment surfaces; the others tint
// their surfaces faintly toward their own hue so each theme reads as a whole.
const PALETTES = [
  { id: 'TEAL', seed: '#1F5F5B', lightSurface: '#FBF8F1', darkSurface: '#121412' },
  { id: 'NAVY', seed: '#1E3A5F' },
  { id: 'FOREST', seed: '#2F5D3A' },
  { id: 'SLATE', seed: '#4A5560', neutral: true },
  { id: 'OLIVE', seed: '#5F6131' },
  { id: 'ROSE', seed: '#A8436A' },
  { id: 'LAVENDER', seed: '#7356A6' },
  { id: 'PEACH', seed: '#C0674A' },
];

function surfaceFor(p, isDark) {
  const explicit = isDark ? p.darkSurface : p.lightSurface;
  if (explicit) return explicit;
  const hue = Hct.fromInt(argbFromHex(p.seed)).hue;
  return hexFromArgb(TonalPalette.fromHueAndChroma(hue, 4).tone(isDark ? 7 : 98));
}

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

const roles = [
  'primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer', 'inversePrimary',
  'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer',
  'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer',
  'error', 'onError', 'errorContainer', 'onErrorContainer',
  'background', 'onBackground', 'surface', 'onSurface', 'surfaceVariant', 'onSurfaceVariant',
  'surfaceTint', 'inverseSurface', 'inverseOnSurface', 'outline', 'outlineVariant', 'scrim',
  'surfaceBright', 'surfaceDim', 'surfaceContainerLowest', 'surfaceContainerLow',
  'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest',
];

function luminance(argb) {
  const c = [(argb >> 16) & 255, (argb >> 8) & 255, argb & 255].map((v) => {
    const s = v / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}

function contrast(a, b) {
  const [l1, l2] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

function build(p, isDark) {
  const Scheme = p.neutral ? SchemeNeutral : SchemeTonalSpot;
  const scheme = new Scheme(Hct.fromInt(argbFromHex(p.seed)), isDark, 0.0);
  const out = {};
  for (const r of roles) out[r] = MaterialDynamicColors[r].getArgb(scheme);

  // Surface override: build a low-chroma palette around the requested surface colour so the
  // container steps stay in the same temperature as the reading surface.
  const surfaceHex = surfaceFor(p, isDark);
  const sHct = Hct.fromInt(argbFromHex(surfaceHex));
  const neutral = TonalPalette.fromHueAndChroma(sHct.hue, Math.max(sHct.chroma, 2));
  const tones = isDark
    ? { surfaceDim: 6, surfaceBright: 24, surfaceContainerLowest: 4, surfaceContainerLow: 10,
        surfaceContainer: 12, surfaceContainerHigh: 17, surfaceContainerHighest: 22, surfaceVariant: 22 }
    : { surfaceDim: 87, surfaceBright: 98, surfaceContainerLowest: 100, surfaceContainerLow: 96,
        surfaceContainer: 94, surfaceContainerHigh: 92, surfaceContainerHighest: 90, surfaceVariant: 90 };
  for (const [k, t] of Object.entries(tones)) out[k] = neutral.tone(t);
  out.surface = argbFromHex(surfaceHex);
  out.background = out.surface;
  if (isDark) out.surfaceDim = out.surface; else out.surfaceBright = out.surface;
  out.onSurface = neutral.tone(isDark ? 90 : 10);
  out.onBackground = out.onSurface;
  out.inverseSurface = neutral.tone(isDark ? 90 : 20);
  out.inverseOnSurface = neutral.tone(isDark ? 20 : 95);

  return out;
}

const built = PALETTES.map((p) => ({ ...p, light: build(p, false), dark: build(p, true) }));

const checks = [
  // [foreground, background, minimum, note]
  ['onSurface', 'surface', 4.5, 'body text'],
  ['onSurfaceVariant', 'surface', 4.5, 'secondary text'],
  ['onSurfaceVariant', 'surfaceContainerHighest', 4.5, 'secondary text on cards'],
  ['onSurface', 'surfaceContainerLow', 4.5, 'text on cards'],
  ['primary', 'surface', 4.5, 'text buttons / links'],
  ['onPrimary', 'primary', 4.5, 'filled buttons'],
  ['onPrimaryContainer', 'primaryContainer', 4.5, 'tonal buttons, badges'],
  ['onSecondaryContainer', 'secondaryContainer', 4.5, 'selected nav pill, chips'],
  ['onTertiaryContainer', 'tertiaryContainer', 4.5, 'tertiary badge'],
  ['onErrorContainer', 'errorContainer', 4.5, 'alert banner'],
  ['outline', 'surface', 3.0, 'component boundaries'],
];

let failed = false;
const report = [];
for (const p of built) {
  for (const [name, s] of [['light', p.light], ['dark', p.dark]]) {
    report.push(`\n### ${p.id} ${name} (seed ${p.seed})\n\n| Foreground | Background | Ratio | Needs | Use |\n|---|---|---|---|---|`);
    for (const [fg, bg, min, note] of checks) {
      const r = contrast(s[fg], s[bg]);
      const ok = r >= min;
      if (!ok) failed = true;
      report.push(`| \`${fg}\` ${hexFromArgb(s[fg])} | \`${bg}\` ${hexFromArgb(s[bg])} | ${r.toFixed(2)}:1 ${ok ? 'pass' : '**FAIL**'} | ${min}:1 | ${note} |`);
    }
  }
}

const kHex = (argb) => `Color(0xFF${hexFromArgb(argb).slice(1).toUpperCase()})`;
const allRoles = roles;
const camel = (id) => id[0] + id.slice(1).toLowerCase();
const tokens = (s) => `ColorTokens(\n${allRoles.map((r) => `        ${r} = ${kHex(s[r])},`).join('\n')}\n    )`;
const kotlin = `// Generated by tools/color/generate_scheme.mjs. Do not edit by hand.
package id.haho.design.theme

import androidx.compose.ui.graphics.Color

/** Colour themes, in picker order. The first is the default. */
enum class HahoPalette(val seed: Color) {
${built.map((p) => `    ${p.id}(${kHex(argbFromHex(p.seed))}),`).join('\n')}
    ;

    companion object {
        /** The default palette: the first in picker order. */
        val Default: HahoPalette get() = entries.first()
    }
}

internal class ColorTokens(
${allRoles.map((r) => `    val ${r}: Color,`).join('\n')}
)

internal fun paletteTokens(palette: HahoPalette, dark: Boolean): ColorTokens = when (palette) {
${built.map((p) => `    HahoPalette.${p.id} -> if (dark) ${camel(p.id)}Dark else ${camel(p.id)}Light`).join('\n')}
}

${built.map((p) => `private val ${camel(p.id)}Light = ${tokens(p.light)}

private val ${camel(p.id)}Dark = ${tokens(p.dark)}`).join('\n\n')}
`;
writeFileSync(resolve(root, 'src/main/java/id/haho/design/theme/Palettes.kt'), kotlin);

const teal = built[0];
const md = `# Colour tokens

Generated by \`tools/color/generate_scheme.mjs\`. Each palette starts from a seed colour (tonal
spot; the grey Slate seed uses the neutral variant). Teal, the default, keeps the parchment
\`${teal.lightSurface}\` / soft-dark \`${teal.darkSurface}\` surfaces; the other palettes tint
their surfaces faintly toward their own hue. Container steps come from a low-chroma palette
built on each surface's hue.

| Palette | Seed |
|---|---|
${built.map((p) => `| ${p.id} | ${p.seed} |`).join('\n')}

Glass roles (translucent fills, rim, sheen) are derived from these schemes at runtime; see
\`GlassTokens.kt\` and \`GlassContrastTest\`.

## Contrast checks
${report.join('\n')}
`;
writeFileSync(resolve(root, 'design-tokens.md'), md);
console.log(failed ? 'Contrast check FAILED' : `OK: ${built.length} palettes`);
if (failed) {
  console.log(report.filter((l) => l.includes('FAIL') || l.startsWith('\n###')).join('\n'));
  process.exit(1);
}
