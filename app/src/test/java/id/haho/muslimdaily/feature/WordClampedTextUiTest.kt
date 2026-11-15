package id.haho.muslimdaily.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.component.WordClampedText
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Audit M10 (option C): a plain-text preview cut at its line limit ends at a whole word, never inside one. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class WordClampedTextUiTest {
    @get:Rule val compose = createComposeRule()

    // Placeholder words only (letter runs of different lengths): this checks layout, not content.
    private val words = (2..11).map { "a".repeat(it) }
    private val text = List(90) { words[(it * 3) % words.size] }.joinToString(" ")

    private val scale = mutableFloatStateOf(1f)
    private val width = mutableIntStateOf(211)

    private fun shownText(): String {
        compose.waitForIdle()
        fun find(n: SemanticsNode): String? =
            n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: n.children.firstNotNullOfOrNull(::find)
        return find(compose.onRoot().fetchSemanticsNode())!!
    }

    @Test
    fun a_cut_preview_ends_in_an_ellipsis_after_whole_words() {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale.floatValue)) {
                AdwamTheme {
                    Box(Modifier.width(width.intValue.dp)) {
                        WordClampedText(AnnotatedString(text), MaterialTheme.typography.bodyLarge, maxLines = 3)
                    }
                }
            }
        }
        // Several widths at 100% and 200% text, so the cut lands in different places.
        for (s in listOf(1f, 2f)) for (w in listOf(211, 233, 257, 280, 331)) {
            scale.floatValue = s
            width.intValue = w
            val shown = shownText()
            assertTrue(shown.endsWith("…"), "ends in an ellipsis ($s, $w): $shown")
            val body = shown.removeSuffix("…")
            assertTrue(body.length < text.length, "was cut ($s, $w)")
            assertTrue(body.split(" ").all { it in words }, "only whole words remain ($s, $w): $body")
            assertTrue(text.startsWith(body), "still a prefix of the text ($s, $w)")
        }
    }

    @Test
    fun a_text_that_fits_is_left_alone() {
        compose.setContent {
            AdwamTheme {
                Box(Modifier.width(300.dp)) {
                    WordClampedText(AnnotatedString("aa bbb cccc"), MaterialTheme.typography.bodyLarge, maxLines = 3)
                }
            }
        }
        assertEquals("aa bbb cccc", shownText())
    }
}
