package id.haho.design.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import id.haho.design.glass.LocalGlass
import id.haho.design.glass.LocalOverlayState
import id.haho.design.glass.OverlayState
import id.haho.design.glass.glassColors

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolves [mode] to dark or light, following the system for [ThemeMode.SYSTEM]. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * The scheme [HahoTheme] uses: [seedColor] when set, else Android 12+ wallpaper colours when
 * [dynamicColor] is on, else [palette].
 */
@Composable
fun rememberHahoColorScheme(
    palette: HahoPalette,
    dark: Boolean,
    seedColor: Color? = null,
    dynamicColor: Boolean = false,
    greySurfaces: Boolean = false,
): ColorScheme {
    val context = LocalContext.current
    return remember(palette, dark, seedColor, dynamicColor, greySurfaces) {
        when {
            seedColor != null -> seedColorScheme(seedColor, dark, greySurfaces)
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else -> colorSchemeFor(palette, dark, greySurfaces)
        }
    }
}

/**
 * The haho design system: Material 3 with a palette, liquid-glass roles, Plus Jakarta Sans,
 * spacing, dimensions and motion.
 *
 * @param palette one of the eight built-in palettes; ignored when [seedColor] is set.
 * @param themeMode light, dark or follow the system.
 * @param glass frosted glass surfaces; off resolves every glass role to an opaque colour.
 * @param seedColor an app's own accent; a full light/dark scheme is generated from it.
 * @param dynamicColor use the wallpaper colours on Android 12+ (when no [seedColor]).
 * @param greySurfaces keep surfaces pure neutral grey; only the accent roles take on colour.
 * @param readingScale scales [HahoTheme.reading] for an in-app text-size setting.
 * @param typography the UI type scale; defaults to Plus Jakarta Sans.
 */
@Composable
fun HahoTheme(
    palette: HahoPalette = HahoPalette.Default,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    glass: Boolean = true,
    seedColor: Color? = null,
    dynamicColor: Boolean = false,
    greySurfaces: Boolean = false,
    readingScale: Float = 1f,
    typography: Typography = HahoTypography,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    val colorScheme = rememberHahoColorScheme(palette, dark, seedColor, dynamicColor, greySurfaces)
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val reduceMotion = remember(context, inspection) {
        !inspection && Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
    }
    val reading = remember(typography, readingScale) { typography.scaled(readingScale) }
    val glassColors = remember(colorScheme, dark, glass) { glassColors(colorScheme, dark, glass) }
    val overlays = remember { OverlayState() }

    CompositionLocalProvider(
        LocalGlass provides glassColors,
        LocalOverlayState provides overlays,
        LocalReadingTypography provides reading,
        LocalIsDark provides dark,
        LocalStatusColors provides statusColors(dark),
        LocalMotion provides Motion(reduced = reduceMotion),
        LocalSpacing provides Spacing(),
        LocalDimens provides Dimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = HahoShapes,
            content = content,
        )
    }
}

/** Type scale for reading text (long body copy); follows [HahoTheme]'s readingScale. */
val LocalReadingTypography = staticCompositionLocalOf { HahoTypography }

/** Whether the current [HahoTheme] resolved to dark. */
val LocalIsDark = staticCompositionLocalOf { false }

/** Single access point for theme values beyond [MaterialTheme]. */
object HahoTheme {
    val reading: Typography
        @Composable @ReadOnlyComposable get() = LocalReadingTypography.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val dimens: Dimens
        @Composable @ReadOnlyComposable get() = LocalDimens.current
    val motion: Motion
        @Composable @ReadOnlyComposable get() = LocalMotion.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current
    /** Success and warning colours; danger is `colorScheme.error`. */
    val status: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}
