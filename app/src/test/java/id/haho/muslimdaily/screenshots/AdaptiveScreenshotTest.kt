package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.domain.prayer.HijriCalendar
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.onboarding.OnboardingActions
import id.haho.muslimdaily.feature.onboarding.OnboardingScreen
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import id.haho.muslimdaily.core.diagnostics.ReminderLog
import id.haho.muslimdaily.feature.settings.diagnostics.Diagnostics
import id.haho.muslimdaily.feature.settings.diagnostics.DiagnosticsContent
import id.haho.muslimdaily.notifications.PlannedReminder
import id.haho.muslimdaily.notifications.ReminderType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 0.5.0 screens: first-launch setup, the isbat question, tablet layouts and the English UI. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h2400dp-xhdpi")
class AdaptiveScreenshotTest {

    @Test
    fun onboarding() = captureVariants("100-onboarding") {
        OnboardingScreen(AppSettings(), notificationsAllowed = false, exactAllowed = false, locationMessage = null, actions = OnboardingActions())
    }

    /** The evening of 29 Syakban (calculated): asks whether Ramadan starts tomorrow. */
    @Test
    fun hijriCheck() = captureVariants("101-today-isbat") {
        val eve = generateSequence(LocalDate.of(2027, 1, 20)) { it.plusDays(1) }
            .first { HijriCalendar.of(it, 0).let { h -> h.month == 8 && h.day == 29 } }
        TodayScreen(
            TodayPreviewData.ready(at = LocalDateTime.of(eve, LocalTime.of(19, 30)), checked = emptySet(), progress = emptyMap()),
            TodayActions(),
        )
    }

    @Test
    @Config(qualifiers = "id-w1280dp-h800dp-xhdpi")
    fun todayTablet() = captureVariants("102-today-tablet") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR", "DHUHR"))
    }

    @Test
    @Config(qualifiers = "en-w400dp-h2400dp-xhdpi")
    fun todayEnglish() = captureVariants("103-today-english") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR"))
    }

    @Test
    @Config(qualifiers = "en-w400dp-h1600dp-xhdpi")
    fun libraryEnglish() = captureVariants("104-library-english") {
        DuaLibraryContent(DuaPreviewData.categories) {}
    }

    @Test
    fun diagnostics() = captureVariants("105-diagnostics") {
        val due = java.time.Instant.parse("2026-10-22T22:13:00Z")
        DiagnosticsContent(
            Diagnostics(
                notifications = true,
                exact = false,
                batteryRestricted = true,
                mutedChannels = emptyList(),
                planned = listOf(
                    PlannedReminder(ReminderType.ADHAN_FAJR, due.plusSeconds(86_400)),
                    PlannedReminder(ReminderType.MORNING_ADHKAR, due.plusSeconds(86_400 + 900)),
                ),
                fired = listOf(
                    ReminderLog.Entry("ADHAN_FAJR", due, due.plusSeconds(420), exact = false),
                    ReminderLog.Entry("ADHAN_ISHA", due.minusSeconds(32_000), due.minusSeconds(31_990), exact = false),
                ),
                crashes = emptyList(),
                zone = java.time.ZoneId.of("Asia/Jakarta"),
                manufacturer = "xiaomi",
            ),
            time = id.haho.muslimdaily.core.ui.TimeFormatter(true),
        )
    }
}

