package id.haho.muslimdaily.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import id.haho.muslimdaily.core.designsystem.component.bookNoteRanges
import id.haho.muslimdaily.core.designsystem.component.withPauseCommas
import org.junit.Test
import kotlin.test.assertEquals

/** Neutral strings only; no religious text is generated here. */
class BookNotesTest {
    @Test
    fun single_brackets_are_notes() {
        val text = "abc (x y) def [note] g"
        assertEquals(listOf("(x y)", "[note]"), bookNoteRanges(text).map { text.substring(it) })
    }

    @Test
    fun double_parentheses_quote_the_dua_and_are_not_notes() {
        val text = "say: ((one two (three))) end"
        assertEquals(listOf("(three)"), bookNoteRanges(text).map { text.substring(it) })
    }

    @Test
    fun nested_groups_count_once_and_unclosed_ones_are_ignored() {
        assertEquals(listOf("[a (b) c]"), "[a (b) c] (open".let { t -> bookNoteRanges(t).map { t.substring(it) } })
    }

    @Test
    fun pause_commas_are_coloured_outside_notes_and_muted_phrases() {
        val accent = Color.Red
        val muted = Color.Gray
        val text = buildAnnotatedString {
            append("a\u060C b (c\u060C d) e\u060C ")
            val start = length
            append("f\u060C g")
            addStyle(SpanStyle(color = muted), start, length)
        }
        val coloured = withPauseCommas(text, accent, muted).spanStyles.filter { it.item.color == accent }.map { it.start }
        assertEquals(listOf(1, 13), coloured)
    }

    @Test
    fun text_without_commas_is_returned_as_is() {
        val text = AnnotatedString("abc")
        assertEquals(text, withPauseCommas(text, Color.Red, Color.Gray))
    }
}
