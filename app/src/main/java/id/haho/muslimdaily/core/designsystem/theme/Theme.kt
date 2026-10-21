package id.haho.muslimdaily.core.designsystem.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightScheme = with(LightTokens) {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer, inversePrimary = inversePrimary,
        secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary, tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error, onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        background = background, onBackground = onBackground, surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant, surfaceTint = surfaceTint,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        outline = outline, outlineVariant = outlineVariant, scrim = scrim,
        surfaceBright = surfaceBright, surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

private val DarkScheme = with(DarkTokens) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer, inversePrimary = inversePrimary,
        secondary = secondary, onSecondary = onSecondary, secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary, tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error, onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer,
        background = background, onBackground = onBackground, surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant, surfaceTint = surfaceTint,
        inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
        outline = outline, outlineVariant = outlineVariant, scrim = scrim,
        surfaceBright = surfaceBright, surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

@Composable
fun MuslimDailyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    arabicScale: Float = 1f,
    translationScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    val inspection = LocalInspectionMode.current
    val reduceMotion = remember(context, inspection) {
        !inspection && Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
    }
    val arabic = remember(arabicScale) { ArabicTypography().scaled(arabicScale) }
    val readingTypography = remember(translationScale) { AppTypography.scaled(translationScale) }

    CompositionLocalProvider(
        LocalArabicTypography provides arabic,
        LocalReadingTypography provides readingTypography,
        LocalExtendedColors provides if (dark) DarkExtendedColors else LightExtendedColors,
        LocalMotion provides Motion(reduced = reduceMotion),
        LocalSpacing provides Spacing(),
        LocalDimens provides Dimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Type scale for translations and other reading text; follows the in-app translation size. */
val LocalReadingTypography = androidx.compose.runtime.staticCompositionLocalOf { AppTypography }

/** Single access point for the app's theme values beyond [MaterialTheme]. */
object AppTheme {
    val arabic: ArabicTypography
        @Composable @ReadOnlyComposable get() = LocalArabicTypography.current
    val reading: Typography
        @Composable @ReadOnlyComposable get() = LocalReadingTypography.current
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val dimens: Dimens
        @Composable @ReadOnlyComposable get() = LocalDimens.current
    val motion: Motion
        @Composable @ReadOnlyComposable get() = LocalMotion.current
}
