package id.haho.muslimdaily.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.muslimdaily.core.designsystem.component.wordBoundaryBefore
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Audit M10: a card preview cut at its line limit ends at a whole word, never inside one. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class ArabicWordClampUiTest {
    @get:Rule val compose = createComposeRule()

    // Placeholder words only: this checks layout, not content.
    private val word = "كلمةتجريبية"
    private val text = List(60) { word }.joinToString(" ")

    @Test
    fun the_boundary_steps_back_to_the_end_of_the_last_whole_word() {
        assertEquals(3, wordBoundaryBefore("abc def ghi", 5)) // inside "def"
        assertEquals(3, wordBoundaryBefore("abc def ghi", 4)) // right after the space
        assertEquals(7, wordBoundaryBefore("abc def ghi", 7)) // exactly at the end of "def"
        assertNull(wordBoundaryBefore("abcdef ghi", 3)) // the first word does not fit
    }

    @Test
    fun a_clamped_preview_ends_in_an_ellipsis_after_whole_words() {
        compose.setContent {
            AdwamTheme { Box(Modifier.width(220.dp)) { ArabicText(text, maxLines = 2, clampToWords = true) } }
        }
        compose.waitForIdle()
        val shown = compose.onRoot().fetchSemanticsNode().let { root ->
            fun find(n: androidx.compose.ui.semantics.SemanticsNode): String? =
                n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: n.children.firstNotNullOfOrNull(::find)
            find(root)
        }!!
        assertTrue(shown.endsWith("…"), "ends in an ellipsis: $shown")
        val body = shown.removeSuffix("…")
        assertTrue(body.length < text.length, "was cut")
        assertTrue(body.split(" ").all { it == word }, "only whole words remain: $body")
    }
}
