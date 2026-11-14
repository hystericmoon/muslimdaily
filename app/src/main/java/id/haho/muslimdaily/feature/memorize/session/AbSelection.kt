package id.haho.muslimdaily.feature.memorize.session

/** The A–B range as the learner builds it by tapping phrases: first tap sets one end, second the other. */
data class AbSelection(val range: IntRange, val awaitingSecond: Boolean = false) {
    /** A tap on phrase [index]: the first tap starts a new range there, the second closes it (either order). */
    fun tap(index: Int): AbSelection = if (awaitingSecond) {
        AbSelection(minOf(range.first, index)..maxOf(range.first, index))
    } else {
        AbSelection(index..index, awaitingSecond = true)
    }

    companion object {
        fun whole(phraseCount: Int) = AbSelection(0..(phraseCount - 1).coerceAtLeast(0))
    }
}
