package id.haho.muslimdaily.domain.memorize

import id.haho.muslimdaily.audio.SequenceItem
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PauseMode
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeitnerTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun `first rating places the dua in box 1 or 2`() {
        assertEquals(LeitnerState(2, today.plusDays(3)), Leitner.rate(null, Rating.EASY, today))
        assertEquals(LeitnerState(1, today.plusDays(1)), Leitner.rate(null, Rating.HARD, today))
        assertEquals(LeitnerState(1, today.plusDays(1)), Leitner.rate(null, Rating.AGAIN, today))
    }

    @Test
    fun `easy climbs one box at a time through 1 3 7 14 30 days`() {
        var state: LeitnerState? = null
        val dues = mutableListOf<Long>()
        repeat(5) {
            state = Leitner.rate(state, Rating.EASY, today)
            dues += java.time.temporal.ChronoUnit.DAYS.between(today, state!!.due)
        }
        assertEquals(listOf(3L, 7L, 14L, 30L, 30L), dues)
        assertTrue(Leitner.isMemorized(state!!))
    }

    @Test
    fun `hard stays in the same box`() {
        val s = Leitner.rate(LeitnerState(3, today), Rating.HARD, today)
        assertEquals(3, s.box)
        assertEquals(today.plusDays(7), s.due)
    }

    @Test
    fun `again resets to box 1`() {
        assertEquals(LeitnerState(1, today.plusDays(1)), Leitner.rate(LeitnerState(5, today), Rating.AGAIN, today))
    }

    @Test
    fun `due on or before today`() {
        assertTrue(Leitner.isDue(LeitnerState(2, today), today))
        assertTrue(Leitner.isDue(LeitnerState(2, today.minusDays(4)), today))
        assertTrue(!Leitner.isDue(LeitnerState(2, today.plusDays(1)), today))
    }
}

class MemorizePlanTest {
    private val dua = DuaPreviewData.dua("t", 1) // 3 phrases: 0–1.6 s, 1.6–3.4 s, 3.4–5.0 s
    private val settings = MemorizeSettings(loopsPerChunk = 3, pauseMode = PauseMode.AUTO, speed = 1f)

    @Test
    fun `listen plays the full dua twice`() {
        val clips = MemorizePlan.listen(dua, "").filterIsInstance<SequenceItem.Clip>()
        assertEquals(2, clips.size)
        assertEquals(0..2, clips.first().phrases)
    }

    @Test
    fun `build grows cumulative chunks with loops and pauses`() {
        val seq = MemorizePlan.build(dua, settings, "")
        val clips = seq.filterIsInstance<SequenceItem.Clip>()
        assertEquals(9, clips.size) // 3 chunks x 3 loops
        assertEquals(listOf(0..0, 0..1, 0..2), clips.map { it.phrases }.distinct())
        assertEquals(listOf(0L, 1600L), listOf(clips[0].startMs, clips[0].endMs))
        assertEquals(3400L, clips[3].endMs)
        // Every play is followed by a repeat-aloud pause.
        assertTrue(seq.zipWithNext().all { (a, b) -> a !is SequenceItem.Clip || b is SequenceItem.Pause })
    }

    @Test
    fun `auto pause is 1_2x the clip at the playback speed`() {
        assertEquals(1920L, MemorizePlan.pauseMs(1600, settings))
        assertEquals(2258L, MemorizePlan.pauseMs(1600, settings.copy(speed = 0.85f)))
        assertEquals(4000L, MemorizePlan.pauseMs(1600, settings.copy(pauseMode = PauseMode.MANUAL, manualPauseSeconds = 4)))
    }

    @Test
    fun `loops are clamped to 1 to 10`() {
        val clips = MemorizePlan.build(dua, settings.copy(loopsPerChunk = 50), "").filterIsInstance<SequenceItem.Clip>()
        assertEquals(30, clips.size)
    }

    @Test
    fun `ab loop covers the chosen phrases`() {
        val clips = MemorizePlan.abLoop(dua, 2, 1, settings, "").filterIsInstance<SequenceItem.Clip>()
        assertEquals(3, clips.size)
        assertEquals(1600L to 5000L, clips.first().startMs to clips.first().endMs)
    }

    @Test
    fun `recall with gaps mutes hidden phrases`() {
        val seq = MemorizePlan.recallWithGaps(dua, setOf(1), settings, "")
        assertTrue(seq[0] is SequenceItem.Clip)
        assertTrue(seq[1] is SequenceItem.Pause)
        assertTrue(seq[2] is SequenceItem.Clip)
    }
}

class RecallTest {
    private val dua = DuaPreviewData.dua("t", 1)
    private val words = Recall.words(dua)

    @Test
    fun `levels hide nested growing sets of words`() {
        val light = Recall.hiddenWords(words, FadeLevel.LIGHT, dua.id)
        val medium = Recall.hiddenWords(words, FadeLevel.MEDIUM, dua.id)
        val full = Recall.hiddenWords(words, FadeLevel.FULL, dua.id)
        assertTrue(Recall.hiddenWords(words, FadeLevel.NONE, dua.id).isEmpty())
        assertTrue(medium.containsAll(light) && full.containsAll(medium))
        assertEquals(words.size, full.size)
        assertTrue(light.size < medium.size)
    }

    @Test
    fun `choice is stable per dua`() {
        assertEquals(Recall.hiddenWords(words, FadeLevel.LIGHT, "x"), Recall.hiddenWords(words, FadeLevel.LIGHT, "x"))
    }

    @Test
    fun `all phrases muted at full level`() {
        val hidden = Recall.hiddenWords(words, FadeLevel.FULL, dua.id)
        assertEquals(setOf(0, 1, 2), Recall.hiddenPhrases(words, hidden))
    }
}
