package id.haho.muslimdaily.feature

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.component.AdwamCheckRow
import id.haho.design.component.NavigationRow
import id.haho.design.component.SegmentedRow
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import kotlin.test.assertEquals

/** What TalkBack is given for the problems found in the 0.8.0 TalkBack pass. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h2400dp")
class TalkBackSemanticsTest {
    @get:Rule val compose = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
    private val notSelectable = SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected)
    private val noText = SemanticsMatcher("has no text of its own") { (it.config.getOrElseNullable(SemanticsProperties.Text) { null }).isNullOrEmpty() }

    @Test
    fun `settings rows are buttons, not unselected toggles`() {
        compose.setContent { AdwamTheme { NavigationRow(title = "Pengingat", summary = "Adzan", onClick = {}) } }
        compose.onNodeWithText("Pengingat").assert(hasRole(Role.Button)).assert(notSelectable)
    }

    @Test
    fun `checklist rows name both states`() {
        compose.setContent {
            AdwamTheme {
                var checked by remember { mutableStateOf(false) }
                AdwamCheckRow(checked = checked, onCheckedChange = { checked = it }) { Text("Qabliyah") }
            }
        }
        val row = compose.onNodeWithText("Qabliyah")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Belum dikerjakan"))
        row.performClick()
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sudah dikerjakan"))
    }

    @Test
    fun `a segmented choice reads its label once`() {
        compose.setContent {
            AdwamTheme {
                SegmentedRow(title = "Subuh", options = listOf("Mati", "Suara"), selected = "Mati", onSelect = {}, label = { it })
            }
        }
        compose.onNode(hasContentDescription("Subuh: Suara")).assert(noText)
    }

    @Test
    fun `the countdown does not announce itself`() {
        compose.setContent { AdwamTheme { TodayScreen(TodayPreviewData.ready(), TodayActions()) } }
        val live = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
        assertEquals(0, live.fetchSemanticsNodes().size)
    }

    @Test
    fun `the location chip says the place once`() {
        compose.setContent { AdwamTheme { TodayScreen(TodayPreviewData.ready(), TodayActions()) } }
        compose.onNode(hasContentDescription("Lokasi: Jakarta")).assert(noText)
    }

    @Test
    fun `Isya's end times are one stop`() {
        compose.setContent {
            AdwamTheme {
                TodayScreen(TodayPreviewData.ready(at = LocalDateTime.of(2026, 10, 21, 20, 30)), TodayActions(), initiallyExpanded = setOf("ISHA"))
            }
        }
        compose.onAllNodesWithText("Hingga sepertiga malam", substring = true).onFirst()
            .assert(hasText("Hingga tengah malam", substring = true))
            .assert(hasText("Hingga Subuh", substring = true))
    }
}
