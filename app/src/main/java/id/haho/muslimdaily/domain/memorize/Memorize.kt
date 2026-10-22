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
                add(SequenceItem.Clip(dua.id, audio.file, 0, androidx.media3.common.C.TIME_END_OF_SOURCE, all, title))
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
                val start = if (segmented) phrases[chunk.first].segment!!.startMs else 0L
                val end = if (segmented) phrases[chunk.last].segment!!.endMs else androidx.media3.common.C.TIME_END_OF_SOURCE
                val clipMs = if (segmented) end - start else dua.phrases.lastOrNull()?.segment?.endMs ?: 3000L
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
    fun hiddenWords(words: List<Word>, level: FadeLevel, seed: String): Set<Int> {
        if (level == FadeLevel.NONE || words.isEmpty()) return emptySet()
        val order = words.indices.shuffled(Random(seed.hashCode()))
        val n = ceil(words.size * level.fraction).toInt().coerceIn(1, words.size)
        return order.take(n).toSet()
    }

    /** For audio recall: a phrase is muted when at least half of its words are hidden. */
    fun hiddenPhrases(words: List<Word>, hidden: Set<Int>): Set<Int> =
        words.groupBy { it.phrase }.filter { (_, ws) -> ws.count { it.index in hidden } * 2 >= ws.size }.keys
}
