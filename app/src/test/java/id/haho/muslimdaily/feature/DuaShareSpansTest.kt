package id.haho.muslimdaily.feature

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.feature.duas.detail.DuaShareCard
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Audit D8: the share image applies the app's ornament and note spans. Placeholder text only. */
@RunWith(AndroidJUnit4::class)
class DuaShareSpansTest {
    @Test
    fun ornaments_get_their_own_face_and_nothing_else_does() {
        val text = "abc۝def﴾g"
        val out = DuaShareCard.withShareOrnaments(text, Typeface.SERIF)
        val spans = out.getSpans(0, out.length, DuaShareCard.FaceSpan::class.java)
        assertEquals(listOf(3, 7), spans.map { out.getSpanStart(it) }.sorted())
        spans.forEach { assertEquals(out.getSpanStart(it) + 1, out.getSpanEnd(it)) }
        assertEquals(text, out.toString())
    }

    @Test
    fun bracketed_notes_are_smaller_and_muted_but_double_brackets_are_left_alone() {
        val text = "one (note) two ((quoted)) three [other]"
        val out = DuaShareCard.withShareNotes(SpannableString(text), 0xFF123456.toInt())
        val sizes = out.getSpans(0, out.length, RelativeSizeSpan::class.java).map { out.getSpanStart(it) to out.getSpanEnd(it) }.sortedBy { it.first }
        assertEquals(listOf(text.indexOf("(note)") to text.indexOf("(note)") + 6, text.indexOf("[other]") to text.length), sizes)
        assertEquals(2, out.getSpans(0, out.length, ForegroundColorSpan::class.java).size)
        assertEquals(text, out.toString())
    }
}
