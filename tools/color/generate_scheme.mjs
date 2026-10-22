// Generates the app colour scheme from the seed colour using Material Color Utilities
// (tonal-spot variant), applies the parchment / soft-dark surface overrides, adds the
// gold accent roles, and checks WCAG contrast for every pairing the UI relies on.
//
//   cd tools/color && npm ci && node generate_scheme.mjs
//
// Writes app/src/main/java/id/haho/muslimdaily/core/designsystem/theme/Color.kt
// and docs/design-tokens.md. Exits non-zero if a required contrast pair fails.

import {
  argbFromHex, hexFromArgb, Hct, SchemeTonalSpot, MaterialDynamicColors, TonalPalette,
} from '@material/material-color-utilities';
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const SEED = '#1F5F5B';
const LIGHT_SURFACE = '#FBF8F1';
const DARK_SURFACE = '#121412';
const GOLD = '#C9A227';

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

function build(isDark) {
  const scheme = new SchemeTonalSpot(Hct.fromInt(argbFromHex(SEED)), isDark, 0.0);
  const out = {};
  for (const r of roles) out[r] = MaterialDynamicColors[r].getArgb(scheme);

  // Surface override: build a low-chroma palette around the requested surface colour so the
  // container steps stay in the same temperature as the reading surface.
  const surfaceHex = isDark ? DARK_SURFACE : LIGHT_SURFACE;
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

  // Gold accent: fill / non-text indicator only, never text on a light surface.
  const gold = argbFromHex(GOLD);
  const goldPalette = TonalPalette.fromInt(gold);
  out.highlightFill = isDark ? goldPalette.tone(30) : goldPalette.tone(90);
  out.onHighlightFill = isDark ? goldPalette.tone(90) : goldPalette.tone(10);
  // Underline under the active phrase: must reach 3:1 against the surface.
  out.highlightIndicator = isDark ? gold : out.primary;
  out.completeFill = gold;
  out.onCompleteFill = goldPalette.tone(10);
  return out;
}

const light = build(false);
const dark = build(true);

const checks = [
  // [foreground, background, minimum, note]
  ['onSurface', 'surface', 4.5, 'body text'],
  ['onSurfaceVariant', 'surface', 4.5, 'secondary text'],
  ['onSurfaceVariant', 'surfaceContainerHighest', 4.5, 'secondary text on cards'],
  ['onSurface', 'surfaceContainerLow', 4.5, 'text on dua cards'],
  ['primary', 'surface', 4.5, 'text buttons / links'],
  ['onPrimary', 'primary', 4.5, 'filled buttons'],
  ['onPrimaryContainer', 'primaryContainer', 4.5, 'tonal buttons, badges'],
  ['onSecondaryContainer', 'secondaryContainer', 4.5, 'selected nav pill, chips'],
  ['onTertiaryContainer', 'tertiaryContainer', 4.5, 'tier badge'],
  ['onErrorContainer', 'errorContainer', 4.5, 'prohibited-time banner'],
  ['outline', 'surface', 3.0, 'component boundaries'],
  ['onHighlightFill', 'highlightFill', 4.5, 'Arabic text on active-phrase fill'],
  ['highlightIndicator', 'surface', 3.0, 'active-phrase underline'],
  ['highlightIndicator', 'highlightFill', 3.0, 'underline on its own fill'],
  ['onCompleteFill', 'completeFill', 3.0, 'check glyph on count-complete fill'],
];

let failed = false;
const report = [];
for (const [name, s] of [['Light', light], ['Dark', dark]]) {
  report.push(`\n### ${name}\n\n| Foreground | Background | Ratio | Needs | Use |\n|---|---|---|---|---|`);
  for (const [fg, bg, min, note] of checks) {
    const r = contrast(s[fg], s[bg]);
    const ok = r >= min;
    if (!ok) failed = true;
    report.push(`| \`${fg}\` ${hexFromArgb(s[fg])} | \`${bg}\` ${hexFromArgb(s[bg])} | ${r.toFixed(2)}:1 ${ok ? 'pass' : '**FAIL**'} | ${min}:1 | ${note} |`);
  }
  const goldOnSurface = contrast(argbFromHex(GOLD), s.surface);
  report.push(`\nRaw gold ${GOLD} vs surface: ${goldOnSurface.toFixed(2)}:1 ` +
    (goldOnSurface < 3 ? '(below 3:1, so gold is never the only indicator on this surface)' : ''));
}

const kHex = (argb) => `Color(0xFF${hexFromArgb(argb).slice(1).toUpperCase()})`;
const allRoles = [...roles, 'highlightFill', 'onHighlightFill', 'highlightIndicator', 'completeFill', 'onCompleteFill'];
const kotlin = `// Generated by tools/color/generate_scheme.mjs from seed ${SEED} (tonal spot). Do not edit by hand.
package id.haho.muslimdaily.core.designsystem.theme

import androidx.compose.ui.graphics.Color

internal object LightTokens {
${allRoles.map((r) => `    val ${r} = ${kHex(light[r])}`).join('\n')}
}

internal object DarkTokens {
${allRoles.map((r) => `    val ${r} = ${kHex(dark[r])}`).join('\n')}
}
`;
writeFileSync(resolve(root, 'app/src/main/java/id/haho/muslimdaily/core/designsystem/theme/Color.kt'), kotlin);

const md = `# Colour tokens

Generated by \`tools/color/generate_scheme.mjs\`. Seed \`${SEED}\`, tonal spot. Surfaces
overridden to \`${LIGHT_SURFACE}\` (light) and \`${DARK_SURFACE}\` (dark); container steps are
taken from a low-chroma palette built on each surface's own hue so cards stay warm on
parchment and neutral on the dark surface.

Gold \`${GOLD}\` is used only as a fill or non-text indicator:

| Role | Light | Dark | Used for |
|---|---|---|---|
| \`highlightFill\` | ${hexFromArgb(light.highlightFill)} | ${hexFromArgb(dark.highlightFill)} | Background of the phrase being recited |
| \`onHighlightFill\` | ${hexFromArgb(light.onHighlightFill)} | ${hexFromArgb(dark.onHighlightFill)} | Arabic text sitting on that fill |
| \`highlightIndicator\` | ${hexFromArgb(light.highlightIndicator)} (primary) | ${hexFromArgb(dark.highlightIndicator)} (gold) | Underline under the active phrase |
| \`completeFill\` | ${hexFromArgb(light.completeFill)} | ${hexFromArgb(dark.completeFill)} | Count-complete disc |
| \`onCompleteFill\` | ${hexFromArgb(light.onCompleteFill)} | ${hexFromArgb(dark.onCompleteFill)} | Check glyph inside the disc |

In light mode the underline uses primary teal rather than gold because raw gold on parchment
is below 3:1. The active phrase is therefore marked by fill + underline + TalkBack state, never
by colour alone.

## Contrast checks
${report.join('\n')}
`;
writeFileSync(resolve(root, 'docs/design-tokens.md'), md);
console.log(md);
if (failed) {
  console.error('Contrast check failed');
  process.exit(1);
}
