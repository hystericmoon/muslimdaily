package id.haho.muslimdaily.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.run.RunActions
import id.haho.muslimdaily.feature.run.RunEffect
import id.haho.muslimdaily.feature.run.RunLogic
import id.haho.muslimdaily.feature.run.RunScreen
import id.haho.muslimdaily.feature.run.RunUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h860dp")
class RunModeUiTest {
    @get:Rule val compose = createComposeRule()

    private val duas = listOf(DuaPreviewData.dua("a", 1, count = 3), DuaPreviewData.dua("b", 2))
    private fun initial(autoAdvance: Boolean = false) = RunUiState(
        eventLabel = "Dzikir pagi",
        duas = duas,
        items = duas.map { RunItem(it.id, it.count) },
        counts = listOf(0, 0),
        index = 0,
        completed = false,
        prefs = ReadingPrefs(),
        autoAdvance = autoAdvance,
        haptics = true,
    )

    private val effects = mutableListOf<RunEffect>()

    private fun setUp(start: RunUiState = initial()) {
        compose.setContent {
            var state by remember { mutableStateOf(start) }
            AdwamTheme {
                RunScreen(
                    state,
                    RunActions(
                        onTap = {
                            val (next, effect) = RunLogic.tap(state)
                            state = next
                            effect?.let(effects::add)
                        },
                        onUndo = { state = RunLogic.undo(state) },
                        onPage = { state = RunLogic.goTo(state, it) },
                    ),
                )
            }
        }
    }

    /** The mark button in the bar: counts on a counted dua, marks read on one read once. */
    private fun card() = compose.onNodeWithContentDescription("Hitung")

    @Test
    fun tapping_the_card_counts_and_announces_progress() {
        setUp()
        compose.onNodeWithText("0/3").assertIsDisplayed()
        card().performClick()
        compose.onNodeWithText("1/3")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        card().performClick()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        assertEquals(listOf(RunEffect.Tick, RunEffect.Tick), effects)
    }

    @Test
    fun reaching_the_count_completes_the_card() {
        setUp()
        repeat(3) { card().performClick() }
        // Gold with a check at the target, and the state is spoken.
        compose.onNodeWithContentDescription("Hitung").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selesai"))
        assertEquals(RunEffect.ItemComplete, effects.last())
        compose.onNodeWithText("Doa 1 dari 2").assertIsDisplayed()
    }

    @Test
    fun undo_reduces_the_count() {
        setUp()
        card().performClick()
        card().performClick()
        compose.onNodeWithContentDescription("Batalkan hitungan").performClick()
        compose.onNodeWithText("1/3").assertIsDisplayed()
    }

    @Test
    fun card_without_count_is_marked_read_with_one_tap() {
        setUp(initial().copy(counts = listOf(3, 0), index = 1))
        compose.onNodeWithContentDescription("Tandai sudah dibaca").performClick()
        assertEquals(RunEffect.SessionComplete, effects.last())
    }

    @Test
    fun progress_label_shows_position() {
        setUp()
        compose.onNodeWithText("Doa 1 dari 2").assertIsDisplayed()
    }

    @Test
    fun a_dua_in_hifz_is_recited_from_memory_until_revealed() {
        var revealed = -1
        compose.setContent {
            var state by remember { mutableStateOf(initial().copy(hifz = setOf("a"), fromMemory = true)) }
            AdwamTheme {
                RunScreen(
                    state,
                    RunActions(
                        onTap = { state = RunLogic.tap(state).first },
                        onReveal = { page ->
                            revealed = page
                            state = state.copy(revealed = state.revealed + page)
                        },
                    ),
                )
            }
        }
        compose.onAllNodesWithText("Baca dari hafalan").onFirst().assertIsDisplayed()
        // Counting still works while the text is hidden.
        card().performClick()
        compose.onNodeWithText("1/3").assertIsDisplayed()
        compose.onNodeWithText("Tampilkan teks").performClick()
        assertEquals(0, revealed)
        compose.onAllNodesWithText("Baca dari hafalan").assertCountEquals(0)
        compose.onNodeWithText("1/3").assertIsDisplayed()
    }

    @Test
    fun play_in_the_dock_listens_without_counting() {
        var played = 0
        var taps = 0
        compose.setContent {
            AdwamTheme { RunScreen(initial(), RunActions(onTap = { taps++ }, onPlay = { played++ })) }
        }
        compose.onNodeWithContentDescription("Putar").performClick()
        assertEquals(1, played)
        assertEquals(0, taps)
    }

    @Test
    fun audio_belongs_to_the_page_playing_it_and_waits_for_hidden_text() {
        val s = initial().copy(playback = PlaybackState(duaId = "a", isPlaying = true, activePhrase = 2), audioSpeed = 0.75f)
        assertEquals(2, s.playbackFor(0).activePhrase)
        assertEquals(-1, s.playbackFor(1).activePhrase)
        assertEquals(0.75f, s.playbackFor(1).speed)
        val hidden = s.copy(hifz = setOf("b"), fromMemory = true)
        assertEquals(true, hidden.canListen(0))
        assertEquals(false, hidden.canListen(1))
        assertEquals(true, hidden.copy(revealed = setOf(1)).canListen(1))
    }
}
