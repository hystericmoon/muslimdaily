package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.ReminderSettings
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.memorize.HifzEntry
import id.haho.muslimdaily.feature.memorize.MemorizeHomeContent
import id.haho.muslimdaily.feature.memorize.MemorizeHomeState
import id.haho.muslimdaily.feature.memorize.session.MemorizeActions
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionScreen
import id.haho.muslimdaily.feature.memorize.session.MemorizeUiState
import id.haho.muslimdaily.feature.memorize.session.Playing
import id.haho.muslimdaily.feature.memorize.session.Step
import id.haho.muslimdaily.feature.onboarding.OnboardingActions
import id.haho.muslimdaily.feature.onboarding.OnboardingScreen
import id.haho.muslimdaily.feature.settings.PermissionState
import id.haho.muslimdaily.feature.settings.PrayerSettingsContent
import id.haho.muslimdaily.feature.settings.RemindersContent
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * The same screens as the Indonesian set (10, 41, 50-53, 100, 102) in the English UI, for the
 * English promo (LinkedIn). `marketing/tools/prep_screens.py` reads the `-en` files.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-w400dp-h1800dp-xhdpi")
class PromoEnglishScreenshotTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val dua = DuaPreviewData.dua("morning_02", 2)

    private fun state(
        step: Step,
        fade: FadeLevel = FadeLevel.LIGHT,
        playing: Playing = Playing.NONE,
        playback: PlaybackState = PlaybackState(),
    ): MemorizeUiState {
        val words = Recall.words(dua)
        return MemorizeUiState(
            dua = dua, eventLabel = "Morning adhkar", step = step, playing = playing, playback = playback,
            settings = MemorizeSettings(), fade = fade, words = words,
            hiddenWords = Recall.hiddenWords(words, fade, dua.id, FadeOrder.RANDOM), review = null, rated = null,
            fromReview = false, nextDueId = null, ab = 0..1, fadeOrder = FadeOrder.RANDOM, firstLetters = false,
            position = 2, positionOf = 19,
        )
    }

    @Test
    fun home() = captureVariants("150-memorize-home-en") {
        MemorizeHomeContent(
            MemorizeHomeState(
                today,
                listOf(
                    HifzEntry(ReviewItem("a", LeitnerState(2, today), today.minusDays(3)), DuaPreviewData.dua("a", 1), "Waking up", due = true),
                    HifzEntry(ReviewItem("b", LeitnerState(5, today.plusDays(21)), today.minusDays(9)), DuaPreviewData.dua("b", 2), "Morning adhkar", due = false),
                    HifzEntry(ReviewItem("c", LeitnerState(3, today.plusDays(4)), today.minusDays(3)), DuaPreviewData.dua("c", 3), "Before sleep", due = false),
                    HifzEntry(ReviewItem("d", LeitnerState(1, today), null), DuaPreviewData.dua("d", 1), "Entering the mosque", due = false),
                ),
            ),
        ) { _, _ -> }
    }

    @Test
    fun build() = captureVariants("151-memorize-build-en") {
        MemorizeSessionScreen(
            state(Step.BUILD, playing = Playing.BUILD, playback = PlaybackState(duaId = dua.id, isPlaying = true, sequenceIndex = 9, inPause = true)),
            MemorizeActions(),
            talkBack = false,
        )
    }

    @Test
    fun recall() = captureVariants("152-memorize-recall-en") {
        MemorizeSessionScreen(state(Step.RECALL, fade = FadeLevel.MEDIUM), MemorizeActions(), talkBack = false)
    }

    @Test
    fun check() = captureVariants("153-memorize-check-en") {
        MemorizeSessionScreen(state(Step.CHECK), MemorizeActions(), talkBack = false)
    }

    @Test
    @Config(qualifiers = "en-w400dp-h2400dp-xhdpi")
    fun todayMorning() = captureVariants("154-today-morning-en") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR", "DHUHR"))
    }

    @Test
    @Config(qualifiers = "en-w400dp-h2400dp-xhdpi")
    fun onboarding() = captureVariants("155-onboarding-en") {
        OnboardingScreen(AppSettings(), notificationsAllowed = false, exactAllowed = false, locationMessage = null, actions = OnboardingActions())
    }

    @Test
    @Config(qualifiers = "en-w400dp-h2400dp-xhdpi")
    fun prayerSettings() = captureVariants("156-settings-prayer-en") {
        PrayerSettingsContent(TodayPreviewData.settings) {}
    }

    @Test
    @Config(qualifiers = "en-w1280dp-h800dp-xhdpi")
    fun todayTablet() = captureVariants("157-today-tablet-en") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR", "DHUHR"))
    }

    @Test
    @Config(qualifiers = "en-w400dp-h1800dp-xhdpi")
    fun reminders() = captureVariants("158-settings-reminders-en") {
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
    @Config(qualifiers = "en-w400dp-h2400dp-xhdpi")
    fun widgetAlarmNote() = captureVariants("159-today-widget-alarm-note-en") {
        TodayScreen(TodayPreviewData.ready(), TodayActions(), widgetAlarmNote = true)
    }
}
