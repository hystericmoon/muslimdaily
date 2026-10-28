package id.haho.muslimdaily.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.CustomEntry
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.domain.myset.FormChoice
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.customize.CustomizeActions
import id.haho.muslimdaily.feature.duas.customize.CustomizeContent
import id.haho.muslimdaily.feature.duas.customize.CustomizeRules
import id.haho.muslimdaily.feature.duas.customize.CustomizeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Drives the real customize UI against an in-memory customisation that applies the same
 * ActiveSet rules as the repository.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h2000dp")
class CustomizeUiTest {
    @get:Rule val compose = createComposeRule()

    private val canonical = ActiveSet.canonical(
        listOf(DuaPreviewData.dua("a", 1), DuaPreviewData.dua("b", 2), DuaPreviewData.dua("v1", 3, group = "g"), DuaPreviewData.dua("v2", 4, group = "g")),
    )
    private var custom = Customization()


    private fun state(c: Customization): CustomizeUiState {
        val items = ActiveSet.items(canonical, c.copy(useMySet = true))
        return CustomizeUiState(null, items, items.filter { it.enabled }, items.filterNot { it.enabled }, c.choices, c.entries.isNotEmpty())
    }

    private fun setUp() {
        compose.setContent {
            var c by remember { mutableStateOf(custom) }
            fun commit(entries: List<CustomEntry>) {
                c = c.copy(useMySet = true, entries = entries)
                custom = c
            }
            val s = state(c)
            AdwamTheme {
                CustomizeContent(
                    s,
                    CustomizeActions(
                        onToggle = { key, enabled -> commit(CustomizeRules.setEnabled(s.all, key, enabled)) },
                        onMove = { key, delta -> CustomizeRules.move(s.all, key, delta)?.let(::commit) },
                        onReorder = { keys -> commit(CustomizeRules.reorder(s.all, keys)) },
                        onChoose = { group, choice -> c = c.copy(choices = c.choices + (group to choice)); custom = c },
                    ),
                    initiallyShowHidden = true,
                )
            }
        }
    }

    @Test
    fun hiding_an_item_moves_it_to_the_hidden_section_and_restore_brings_it_back() {
        setUp()
        compose.onNodeWithText("Doa 2").performClick()
        compose.onNodeWithText("Tersembunyi (1)").assertIsDisplayed()
        assertEquals(listOf("a", "v1"), ActiveSet.active(canonical, custom).map { it.id })

        // Restored to its original slot, not appended.
        compose.onNodeWithText("Tampilkan").performClick()
        assertEquals(listOf("a", "b", "v1"), ActiveSet.active(canonical, custom).map { it.id })
    }

    @Test
    fun accessibility_action_moves_an_item_down() {
        setUp()
        val row = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions) and hasAnyDescendant(hasText("Doa 1")),
        ).fetchSemanticsNode()
        val labels = row.config[SemanticsActions.CustomActions].map { it.label }
        assertEquals(listOf("Pindah ke bawah"), labels) // first row cannot move up
        compose.runOnIdle { row.config[SemanticsActions.CustomActions].first().action() }
        compose.waitForIdle()
        assertEquals(listOf("b", "a", "v1"), ActiveSet.active(canonical, custom).map { it.id })
    }

    @Test
    fun reorder_keeps_hidden_items_in_their_slots() {
        val items = ActiveSet.items(canonical, Customization())
        val hiddenB = CustomizeRules.setEnabled(items, "b", false)
        val withHidden = ActiveSet.items(canonical, Customization(true, hiddenB))
        val reordered = CustomizeRules.reorder(withHidden, listOf("vg:g", "a"))
        assertEquals(listOf("vg:g", "b", "a"), reordered.map { it.key })
        assertEquals(listOf(true, false, true), reordered.map { it.enabled })
    }

    @Test
    fun variant_group_offers_forms_and_rotate() {
        setUp()
        compose.onNodeWithText("Bentuk 2").performClick()
        assertEquals(FormChoice.Fixed("v2"), custom.choices["g"])
        compose.onNodeWithText("Bergantian tiap sesi").performClick()
        assertEquals(FormChoice.Rotate(0), custom.choices["g"])
    }
}
