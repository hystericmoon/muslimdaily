package id.haho.muslimdaily.core.ui

import androidx.compose.runtime.Composable
import id.haho.design.glass.GlassBackdrop
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme

/** Wraps a preview in [AdwamTheme] over the glass backdrop; annotate it with `@HahoPreviews`. */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    AdwamTheme {
        GlassBackdrop { content() }
    }
}
