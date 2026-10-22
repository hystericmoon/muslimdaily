package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.feature.settings.PrayerSettingsContent
import id.haho.muslimdaily.feature.settings.SettingsDestinations
import id.haho.muslimdaily.feature.settings.SettingsHomeContent
import id.haho.muslimdaily.feature.settings.location.LocationContent
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import id.haho.muslimdaily.feature.today.TodayUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h2400dp-xhdpi")
class TodayScreenshotTest {

    @Test
    fun todayMorning() = captureVariants("10-today-morning") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR", "DHUHR"))
    }

    @Test
    fun todayFridayZawal() = captureVariants("11-today-friday-zawal") {
        TodayScreen(
            TodayPreviewData.ready(at = LocalDateTime.of(2026, 10, 23, 11, 35), checked = emptySet(), progress = emptyMap()),
            TodayActions(),
            initiallyExpanded = setOf("DHUHR"),
        )
    }

    @Test
    fun todayTravel() = captureVariants("12-today-travel") {
        TodayScreen(
            TodayPreviewData.ready(
                at = LocalDateTime.of(2026, 10, 21, 16, 0),
                settings = TodayPreviewData.settings.copy(travelMode = true),
                checked = emptySet(),
                progress = emptyMap(),
            ),
            TodayActions(),
            initiallyExpanded = setOf("DHUHR", "ASR"),
        )
    }

    /** 14 Ramadan 1448 (Umm al-Qura): counting down to iftar, then to the end of suhur. */
    @Test
    fun ramadanAfternoon() = captureVariants("14-today-ramadan-iftar") {
        TodayScreen(TodayPreviewData.ready(at = LocalDateTime.of(2027, 2, 21, 15, 0), checked = emptySet(), progress = emptyMap()), TodayActions())
    }

    @Test
    fun ramadanSuhur() = captureVariants("15-today-ramadan-suhur") {
        TodayScreen(TodayPreviewData.ready(at = LocalDateTime.of(2027, 2, 21, 3, 30), checked = emptySet(), progress = emptyMap()), TodayActions())
    }

    @Test
    fun noLocation() = captureVariants("13-today-no-location") { TodayScreen(TodayUiState.NoLocation, TodayActions()) }

    @Test
    fun settingsHome() = captureVariants("40-settings-home") { SettingsHomeContent(TodayPreviewData.settings, SettingsDestinations()) }

    @Test
    fun prayerSettings() = captureVariants("41-settings-prayer") { PrayerSettingsContent(TodayPreviewData.settings) {} }

    @Test
    fun location() = captureVariants("42-settings-location") {
        LocationContent(TodayPreviewData.settings.location, "", emptyList(), false, {}, {}, {})
    }
}
