package id.haho.muslimdaily.core.designsystem.glass

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private val White = Color.White
private val Gold = Color(0xFFC9A227)

/**
 * Derives the glass roles from the active colour scheme (so dynamic colour keeps working).
 * Alphas are checked against the worst-case backdrop in GlassContrastTest.
 */
fun glassColors(scheme: ColorScheme, dark: Boolean, enabled: Boolean): GlassColors {
    if (!enabled) {
        return GlassColors(
            enabled = false,
            base = scheme.surface,
            glows = emptyList(),
            card = scheme.surfaceContainerLow,
            chrome = scheme.surfaceContainer,
            sheet = scheme.surfaceContainerLow,
            tinted = scheme.secondaryContainer,
            onTinted = scheme.onSecondaryContainer,
            alert = scheme.errorContainer,
            onAlert = scheme.onErrorContainer,
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            tonal = scheme.secondaryContainer,
            onTonal = scheme.onSecondaryContainer,
            clear = Color.Transparent,
            rimTop = Color.Transparent,
            rimBottom = Color.Transparent,
            sheen = Color.Transparent,
        )
    }
    return if (!dark) {
        GlassColors(
            enabled = true,
            base = scheme.surface,
            glows = listOf(
                scheme.primaryContainer to 0.55f,
                scheme.tertiaryContainer to 0.60f,
                Gold to 0.16f,
            ),
            card = White.copy(alpha = 0.58f),
            chrome = White.copy(alpha = 0.62f),
            sheet = scheme.surfaceContainerLow.copy(alpha = 0.92f),
            tinted = scheme.secondaryContainer.copy(alpha = 0.72f),
            onTinted = scheme.onSecondaryContainer,
            alert = scheme.errorContainer,
            onAlert = scheme.onErrorContainer,
            primary = scheme.primary.copy(alpha = 0.90f),
            onPrimary = scheme.onPrimary,
            tonal = scheme.secondaryContainer.copy(alpha = 0.70f),
            onTonal = scheme.onSecondaryContainer,
            clear = White.copy(alpha = 0.34f),
            rimTop = White.copy(alpha = 0.95f),
            rimBottom = White.copy(alpha = 0.30f),
            sheen = White.copy(alpha = 0.32f),
        )
    } else {
        GlassColors(
            enabled = true,
            base = scheme.surface,
            glows = listOf(
                scheme.primaryContainer to 0.28f,
                scheme.tertiaryContainer to 0.26f,
                Gold to 0.06f,
            ),
            card = White.copy(alpha = 0.07f),
            chrome = White.copy(alpha = 0.08f),
            sheet = scheme.surfaceContainerLow.copy(alpha = 0.92f),
            tinted = scheme.secondaryContainer.copy(alpha = 0.62f),
            onTinted = scheme.onSecondaryContainer,
            // An opaque errorContainer slab outweighs the hero card in dark; a wash keeps it a note.
            alert = scheme.errorContainer.copy(alpha = 0.55f),
            onAlert = scheme.onErrorContainer,
            primary = scheme.primary.copy(alpha = 0.88f),
            onPrimary = scheme.onPrimary,
            tonal = scheme.secondaryContainer.copy(alpha = 0.66f),
            onTonal = scheme.onSecondaryContainer,
            clear = White.copy(alpha = 0.05f),
            rimTop = White.copy(alpha = 0.24f),
            rimBottom = White.copy(alpha = 0.05f),
            sheen = White.copy(alpha = 0.07f),
        )
    }
}
