package id.haho.muslimdaily.data.content

/**
 * Last line of defence at import time. The build already refuses incomplete content; this
 * guards against a malformed asset reaching users.
 */
internal object ContentValidator {

    sealed interface Result {
        data object Ok : Result
        data class Rejected(val reason: String) : Result
    }

    fun validate(dua: DuaJson): Result {
        if (dua.arabic.isBlank() || dua.arabic.contains("[PLACEHOLDER")) return Result.Rejected("empty Arabic")
        if (dua.count != null && dua.count < 1) return Result.Rejected("invalid count")
        val breaks = dua.phraseBreaks
        if (breaks != breaks.sorted().distinct() || breaks.any { it <= 0 || it >= dua.arabic.length }) {
            return Result.Rejected("invalid phrase breaks")
        }
        if (dua.sources.none { it.book.isNotBlank() }) return Result.Rejected("missing source")
        val g = dua.grading
        if (g != null && g.grade.isNotBlank()) {
            if (g.grader.isBlank()) return Result.Rejected("grade without grader")
            if (g.grade.lowercase() !in AllowedGrades) return Result.Rejected("grade out of scope")
        }
        return Result.Ok
    }

    /** Segments are kept only if they are well-formed and match the phrase count. */
    fun segmentsUsable(dua: DuaJson): Boolean {
        val segments = dua.audio?.segments ?: return false
        if (segments.size != dua.phraseBreaks.size + 1) return false
        var last = 0.0
        for (s in segments) {
            if (s.size != 2 || s[1] <= s[0] || s[0] < last - 1e-6) return false
            last = s[1]
        }
        return true
    }

    /** The dua's part of the recording, when it is well-formed. */
    fun rangeUsable(dua: DuaJson): List<Double>? =
        dua.audio?.range?.takeIf { it.size == 2 && it[0] >= 0 && it[1] > it[0] }

    private val AllowedGrades = setOf("sahih", "hasan")
}
