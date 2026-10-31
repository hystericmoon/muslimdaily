package id.haho.muslimdaily.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import id.haho.design.glass.GlassBackdrop
import id.haho.design.theme.ThemeMode
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.TranslationMode
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.duas.detail.DuaDetailActions
import id.haho.muslimdaily.feature.duas.detail.DuaDetailContent
import id.haho.muslimdaily.feature.duas.detail.DuaDetailState
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.onboarding.OnboardingActions
import id.haho.muslimdaily.feature.onboarding.OnboardingScreen
import id.haho.muslimdaily.feature.run.RunActions
import id.haho.muslimdaily.feature.run.RunScreen
import id.haho.muslimdaily.feature.run.RunUiState
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import id.haho.muslimdaily.feature.today.TodayUiState
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PhraseLayout
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.memorize.session.MemorizeActions
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionScreen
import id.haho.muslimdaily.feature.memorize.session.MemorizeUiState
import id.haho.muslimdaily.feature.memorize.session.Playing
import id.haho.muslimdaily.feature.memorize.session.Step
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Google's Accessibility Test Framework over the main screens, in light and dark: touch targets,
 * labels, contrast of text and controls, duplicate descriptions. Fails on an error, so a
 * regression shows up in CI between manual TalkBack passes (docs/talkback-checklist.md).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1600dp-xhdpi")
class AccessibilityChecksTest {
    @get:Rule val compose = createComposeRule()

    private val options = RoborazziATFAccessibilityCheckOptions(
        checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
        failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
    )

    private fun check(mode: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        compose.setContent { AdwamTheme(themeMode = mode) { GlassBackdrop { content() } } }
        compose.onRoot().checkRoboAccessibility(options)
    }

    @Test fun today() = check { TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR")) }

    @Test fun todayDark() = check(ThemeMode.DARK) { TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("FAJR")) }

    @Test fun todayWithoutLocation() = check { TodayScreen(TodayUiState.NoLocation, TodayActions()) }

    @Test fun onboarding() = check {
        OnboardingScreen(AppSettings(), notificationsAllowed = false, exactAllowed = false, locationMessage = null, actions = OnboardingActions(), showLanguage = false)
    }

    @Test fun library() = check { DuaLibraryContent(DuaPreviewData.categories) {} }

    @Test fun duaDetail() = check { DuaDetailContent(detailState(), DuaDetailActions()) }

    @Test fun duaDetailDark() = check(ThemeMode.DARK) { DuaDetailContent(detailState(), DuaDetailActions()) }

    @Test fun run() = check {
        val duas = listOf(DuaPreviewData.dua("a", 1, count = 3), DuaPreviewData.dua("b", 2))
        RunScreen(
            RunUiState(
                eventLabel = "Dzikir pagi",
                duas = duas,
                items = duas.map { RunItem(it.id, it.count) },
                counts = listOf(1, 0),
                index = 0,
                completed = false,
                prefs = ReadingPrefs(),
                autoAdvance = false,
                haptics = true,
            ),
            RunActions(),
        )
    }

    @Test fun duaDetailInline() = check { DuaDetailContent(detailState().copy(prefs = ReadingPrefs(layout = PhraseLayout.INLINE)), DuaDetailActions()) }

    @Test fun duaDetailFocus() = check { DuaDetailContent(detailState().copy(prefs = ReadingPrefs(layout = PhraseLayout.FOCUS)), DuaDetailActions()) }

    @Test fun memorizeCheck() = check { MemorizeSessionScreen(memorizeCheckState(), MemorizeActions(), talkBack = false) }

    @Test fun memorizeCheckDark() = check(ThemeMode.DARK) { MemorizeSessionScreen(memorizeCheckState(), MemorizeActions(), talkBack = false) }

    private fun memorizeCheckState(): MemorizeUiState {
        val dua = DuaPreviewData.dua("morning_02", 2)
        val words = Recall.words(dua)
        return MemorizeUiState(
            dua = dua, eventLabel = "Dzikir pagi", step = Step.CHECK, playing = Playing.NONE, playback = PlaybackState(),
            settings = MemorizeSettings(), fade = FadeLevel.LIGHT, words = words, hiddenWords = emptySet(), review = null,
            rated = null, fromReview = false, nextDueId = null, ab = 0..1, missedWords = setOf(1),
        )
    }

    private fun detailState() = DuaDetailState(
        dua = DuaPreviewData.dua("morning_02", 2, count = 3),
        event = DuaEvent("morning", "adhkar", "Dzikir pagi", 1),
        prefs = ReadingPrefs(TranslationMode.BOTH, transliteration = true),
        playback = PlaybackState(duaId = "morning_02", isPlaying = true, activePhrase = 1),
    )
}
