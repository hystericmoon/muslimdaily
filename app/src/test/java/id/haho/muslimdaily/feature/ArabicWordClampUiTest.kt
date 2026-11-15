package id.haho.muslimdaily.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
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

    // Placeholder words only (letter runs of different lengths): this checks layout, not content.
    private val words = (2..9).map { "ب".repeat(it) }
    private val text = List(80) { words[it % words.size] }.joinToString(" ")

    @Test
    fun the_boundary_steps_back_to_the_end_of_the_last_whole_word() {
        assertEquals(3, wordBoundaryBefore("abc def ghi", 5)) // inside "def"
        assertEquals(3, wordBoundaryBefore("abc def ghi", 4)) // right after the space
        assertEquals(7, wordBoundaryBefore("abc def ghi", 7)) // exactly at the end of "def"
        assertNull(wordBoundaryBefore("abcdef ghi", 3)) // the first word does not fit
    }

    private val scale = mutableFloatStateOf(1f)
    private val width = mutableIntStateOf(211)

    private fun shownText(): String {
        compose.waitForIdle()
        return compose.onRoot().fetchSemanticsNode().let { root ->
            fun find(n: androidx.compose.ui.semantics.SemanticsNode): String? =
                n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: n.children.firstNotNullOfOrNull(::find)
            find(root)
        }!!
    }

    @Test
    fun a_clamped_preview_ends_in_an_ellipsis_after_whole_words() {
        // Several widths and both 100% and 200% text, so the cut lands in different places.
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale.floatValue)) {
                AdwamTheme { Box(Modifier.width(width.intValue.dp)) { ArabicText(text, maxLines = 3, clampToWords = true) } }
            }
        }
        for (s in listOf(1f, 2f)) for (w in listOf(211, 233, 257, 280)) {
            scale.floatValue = s
            width.intValue = w
            val shown = shownText()
            assertTrue(shown.endsWith("…"), "ends in an ellipsis ($s, $w): $shown")
            val body = shown.removeSuffix("…")
            assertTrue(body.length < text.length, "was cut")
            assertTrue(body.split(" ").all { it in words }, "only whole words remain ($s, $w): $body")
            assertTrue(text.startsWith(body), "still a prefix of the text")
        }
    }
}
