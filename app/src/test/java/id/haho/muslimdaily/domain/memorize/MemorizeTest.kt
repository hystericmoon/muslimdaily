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
    fun `with an audio range only the dua's part plays`() {
        val ranged = dua.copy(audio = dua.audio!!.copy(segments = emptyList(), range = id.haho.muslimdaily.core.model.Segment(2.0, 6.5)))
        val clip = MemorizePlan.listen(ranged, "").filterIsInstance<SequenceItem.Clip>().first()
        assertEquals(2000L to 6500L, clip.startMs to clip.endMs)
        val built = MemorizePlan.build(ranged, settings, "").filterIsInstance<SequenceItem.Clip>().first()
        assertEquals(2000L to 6500L, built.startMs to built.endMs)
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

    @Test
    fun `from the end hides the last words first, nested by level`() {
        val light = Recall.hiddenWords(words, FadeLevel.LIGHT, dua.id, FadeOrder.FROM_END)
        val medium = Recall.hiddenWords(words, FadeLevel.MEDIUM, dua.id, FadeOrder.FROM_END)
        assertTrue(words.last().index in light)
        assertTrue(words.first().index !in medium)
        assertTrue(medium.containsAll(light))
        assertEquals((words.size - light.size until words.size).toSet(), light)
    }

    @Test
    fun `tokens without a letter are never hidden`() {
        val ws = listOf(Word(0, "بِكَ", 0), Word(1, "،", 0), Word(2, "وَ", 0))
        assertEquals(setOf(0, 2), Recall.hiddenWords(ws, FadeLevel.FULL, "x"))
    }

    @Test
    fun `next hidden is the first hidden word in reading order`() {
        assertEquals(words[0], Recall.nextHidden(words, words.indices.toSet()))
        assertEquals(words[2], Recall.nextHidden(words, setOf(words.lastIndex, 2)))
        assertEquals(null, Recall.nextHidden(words, emptySet()))
    }

    @Test
    fun `first letter cue is the opening letter with its marks`() {
        // Joining letter: tatweel keeps the joined shape.
        assertEquals("\u0628\u0650\u0640", Recall.firstLetterCue("\u0628\u0650\u0633\u0652\u0645\u0650"))
        // Right-joining letter (ra): no tatweel.
        assertEquals("\u0631\u064E", Recall.firstLetterCue("\u0631\u064E\u0628\u0651\u0650"))
        // Article: alif + lam + the next letter.
        assertEquals("\u0627\u0644\u0652\u062D\u064E\u0640", Recall.firstLetterCue("\u0627\u0644\u0652\u062D\u064E\u0645\u0652\u062F\u064F"))
        // A one-letter word is shown whole, without tatweel.
        assertEquals("\u0648\u064E", Recall.firstLetterCue("\u0648\u064E"))
        // No Arabic letter: unchanged.
        assertEquals("\u06DD", Recall.firstLetterCue("\u06DD"))
    }

    @Test
    fun `first letter cue is a prefix of the word apart from tatweel`() {
        words.forEach { w ->
            assertTrue(w.text.startsWith(Recall.firstLetterCue(w.text).removeSuffix("\u0640")))
        }
    }
}

class FadeLadderTest {
    @Test
    fun `a recital with no reveals fades a stage, and finishes at the last`() {
        assertEquals(FadeLadder.Outcome.UP, FadeLadder.outcome(1, revealed = 0, hidden = 5))
        assertEquals(2, FadeLadder.next(1, FadeLadder.Outcome.UP))
        assertEquals(FadeLadder.Outcome.DONE, FadeLadder.outcome(FadeLadder.LAST, revealed = 0, hidden = 9))
    }

    @Test
    fun `a few reveals keep the stage, many bring a hint back but never below 30 percent`() {
        assertEquals(FadeLadder.Outcome.SAME, FadeLadder.outcome(3, revealed = 3, hidden = 9))
        assertEquals(FadeLadder.Outcome.DOWN, FadeLadder.outcome(3, revealed = 4, hidden = 9))
        assertEquals(2, FadeLadder.next(3, FadeLadder.Outcome.DOWN))
        assertEquals(FadeLadder.Outcome.SAME, FadeLadder.outcome(1, revealed = 5, hidden = 5))
    }

    @Test
    fun `manual choices map onto stages`() {
        assertEquals(0, FadeLadder.stageOf(FadeLevel.NONE, firstLetters = true))
        assertEquals(2, FadeLadder.stageOf(FadeLevel.MEDIUM, firstLetters = true))
        assertEquals(3, FadeLadder.stageOf(FadeLevel.FULL, firstLetters = true))
        assertEquals(4, FadeLadder.stageOf(FadeLevel.FULL, firstLetters = false))
        FadeLadder.STAGES.forEachIndexed { i, s -> assertEquals(i, FadeLadder.stageOf(s.fade, s.firstLetters)) }
    }
}
