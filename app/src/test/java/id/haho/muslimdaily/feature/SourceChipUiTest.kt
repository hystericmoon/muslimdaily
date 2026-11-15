package id.haho.muslimdaily.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.SourceChip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Audit D6: no source and no grading means no chip, not an empty pill. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class SourceChipUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun blank_label_draws_no_chip() {
        val dua = DuaPreviewData.dua("x", 1).copy(sources = emptyList(), grading = null)
        compose.setContent { AdwamTheme { SourceChip(dua) } }
        compose.onNode(hasContentDescription("Sumber dan derajat", substring = true)).assertDoesNotExist()
    }

    @Test
    fun a_source_draws_the_chip() {
        val dua = DuaPreviewData.dua("x", 1).copy(sources = listOf(SourceRef("Kitab contoh", "1")), grading = null)
        compose.setContent { AdwamTheme { SourceChip(dua) } }
        compose.onNodeWithText("Kitab contoh 1").assertIsDisplayed()
    }
}
