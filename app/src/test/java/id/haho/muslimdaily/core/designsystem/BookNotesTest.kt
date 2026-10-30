package id.haho.muslimdaily.core.designsystem

import id.haho.muslimdaily.core.designsystem.component.bookNoteRanges
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
}
