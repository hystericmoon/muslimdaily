package id.haho.muslimdaily.core.model

import org.junit.Test
import kotlin.test.assertEquals

class PlayablePartsTest {
    // Neutral test strings only; no religious text is generated in tests.
    private fun dua(segments: List<Segment>, breaks: List<Int> = listOf(4)) = Dua(
        "d", "adhkar", "morning", 1, null, "aaa bbb", false, "", "", "", null,
        emptyList(), "", null, emptyList(), null, DuaAudio("d.opus", segments, Segment(1.0, 9.0)), breaks,
    )

    @Test
    fun `touching phrases play as one part`() {
        val d = dua(listOf(Segment(1.0, 4.0), Segment(4.0, 9.0)))
        assertEquals(listOf((1000L to 9000L)), d.playableParts(1000, 9000))
    }

    @Test
    fun `a gap between two phrases is skipped`() {
        val d = dua(listOf(Segment(1.0, 4.0), Segment(6.5, 9.0)))
        assertEquals(listOf((1000L to 4000L), (6500L to 9000L)), d.playableParts(1000, 9000))
    }

    @Test
    fun `a clip that starts after the gap has one part`() {
        val d = dua(listOf(Segment(1.0, 4.0), Segment(6.5, 9.0)))
        assertEquals(listOf((6500L to 9000L)), d.playableParts(6500, 9000))
    }

    @Test
    fun `a clip that ends before the gap has one part`() {
        val d = dua(listOf(Segment(1.0, 4.0), Segment(6.5, 9.0)))
        assertEquals(listOf((1000L to 4000L)), d.playableParts(1000, 4000))
    }

    @Test
    fun `end of file is kept as given`() {
        val d = dua(listOf(Segment(1.0, 4.0), Segment(6.5, 9.0)))
        assertEquals(listOf((1000L to 4000L), (6500L to END_OF_FILE)), d.playableParts(1000, END_OF_FILE))
    }

    @Test
    fun `without segments for every phrase nothing is cut`() {
        val d = dua(listOf(Segment(1.0, 4.0)), breaks = emptyList())
        assertEquals(listOf((0L to END_OF_FILE)), d.playableParts(0, END_OF_FILE))
    }
}
