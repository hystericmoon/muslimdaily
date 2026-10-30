package id.haho.muslimdaily.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.routines.RoutineEditActions
import id.haho.muslimdaily.feature.routines.RoutineEditScreen
import id.haho.muslimdaily.feature.routines.RoutineEditState
import id.haho.muslimdaily.feature.routines.RoutineEdits
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** The routine editor's rows offer move actions as the alternative to dragging (WCAG 2.5.7). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h2000dp")
class RoutineEditUiTest {
    @get:Rule val compose = createComposeRule()

    private var items = listOf(DuaPreviewData.dua("a", 1), DuaPreviewData.dua("b", 2), DuaPreviewData.dua("c", 3))

    private fun setUp() {
        compose.setContent {
            var state by remember { mutableStateOf(RoutineEditState(isNew = false, name = "Doa saya", items = items)) }
            AdwamTheme {
                RoutineEditScreen(
                    state,
                    onBack = {},
                    RoutineEditActions(
                        onMove = { id, delta ->
                            state = state.copy(items = RoutineEdits.move(state.items, id, delta))
                            items = state.items
                        },
                    ),
                )
            }
        }
    }

    private fun rows() = compose
        .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        .fetchSemanticsNodes()

    @Test
    fun rows_offer_only_the_moves_their_position_allows() {
        setUp()
        val labels = rows().map { row -> row.config[SemanticsActions.CustomActions].map { it.label } }
        assertEquals(
            listOf(listOf("Pindah ke bawah"), listOf("Pindah ke atas", "Pindah ke bawah"), listOf("Pindah ke atas")),
            labels,
        )
    }

    @Test
    fun move_actions_reorder_the_routine() {
        setUp()
        compose.runOnIdle { rows().first().config[SemanticsActions.CustomActions].single().action() }
        compose.waitForIdle()
        assertEquals(listOf("b", "a", "c"), items.map { it.id })
        compose.runOnIdle { rows().last().config[SemanticsActions.CustomActions].single().action() }
        compose.waitForIdle()
        assertEquals(listOf("b", "c", "a"), items.map { it.id })
    }
}
