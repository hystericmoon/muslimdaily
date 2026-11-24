package id.haho.design.component

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.haho.design.glass.GlassBackdrop
import id.haho.design.theme.HahoPalette
import id.haho.design.theme.HahoTheme
import id.haho.design.theme.ThemeMode

/** Preview a screen in light, dark and at 200% font scale. */
@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 860)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 860,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Font 200%", showBackground = true, widthDp = 400, heightDp = 860, fontScale = 2f)
annotation class HahoPreviews

/** Wraps preview content in [HahoTheme] over the glass backdrop. */
@Composable
fun HahoPreviewSurface(
    palette: HahoPalette = HahoPalette.Default,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    glass: Boolean = true,
    content: @Composable () -> Unit,
) {
    HahoTheme(palette = palette, themeMode = themeMode, glass = glass) {
        GlassBackdrop { content() }
    }
}
