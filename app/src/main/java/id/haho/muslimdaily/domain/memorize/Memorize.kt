package id.haho.muslimdaily.domain.memorize

import id.haho.muslimdaily.audio.SequenceItem
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PauseMode
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.random.Random

enum class Rating { AGAIN, HARD, EASY }

data class LeitnerState(val box: Int, val due: LocalDate)

/**
 * Leitner boxes with review intervals of 1, 3, 7, 14 and 30 days.
 * Easy moves up one box, Hard stays, Again goes back to box 1. A dua enters the system on its first
 * rating, starting from box 1. There are no streaks.
 */
object Leitner {
    val INTERVALS = listOf(1, 3, 7, 14, 30)
    const val TOP_BOX = 5

    fun rate(current: LeitnerState?, rating: Rating, today: LocalDate): LeitnerState {
        val box = current?.box ?: 1
        val next = when (rating) {
            Rating.EASY -> (box + 1).coerceAtMost(TOP_BOX)
            Rating.HARD -> box
            Rating.AGAIN -> 1
        }
        return LeitnerState(next, today.plusDays(INTERVALS[next - 1].toLong()))
    }

    fun isDue(state: LeitnerState, today: LocalDate): Boolean = !state.due.isAfter(today)

    /**
     * A dua recited from memory in a routine counts as a review only when it is due: daily
     * recitation would otherwise climb a box a day. Needing the text is a lapse whenever it
     * happens. Null when the schedule stays as it is.
     */
    fun ratingFromRoutine(current: LeitnerState, knew: Boolean, today: LocalDate): Rating? = when {
        !knew -> Rating.AGAIN
        isDue(current, today) -> Rating.EASY
        else -> null
    }

    /** Reached the last box: shown as memorised in "My hifz". */
    fun isMemorized(state: LeitnerState): Boolean = state.box >= TOP_BOX
}

/**
 * Builds the audio sequences for memorize mode from the dua's phrase segments. Memorisation loops
 * are counted here only; they are never mixed with worship counts.
 */
object MemorizePlan {
    private const val LISTEN_TIMES = 2
    private const val LISTEN_GAP_MS = 1200L
    private const val AUTO_PAUSE_FACTOR = 1.2

    /** Repeat-aloud pause after a clip: ≈1.2× the clip as heard at [speed], or a fixed time. */
    fun pauseMs(clipMs: Long, settings: MemorizeSettings): Long = when (settings.pauseMode) {
        PauseMode.AUTO -> (clipMs / settings.speed * AUTO_PAUSE_FACTOR).toLong()
        PauseMode.MANUAL -> settings.manualPauseSeconds * 1000L
    }

    /** Step 1: the full dua, twice. */
    fun listen(dua: Dua, title: String): List<SequenceItem> {
        val audio = dua.audio ?: return emptyList()
        val all = dua.phrases.indices
        return buildList {
            repeat(LISTEN_TIMES) { i ->
                add(SequenceItem.Clip(dua.id, audio.file, audio.startMs, audio.endMs ?: androidx.media3.common.C.TIME_END_OF_SOURCE, all, title))
                if (i < LISTEN_TIMES - 1) add(SequenceItem.Pause(LISTEN_GAP_MS, all, title))
            }
        }
    }

    /**
     * Step 2: cumulative chunks 1 → 1+2 → 1+2+3 …, each played [MemorizeSettings.loopsPerChunk]
     * times with a repeat-aloud pause after every play. Falls back to whole-dua loops when the dua
     * has no phrase segments.
     */
    fun build(dua: Dua, settings: MemorizeSettings, title: String): List<SequenceItem> {
        val audio = dua.audio ?: return emptyList()
        val phrases = dua.phrases
        val segmented = phrases.isNotEmpty() && phrases.all { it.segment != null }
        val chunks: List<IntRange> = if (segmented) phrases.indices.map { 0..it } else listOf(phrases.indices)
        return buildList {
            for (chunk in chunks) {
                val start = if (segmented) phrases[chunk.first].segment!!.startMs else audio.startMs
                val end = if (segmented) phrases[chunk.last].segment!!.endMs else audio.endMs ?: androidx.media3.common.C.TIME_END_OF_SOURCE
                val clipMs = when {
                    segmented -> end - start
                    audio.range != null -> audio.range.durationMs
                    else -> dua.phrases.lastOrNull()?.segment?.endMs ?: 3000L
                }
                repeat(settings.loopsPerChunk.coerceIn(1, 10)) {
                    add(SequenceItem.Clip(dua.id, audio.file, start, end, chunk, title))
                    add(SequenceItem.Pause(pauseMs(clipMs, settings), chunk, title))
                }
            }
        }
    }

    /** A–B loop over phrases [a]..[b]. */
    fun abLoop(dua: Dua, a: Int, b: Int, settings: MemorizeSettings, title: String): List<SequenceItem> {
        val audio = dua.audio ?: return emptyList()
        val from = dua.phrases.getOrNull(minOf(a, b))?.segment ?: return emptyList()
        val to = dua.phrases.getOrNull(maxOf(a, b))?.segment ?: return emptyList()
        val range = minOf(a, b)..maxOf(a, b)
        return buildList {
            repeat(settings.loopsPerChunk.coerceIn(1, 10)) {
                add(SequenceItem.Clip(dua.id, audio.file, from.startMs, to.endMs, range, title))
                add(SequenceItem.Pause(pauseMs(to.endMs - from.startMs, settings), range, title))
            }
        }
    }

    /**
     * Recall by ear: visible phrases are recited, hidden phrases become silence for the user to fill.
     * Used instead of the visual fade when TalkBack is on.
     */
    fun recallWithGaps(dua: Dua, hiddenPhrases: Set<Int>, settings: MemorizeSettings, title: String): List<SequenceItem> {
        val audio = dua.audio ?: return emptyList()
        return dua.phrases.mapNotNull { p ->
            val seg = p.segment ?: return@mapNotNull null
            if (p.index in hiddenPhrases) {
                SequenceItem.Pause((seg.durationMs / settings.speed * AUTO_PAUSE_FACTOR).toLong(), p.index..p.index, title)
            } else {
                SequenceItem.Clip(dua.id, audio.file, seg.startMs, seg.endMs, p.index..p.index, title)
            }
        }
    }
}

/** Recall levels: share of words hidden. */
enum class FadeLevel(val fraction: Double) { NONE(0.0), LIGHT(0.3), MEDIUM(0.6), FULL(1.0) }

/**
 * Vanishing cues: the hints fade stage by stage as recall holds. 0 is the full text; stages 1–4 hide
 * 30% and 60% of the words, then all words with first letters, then all words.
 */
object FadeLadder {
    data class Stage(val fade: FadeLevel, val firstLetters: Boolean)

    val STAGES = listOf(
        Stage(FadeLevel.NONE, false),
        Stage(FadeLevel.LIGHT, false),
        Stage(FadeLevel.MEDIUM, false),
        Stage(FadeLevel.FULL, true),
        Stage(FadeLevel.FULL, false),
    )
    val LAST = STAGES.lastIndex

    enum class Outcome { UP, SAME, DOWN, DONE }

    /** The stage a manual choice sits on; first letters only count once every word is hidden. */
    fun stageOf(fade: FadeLevel, firstLetters: Boolean): Int = when (fade) {
        FadeLevel.NONE -> 0
        FadeLevel.LIGHT -> 1
        FadeLevel.MEDIUM -> 2
        FadeLevel.FULL -> if (firstLetters) 3 else 4
    }

    /**
     * After a recital at [stage]: with no word revealed the hints fade a stage (DONE at the last);
     * up to a third of the hidden words revealed keeps the stage; more brings a hint back.
     */
    fun outcome(stage: Int, revealed: Int, hidden: Int): Outcome = when {
        revealed == 0 -> if (stage >= LAST) Outcome.DONE else Outcome.UP
        revealed * 3 <= hidden || stage <= 1 -> Outcome.SAME
        else -> Outcome.DOWN
    }

    fun next(stage: Int, outcome: Outcome): Int = when (outcome) {
        Outcome.UP -> (stage + 1).coerceAtMost(LAST)
        Outcome.DOWN -> (stage - 1).coerceAtLeast(1)
        Outcome.SAME, Outcome.DONE -> stage
    }
}

/** Which words go first as the level rises: a stable shuffle, or the last words first (backward chaining). */
enum class FadeOrder { RANDOM, FROM_END }

/** A word of the Arabic text and the phrase it belongs to. */
data class Word(val index: Int, val text: String, val phrase: Int)

object Recall {
    fun words(dua: Dua): List<Word> {
        var i = 0
        return dua.phrases.flatMap { p ->
            p.text.split(Regex("\\s+")).filter { it.isNotBlank() }.map { Word(i++, it, p.index) }
        }
    }

    /**
     * Words hidden at a level. Levels are nested (everything hidden at 30% stays hidden at 60%), and
     * the choice is stable per dua so the same words disappear each time.
     */
    fun hiddenWords(words: List<Word>, level: FadeLevel, seed: String, order: FadeOrder = FadeOrder.RANDOM): Set<Int> {
        // Tokens without a letter (a lone ، or ۝) stay visible: there is nothing to recall in them.
        val candidates = words.filter { w -> w.text.any(::isLetter) }.map { it.index }
        if (level == FadeLevel.NONE || candidates.isEmpty()) return emptySet()
        val sequence = when (order) {
            FadeOrder.RANDOM -> candidates.shuffled(Random(seed.hashCode()))
            FadeOrder.FROM_END -> candidates.reversed()
        }
        val n = ceil(candidates.size * level.fraction).toInt().coerceIn(1, candidates.size)
        return sequence.take(n).toSet()
    }

    /** The first hidden word in reading order, shown next by "reveal as you go". */
    fun nextHidden(words: List<Word>, hidden: Set<Int>): Word? = words.firstOrNull { it.index in hidden }

    private const val TATWEEL = 'ـ'

    /** Letters that never join to the following letter, so a cue ending in one needs no tatweel. */
    private const val RIGHT_JOINING = "ءآأؤإاةدذرزوٱ"

    private fun isLetter(c: Char) = Character.isLetter(c) && Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC

    private fun isMark(c: Char) = Character.getType(c) == Character.NON_SPACING_MARK.toInt()

    /**
     * First-letter cue for a hidden word: the word's opening letter with its marks, exactly as in
     * the source. After the article (ا/ٱ + ل) the next letter is included too, since the article
     * alone tells nothing. A tatweel keeps the joined shape when more letters follow. Words with no
     * Arabic letter (e.g. ۝) are returned unchanged.
     */
    fun firstLetterCue(word: String): String {
        val start = word.indexOfFirst(::isLetter).takeIf { it >= 0 } ?: return word
        // End index (exclusive) of each letter cluster: a letter and the marks that follow it.
        val clusterEnds = buildList {
            var i = start
            while (i < word.length && size < 3) {
                if (!isLetter(word[i])) break
                i++
                while (i < word.length && isMark(word[i])) i++
                add(i)
            }
        }
        val first = word[start]
        val article = (first == 'ا' || first == 'ٱ') && clusterEnds.size >= 3 &&
            word.getOrNull(clusterEnds[0]) == 'ل'
        val end = if (article) clusterEnds[2] else clusterEnds[0]
        val cue = word.substring(0, end)
        val lastLetter = cue.last { isLetter(it) }
        val more = word.substring(end).any(::isLetter)
        return if (more && lastLetter !in RIGHT_JOINING) cue + TATWEEL else cue
    }

    /** For audio recall: a phrase is muted when at least half of its words are hidden. */
    fun hiddenPhrases(words: List<Word>, hidden: Set<Int>): Set<Int> =
        words.groupBy { it.phrase }.filter { (_, ws) -> ws.count { it.index in hidden } * 2 >= ws.size }.keys
}
