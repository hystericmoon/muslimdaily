package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.feature.settings.AboutContent
import id.haho.muslimdaily.feature.settings.DisplaySettingsContent
import id.haho.muslimdaily.feature.settings.MethodologyContent
import id.haho.muslimdaily.feature.settings.data.MySetsDataContent
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1400dp-xhdpi")
class SettingsScreenshotTest {
    @Test fun display() = captureVariants("45-settings-display") { DisplaySettingsContent(TodayPreviewData.settings) {} }
    @Test fun methodology() = captureVariants("46-settings-methodology") { MethodologyContent(3) }
    @Test fun about() = captureVariants("47-settings-about") { AboutContent() }
    @Test fun mySetsData() = captureVariants("48-settings-mysets-data") { MySetsDataContent() }
}
