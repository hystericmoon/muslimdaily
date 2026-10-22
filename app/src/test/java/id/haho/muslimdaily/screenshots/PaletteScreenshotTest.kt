package id.haho.muslimdaily.screenshots

import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Today in every colour theme, light and dark, at phone size. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class PaletteScreenshotTest {

    @Test
    fun todayPalettes() = capturePalettes("70-palette-today") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR"))
    }
}
