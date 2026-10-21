package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.ReminderSettings
import id.haho.muslimdaily.feature.settings.AudioSettingsContent
import id.haho.muslimdaily.feature.settings.PermissionState
import id.haho.muslimdaily.feature.settings.RemindersContent
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1800dp-xhdpi")
class RemindersScreenshotTest {
    @Test
    fun reminders() = captureVariants("43-settings-reminders") {
        RemindersContent(
            settings = TodayPreviewData.settings.copy(
                reminders = ReminderSettings(
                    adhan = mapOf(Prayer.FAJR to AdhanMode.SOUND, Prayer.MAGHRIB to AdhanMode.SILENT),
                    morningAdhkar = true,
                    beforeSleep = true,
                ),
            ),
            permissions = PermissionState(notifications = true, exactAlarms = false),
            onAskNotifications = {},
            onAskExact = {},
            update = {},
        )
    }

    @Test
    fun audio() = captureVariants("44-settings-audio") { AudioSettingsContent(TodayPreviewData.settings) {} }
}
