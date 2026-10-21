package id.haho.muslimdaily.feature.duas

import id.haho.muslimdaily.core.model.ContentStatus
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaAudio
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.core.model.Segment

/**
 * Preview and screenshot data. The Arabic is a neutral typographic sample ("this is sample text to
 * test the typeface"), NOT dua text: real content only ever comes from the reviewed spreadsheet.
 */
object DuaPreviewData {
    private const val SAMPLE = "هٰذَا نَصٌّ تَجْرِيبِيٌّ لِاخْتِبَارِ الْخَطِّ وَالْحَرَكَاتِ فِي سَطْرٍ طَوِيلٍ يَلْتَفُّ إِلَى السَّطْرِ التَّالِي"
    private val breaks = listOf(SAMPLE.indexOf("لِاخْتِبَارِ"), SAMPLE.indexOf("فِي سَطْرٍ"))

    fun dua(
        id: String,
        order: Int,
        count: Int? = null,
        group: String? = null,
        status: ContentStatus = ContentStatus.DRAFT,
    ) = Dua(
        id = id,
        category = "adhkar",
        event = "morning",
        order = order,
        variantGroup = group,
        arabic = SAMPLE,
        isQuran = false,
        translationId = "[Teks contoh untuk uji tipografi, bukan terjemahan doa.]",
        translationEn = "[Sample text for typography testing.]",
        transliteration = "[contoh transliterasi]",
        count = count,
        sources = emptyList(),
        grading = null,
        altGradings = emptyList(),
        virtue = null,
        audio = DuaAudio("$id.opus", listOf(Segment(0.0, 1.6), Segment(1.6, 3.4), Segment(3.4, 5.0))),
        phraseBreaks = breaks,
        status = status,
    )

    val duas = listOf(dua("morning_01", 1), dua("morning_02", 2, count = 3), dua("v1", 3, group = "g"), dua("v2", 4, group = "g"))

    val categories = listOf(
        DuaCategory(
            "daily", "Rutinitas harian", 1,
            listOf("Bangun tidur", "Kamar kecil", "Wudhu", "Berpakaian", "Keluar rumah", "Masuk rumah")
                .mapIndexed { i, l -> DuaEvent("e$i", "daily", l, i, if (i == 0) 1 else 0) },
        ),
        DuaCategory("adhkar", "Dzikir pagi & petang", 3, listOf(DuaEvent("morning", "adhkar", "Dzikir pagi", 1, 8), DuaEvent("evening", "adhkar", "Dzikir petang", 2, 6))),
    )
}
