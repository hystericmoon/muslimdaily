package id.haho.muslimdaily.screenshots

import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import id.haho.muslimdaily.core.designsystem.theme.MuslimDailyTheme
import id.haho.muslimdaily.core.designsystem.theme.ThemeMode

/** The three variants every screen is checked in, matching @ThemePreviews. */
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
                MuslimDailyTheme(themeMode = variant.mode) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                        content()
                    }
                }
            }
        }
    }
}
