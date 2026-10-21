package id.haho.muslimdaily.core.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import id.haho.muslimdaily.core.designsystem.theme.MuslimDailyTheme

/** Every screen is previewed in light, dark and at 200% font scale. */
@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 860)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 400,
    heightDp = 860,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Font 200%", showBackground = true, widthDp = 400, heightDp = 860, fontScale = 2f)
annotation class ThemePreviews

@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    MuslimDailyTheme {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}
