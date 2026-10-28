package id.haho.muslimdaily.data.content

import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaAudio
import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Grading
import id.haho.muslimdaily.core.model.Position
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.core.model.Segment
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.core.model.Tier
import id.haho.muslimdaily.core.model.Virtue
import id.haho.muslimdaily.data.db.DuaEntity
import id.haho.muslimdaily.data.db.FardEntity
import id.haho.muslimdaily.data.db.RawatibEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val sources = ListSerializer(SourceRef.serializer())
private val gradings = ListSerializer(GradingJson.serializer())
private val segments = ListSerializer(ListSerializer(Double.serializer()))
private val ints = ListSerializer(Int.serializer())

internal fun DuaEntity.toModel(): Dua {
    val segs = ContentJsonFormat.decodeFromString(segments, segmentsJson).map { Segment(it[0], it[1]) }
    return Dua(
        id = id,
        category = category,
        event = event,
        order = sortOrder,
        variantGroup = variantGroup,
        arabic = arabic,
        isQuran = isQuran,
        translationId = translationId,
        translationEn = translationEn,
        transliteration = transliteration,
        count = count,
        sources = ContentJsonFormat.decodeFromString(sources, sourcesJson),
        reference = reference,
        grading = if (gradingGrade != null && gradingGrader != null) Grading(gradingGrade, gradingGrader) else null,
        altGradings = ContentJsonFormat.decodeFromString(gradings, altGradingsJson).map { Grading(it.grade, it.grader) },
        virtue = virtueText?.let { Virtue(it, virtueSource.orEmpty()) },
        audio = audioFile?.let { DuaAudio(it, segs, if (audioStart != null && audioEnd != null) Segment(audioStart, audioEnd) else null) },
        phraseBreaks = ContentJsonFormat.decodeFromString(ints, phraseBreaksJson),
        arabicFull = arabicFull,
        translationIdFull = translationIdFull,
    )
}

internal fun RawatibEntity.toModel() = Rawatib(
    id = id,
    prayer = prayer?.let { Prayer.valueOf(it) },
    position = Position.valueOf(position),
    rakah = rakah,
    tier = Tier.valueOf(tier),
    evidence = ContentJsonFormat.decodeFromString(sources, evidenceJson),
    notes = notes,
    hiddenWhenTravelling = hiddenWhenTravelling,
    variantGroup = variantGroup,
    variantLabel = variantLabel,
    defaultVariant = defaultVariant,
)

internal fun FardEntity.toModel() = Fard(Prayer.valueOf(prayer), rakah)
