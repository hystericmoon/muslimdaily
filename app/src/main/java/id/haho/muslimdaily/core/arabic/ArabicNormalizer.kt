package id.haho.muslimdaily.core.arabic

import java.text.Normalizer
import java.util.Locale

/**
 * Normalises Arabic (and Latin) text for search, applied identically when indexing and querying:
 * strips tashkeel and Qur'anic annotation marks, removes tatweel, unifies alif forms
 * (أ إ آ ٱ → ا), alif maqsura (ى → ي) and ta marbuta (ة → ه). Latin text is lower-cased and
 * stripped of diacritics so "doa", "Doa" and "dôa" match.
 */
object ArabicNormalizer {

    private const val TATWEEL = 'ـ'

    private fun isTashkeel(c: Char): Boolean =
        c in 'ً'..'ٟ' || // fathatan .. wavy hamza below
            c == 'ٰ' || // superscript alif
            c in 'ۖ'..'ۜ' || // Qur'anic small high ligatures and marks
            c in '۟'..'ۨ' ||
            c in '۪'..'ۭ' ||
            c in 'ؐ'..'ؚ'

    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            if (c == TATWEEL || isTashkeel(c)) continue
            sb.append(
                when (c) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا' // أ إ آ ٱ → ا
                    'ى' -> 'ي' // ى → ي
                    'ة' -> 'ه' // ة → ه
                    else -> c
                },
            )
        }
        val latin = Normalizer.normalize(sb.toString(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return latin.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Builds an FTS4 MATCH expression: every term must appear, each as a prefix. Characters with
     * meaning in FTS syntax are dropped so user input can never form an invalid query.
     */
    fun toFtsQuery(input: String): String? {
        val terms = normalize(input)
            .replace(Regex("[\"*^():\\-]"), " ")
            .split(' ')
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "$it*" }
    }
}
