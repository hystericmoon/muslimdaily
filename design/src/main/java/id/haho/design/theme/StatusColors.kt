package id.haho.design.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Success and warning text/icon colours, which Material has no role for. Danger is
 * `colorScheme.error`. They pass 4.5:1 on every surface, glass card and sheet for every palette,
 * light and dark, glass on and off, grey surfaces and the sample seeds (`StatusContrastTest`).
 * Always pair them with words or an icon; never signal state by colour alone.
 */
@Immutable
data class StatusColors(
    val success: Color,
    val warning: Color,
)

/** The status colours for a light or dark theme. */
fun statusColors(dark: Boolean): StatusColors =
    if (dark) StatusColors(success = Color(0xFF6FD9A2), warning = Color(0xFFF5B83D))
    else StatusColors(success = Color(0xFF13683F), warning = Color(0xFF7F5200))

val LocalStatusColors = staticCompositionLocalOf { statusColors(dark = false) }

/**
 * A palette's accent (`primary`) for a picker swatch. With [dynamicColor] on Android 12+ it is the
 * wallpaper's primary instead, so a "Wallpaper" choice previews what it will look like.
 */
@Composable
fun paletteSwatch(palette: HahoPalette, dark: Boolean, dynamicColor: Boolean = false): Color {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    }
    return colorSchemeFor(palette, dark).primary
}
