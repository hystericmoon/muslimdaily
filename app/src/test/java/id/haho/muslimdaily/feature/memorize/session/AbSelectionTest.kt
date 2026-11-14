package id.haho.muslimdaily.feature.memorize.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbSelectionTest {
    private val start = AbSelection.whole(5)

    @Test
    fun startsAsTheWholeDua() {
        assertEquals(0..4, start.range)
        assertFalse(start.awaitingSecond)
    }

    @Test
    fun firstTapSetsFromAndWaitsForTheSecond() {
        val s = start.tap(1)
        assertEquals(1..1, s.range)
        assertTrue(s.awaitingSecond)
    }

    @Test
    fun secondTapSetsToAfterFrom() {
        val s = start.tap(1).tap(3)
        assertEquals(1..3, s.range)
        assertFalse(s.awaitingSecond)
    }

    @Test
    fun secondTapBeforeFromSwapsTheEnds() {
        assertEquals(1..3, start.tap(3).tap(1).range)
    }

    @Test
    fun secondTapOnTheSamePhraseIsASinglePhrase() {
        assertEquals(2..2, start.tap(2).tap(2).range)
    }

    @Test
    fun thirdTapRestarts() {
        val s = start.tap(1).tap(3).tap(4)
        assertEquals(4..4, s.range)
        assertTrue(s.awaitingSecond)
    }

    @Test
    fun wholeOfOnePhraseOrNone() {
        assertEquals(0..0, AbSelection.whole(1).range)
        assertEquals(0..0, AbSelection.whole(0).range)
    }
}
