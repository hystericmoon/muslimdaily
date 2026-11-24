# :design module (formerly haho-design)

A Jetpack Compose design system for haho apps: frosted "liquid glass" surfaces over a soft
aurora backdrop, eight seed palettes in light and dark (or your own seed colour), Plus Jakarta
Sans, pill buttons, outlined chips, large rounded cards, and a "glass off" mode where every
surface is opaque. Every text pairing is tested to WCAG 2.2 AA.

This is the `:design` module of Adwam. It started as the shared haho-design library (v0.2.0, also used
by Smol) and was copied in on 24 Nov 2026 so Adwam can be redesigned without coordinating a second
repo. It is no longer published and takes no changes from haho-design. The rules for designing
screens with it are in [DESIGN.md](DESIGN.md).

- Package `id.haho.design`, Android library, Kotlin, Compose, Material 3.
- minSdk 26, compileSdk 37. AGP 9.4.1, Kotlin 2.3.21, Compose BOM 2026.09.00 (from `gradle/libs.versions.toml`).
- One runtime dependency besides Compose: `com.materialkolor:material-color-utilities` 4.1.1,
  used to generate schemes from custom seed colours.

## What's inside

| Package | Contents |
|---|---|
| `theme` | `HahoTheme`, `ThemeMode`, `HahoPalette` (8 palettes), `StatusColors` (`HahoTheme.status`), `paletteSwatch`, `colorSchemeFor`, `seedColorScheme`, `HahoTypography` / `hahoTypography`, `PlusJakartaSans`, `HahoShapes`, `Spacing`, `Dimens`, `Motion` |
| `glass` | `GlassColors` / `Glass.colors` / `glassColors`, `GlassBackdrop`, `GlassCard`, glass buttons (incl. `GlassOutlinedIconButton`), FAB, bottom sheet, dialog, `DockedBar`, segmented colours, press/enter/pop motion modifiers |
| `component` | `HahoScreen`, `HahoTopBar`, `SectionHeader`, `EmptyState`, `SettingsGroup`, `ListRow`, settings rows (navigation, info, switch, radio, stepper, segmented), `CheckRow`, `HahoFilterChip`, `HahoAssistChip`, `HahoOptionChip`, `HahoSegmentTrack` / `HahoSegment`, `CenterZeroSlider`, `HahoTextField`, `OpenSourceNoticesDialog`, `Badge`, `InfoNote`, previews |

## Usage

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HahoTheme(
                palette = HahoPalette.TEAL,
                themeMode = ThemeMode.SYSTEM,
                glass = true,
                // Or bring your own accent (palette is then ignored):
                // seedColor = Color(0xFF6750A4), greySurfaces = true,
            ) {
                GlassBackdrop {
                    HahoScreen(title = "Settings", onBack = ::finish) {
                        LazyColumn {
                            item {
                                SettingsGroup(title = "Appearance") {
                                    SwitchRow(title = "Glass effect", checked = glass, onCheckedChange = { glass = it })
                                    NavigationRow(title = "Colour", summary = "Teal", onClick = { /* … */ })
                                }
                            }
                            item {
                                GlassCard(Modifier.padding(HahoTheme.dimens.screenGutter)) {
                                    GlassButton(onClick = { /* … */ }) { Text("Save") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## Build and test

```
./gradlew :design:testDebugUnitTest :design:lintDebug
```

- `GlassContrastTest`: every glass role, every palette, light and dark, glass on and off, grey
  surfaces, and sample custom seeds, at 4.5:1 over the worst point of the backdrop.
- `PaletteContrastTest`: the opaque pairings from the palette generator, for the same set.
- `SeedSchemeTest`: the runtime seed pipeline reproduces the eight generated palettes.
- `StatusContrastTest`: success, warning and error as text on every surface, card, chrome and
  sheet, for the same set.
- `TokensTest`: motion stagger, type-scale scaling, glass-off opacity.

Palettes are generated, not hand-edited:

```
cd design/tools/color && npm ci && node generate_scheme.mjs   # writes Palettes.kt and design-tokens.md
```

## Stays in Adwam

Religion- and Adwam-specific parts are deliberately not in this library:

- `ArabicTypography`, `ArabicFont`, the Arabic font families and `ArabicText`.
- The active-phrase highlight and count-complete colours (`ExtendedColors`: highlightFill,
  onHighlightFill, highlightIndicator, completeFill, onCompleteFill).
- `TierBadge` and the `Tier` model.
- Adwam-only sizes (player main button, completion disc, highlight underline, timeline dot,
  placeholder word height).
- Adwam's strings; the library ships only a few generic strings (English and Indonesian).

## Licences

- Plus Jakarta Sans: SIL Open Font License 1.1, `licenses/plus_jakarta_sans_OFL.txt`. Apps that
  ship it should list it in their open-source notices.
- Bundled icons: Material Symbols (Apache 2.0).
- Library code: licence not yet chosen (the repo is private).
