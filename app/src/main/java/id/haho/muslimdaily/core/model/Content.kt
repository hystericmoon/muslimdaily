package id.haho.muslimdaily.core.model

data class Grading(val grade: String, val grader: String)

data class Virtue(val text: String, val source: String)

/** Start and end of one recited phrase in the dua's audio file, in seconds. */
data class Segment(val start: Double, val end: Double) {
    val durationMs: Long get() = ((end - start) * 1000).toLong()
    val startMs: Long get() = (start * 1000).toLong()
    val endMs: Long get() = (end * 1000).toLong()
}

/**
 * A dua's recitation. [range] is the dua's part of the file when the recording also reads its
 * context (the hadith wording, a note); playback then covers only that part.
 */
data class DuaAudio(val file: String, val segments: List<Segment>, val range: Segment? = null) {
    val startMs: Long get() = range?.startMs ?: 0L
    /** Where playback stops, or null for the end of the file. */
    val endMs: Long? get() = range?.endMs
}

/** A phrase of the Arabic text. [range] indexes into [Dua.arabic]; [segment] is its audio, if any. */
data class Phrase(val index: Int, val text: String, val range: IntRange, val segment: Segment?)

data class Dua(
    val id: String,
    val category: String,
    val event: String,
    val order: Int,
    val variantGroup: String?,
    val arabic: String,
    val isQuran: Boolean,
    val translationId: String,
    val translationEn: String,
    val transliteration: String,
    /** Repetitions, only where the Sunnah specifies them. Null means no count is shown. */
    val count: Int?,
    val sources: List<SourceRef>,
    /** Takhrij as printed in the source book (footnote text), shown with the sources. */
    val reference: String,
    val grading: Grading?,
    val altGradings: List<Grading>,
    val virtue: Virtue?,
    val audio: DuaAudio?,
    val phraseBreaks: List<Int>,
    /** The book's full entry (with the hadith wording and notes) when [arabic] is only the dua. */
    val arabicFull: String = "",
    val translationIdFull: String = "",
    val translationEnFull: String = "",
    /** Another wording the book gives as a note (the evening form), shown under the dua. */
    val footnote: String = "",
    /** False for a hadith or instruction with no words to recite: a reading card, no counter. */
    val isRecitable: Boolean = true,
) {
    val phrases: List<Phrase> by lazy { splitPhrases(arabic, phraseBreaks, audio?.segments.orEmpty()) }

    val isPlaceholder: Boolean get() = arabic.contains("[PLACEHOLDER")
}

/**
 * Splits [arabic] at [breaks]. Segments are attached only when their count matches the phrase
 * count; a mismatch means the content is inconsistent and highlighting is switched off.
 */
fun splitPhrases(arabic: String, breaks: List<Int>, segments: List<Segment>): List<Phrase> {
    val bounds = (listOf(0) + breaks.filter { it in 1 until arabic.length }.distinct().sorted() + arabic.length)
    val useSegments = segments.size == bounds.size - 1
    return bounds.zipWithNext().mapIndexed { i, (start, end) ->
        val raw = arabic.substring(start, end)
        val lead = raw.length - raw.trimStart().length
        val trimmed = raw.trim()
        val from = start + lead
        Phrase(
            index = i,
            text = trimmed,
            range = from until (from + trimmed.length),
            segment = if (useSegments) segments[i] else null,
        )
    }
}

data class DuaEvent(
    val id: String,
    val categoryId: String,
    val labelId: String,
    val order: Int,
    val duaCount: Int = 0,
    val labelEn: String = "",
) {
    /** The name in the app's language; read on use so a language switch applies at once. */
    val label: String get() = localizedLabel(labelId, labelEn)
}

data class DuaCategory(
    val id: String,
    val labelId: String,
    val order: Int,
    val events: List<DuaEvent>,
    val labelEn: String = "",
) {
    val label: String get() = localizedLabel(labelId, labelEn)
}

private fun localizedLabel(id: String, en: String): String =
    if (en.isNotBlank() && java.util.Locale.getDefault().language == "en") en else id

data class Rawatib(
    val id: String,
    /** Null for the general "between adhan and iqamah" prayer that applies to any prayer. */
    val prayer: Prayer?,
    val position: Position,
    val rakah: Int?,
    val tier: Tier,
    val evidence: List<SourceRef>,
    val notes: String?,
    val hiddenWhenTravelling: Boolean,
    val variantGroup: String?,
    val variantLabel: String?,
    val defaultVariant: Boolean,
)

data class Fard(val prayer: Prayer, val rakah: Int)
