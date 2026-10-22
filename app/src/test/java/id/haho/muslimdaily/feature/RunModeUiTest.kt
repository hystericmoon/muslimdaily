package id.haho.muslimdaily.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.MuslimDailyTheme
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
@Config(qualifiers = "w400dp-h860dp")
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
            MuslimDailyTheme {
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

    private fun card() = compose.onAllNodesWithText("Ketuk kartu untuk menghitung").onFirst()

    @Test
    fun tapping_the_card_counts_and_announces_progress() {
        setUp()
        compose.onNodeWithText("0 dari 3").assertIsDisplayed()
        card().performClick()
        compose.onNodeWithText("1 dari 3")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        card().performClick()
        compose.onNodeWithText("2 dari 3").assertIsDisplayed()
        assertEquals(listOf(RunEffect.Tick, RunEffect.Tick), effects)
    }

    @Test
    fun reaching_the_count_completes_the_card() {
        setUp()
        repeat(3) { card().performClick() }
        compose.onAllNodesWithText("Selesai").onFirst().assertIsDisplayed()
        assertEquals(RunEffect.ItemComplete, effects.last())
        compose.onNodeWithText("Doa 1 dari 2").assertIsDisplayed()
    }

    @Test
    fun undo_reduces_the_count() {
        setUp()
        card().performClick()
        card().performClick()
        compose.onNodeWithText("Kurangi").performClick()
        compose.onNodeWithText("1 dari 3").assertIsDisplayed()
    }

    @Test
    fun card_without_count_is_marked_read_with_one_tap() {
        setUp(initial().copy(counts = listOf(3, 0), index = 1))
        compose.onAllNodesWithText("Ketuk kartu setelah membaca").onFirst().performClick()
        assertEquals(RunEffect.SessionComplete, effects.last())
    }

    @Test
    fun progress_label_shows_position() {
        setUp()
        compose.onNodeWithText("Doa 1 dari 2").assertIsDisplayed()
    }
}
