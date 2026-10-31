package id.haho.muslimdaily.data.content

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.SourceRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON shapes of the bundled content files in /content. See docs/content-guide.md. */
internal val ContentJsonFormat = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

@Serializable
internal data class DuasFile(
    @SerialName("content_version") val contentVersion: Int = 0,
    val duas: List<DuaJson> = emptyList(),
)

@Serializable
internal data class DuaJson(
    val id: String,
    val category: String,
    val event: String,
    val order: Int = 0,
    @SerialName("variant_group") val variantGroup: String? = null,
    val arabic: String = "",
    /** The book's full entry when [arabic] is only the words to recite (see content guide). */
    @SerialName("arabic_full") val arabicFull: String = "",
    @SerialName("is_quran") val isQuran: Boolean = false,
    val translations: Map<String, String> = emptyMap(),
    @SerialName("translations_full") val translationsFull: Map<String, String> = emptyMap(),
    val transliteration: String = "",
    val count: Int? = null,
    val sources: List<SourceRef> = emptyList(),
    val reference: String = "",
    val grading: GradingJson? = null,
    @SerialName("alt_gradings") val altGradings: List<GradingJson> = emptyList(),
    val virtue: VirtueJson? = null,
    val audio: AudioJson? = null,
    @SerialName("phrase_breaks") val phraseBreaks: List<Int> = emptyList(),
    /** "hadith" when there is nothing to recite; absent for a dua. */
    val kind: String = "",
    val footnote: String = "",
)

@Serializable
internal data class GradingJson(val grade: String = "", val grader: String = "")

@Serializable
internal data class VirtueJson(val text: String = "", val source: String = "")

@Serializable
internal data class AudioJson(
    val file: String = "",
    val segments: List<List<Double>> = emptyList(),
    /** Start and end of the dua in the file, when the recording also reads its context. */
    val range: List<Double> = emptyList(),
)

@Serializable
internal data class RawatibFile(
    @SerialName("content_version") val contentVersion: Int = 0,
    val fard: List<FardJson> = emptyList(),
    val rawatib: List<RawatibJson> = emptyList(),
)

@Serializable
internal data class FardJson(
    val id: String,
    val prayer: Prayer,
    val rakah: Int,
)

@Serializable
internal data class RawatibJson(
    val id: String,
    val prayer: Prayer? = null,
    val position: String,
    val rakah: Int? = null,
    val tier: String,
    val evidence: List<SourceRef> = emptyList(),
    val notes: String? = null,
    @SerialName("hidden_when_travelling") val hiddenWhenTravelling: Boolean = true,
    @SerialName("variant_group") val variantGroup: String? = null,
    @SerialName("variant_label") val variantLabel: String? = null,
    @SerialName("default_variant") val defaultVariant: Boolean = false,
)

@Serializable
internal data class TaxonomyFile(val categories: List<CategoryJson> = emptyList())

@Serializable
internal data class CategoryJson(
    val id: String,
    val label: String,
    @SerialName("label_en") val labelEn: String = "",
    val order: Int = 0,
    val events: List<EventJson> = emptyList(),
)

@Serializable
internal data class EventJson(
    val id: String,
    val label: String,
    @SerialName("label_en") val labelEn: String = "",
    val order: Int = 0,
)
