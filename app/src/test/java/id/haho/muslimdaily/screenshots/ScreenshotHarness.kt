package id.haho.muslimdaily.screenshots

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import id.haho.design.glass.GlassBackdrop
import id.haho.design.theme.HahoPalette
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.design.theme.ThemeMode

/** The three variants every screen is checked in, matching @HahoPreviews. */
enum class Variant(val suffix: String, val mode: ThemeMode, val fontScale: Float) {
    LIGHT("light", ThemeMode.LIGHT, 1f),
    DARK("dark", ThemeMode.DARK, 1f),
    FONT_200("font200", ThemeMode.LIGHT, 2f),
}

fun captureVariants(name: String, content: @Composable () -> Unit) {
    // Indonesian phones default to 24-hour time.
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
    Variant.entries.forEach { variant ->
        captureRoboImage("$name-${variant.suffix}.png") {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, variant.fontScale)) {
                AdwamTheme(themeMode = variant.mode) {
                    GlassBackdrop { content() }
                }
            }
        }
    }
}

/** A phone shot for the landing page (site/shots), in the site's Forest dark theme. */
fun captureSite(name: String, content: @Composable () -> Unit) {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
    captureRoboImage("$name.png") {
        AdwamTheme(themeMode = ThemeMode.DARK, palette = HahoPalette.FOREST) {
            GlassBackdrop { content() }
        }
    }
}

/** Captures [content] once per colour theme in light and dark, at a phone-sized viewport. */
fun capturePalettes(name: String, content: @Composable () -> Unit) {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
    HahoPalette.entries.forEach { palette ->
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
            captureRoboImage("$name-${palette.name.lowercase()}-${mode.name.lowercase()}.png") {
                AdwamTheme(themeMode = mode, palette = palette) {
                    GlassBackdrop { content() }
                }
            }
        }
    }
}
