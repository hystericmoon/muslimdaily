package id.haho.muslimdaily.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.core.arabic.ArabicNormalizer
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.data.db.CategoryEntity
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.db.DuaEntity
import id.haho.muslimdaily.data.db.DuaFtsEntity
import id.haho.muslimdaily.data.db.EventEntity
import id.haho.muslimdaily.data.db.FardEntity
import id.haho.muslimdaily.data.db.MetaEntity
import id.haho.muslimdaily.data.db.RawatibEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports the bundled content (assets/content/…) into Room on first launch and whenever the
 * files change (content version bump or new build). Release builds only ever see published
 * entries: the build strips drafts, and this importer rejects them again.
 */
@Singleton
class ContentImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ContentDatabase,
) {
    private val mutex = Mutex()

    @Volatile private var ready = false

    suspend fun ensureImported() {
        if (ready) return
        mutex.withLock {
            if (ready) return
            withContext(Dispatchers.IO) { importIfChanged() }
            ready = true
        }
    }

    private suspend fun importIfChanged() {
        val files = CONTENT_FILES.associateWith { name ->
            context.assets.open("content/$name").use { it.readBytes() }
        }
        val hash = sha256(files.values.toList(), BuildConfig.INCLUDE_DRAFT_CONTENT)
        val dao = db.contentDao()
        if (dao.meta(META_HASH) == hash) return

        val bundle = ContentMapper.map(
            duas = ContentJsonFormat.decodeFromString(DuasFile.serializer(), files.getValue("duas.json").decodeToString()),
            rawatib = ContentJsonFormat.decodeFromString(RawatibFile.serializer(), files.getValue("rawatib.json").decodeToString()),
            taxonomy = ContentJsonFormat.decodeFromString(TaxonomyFile.serializer(), files.getValue("taxonomy.json").decodeToString()),
            includeDrafts = BuildConfig.INCLUDE_DRAFT_CONTENT,
        )
        dao.replaceAll(
            bundle.categories, bundle.events, bundle.duas, bundle.fts, bundle.rawatib, bundle.fard,
            MetaEntity(META_HASH, hash),
        )
        dao.putMeta(MetaEntity(META_VERSION, bundle.contentVersion.toString()))
    }

    suspend fun contentVersion(): Int {
        ensureImported()
        return db.contentDao().meta(META_VERSION)?.toIntOrNull() ?: 0
    }

    private fun sha256(parts: List<ByteArray>, drafts: Boolean): String {
        val md = MessageDigest.getInstance("SHA-256")
        parts.forEach { md.update(it) }
        md.update(if (drafts) 1 else 0)
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val CONTENT_FILES = listOf("duas.json", "rawatib.json", "taxonomy.json")
        const val META_HASH = "content_hash"
        const val META_VERSION = "content_version"
    }
}

internal class ContentBundle(
    val contentVersion: Int,
    val categories: List<CategoryEntity>,
    val events: List<EventEntity>,
    val duas: List<DuaEntity>,
    val fts: List<DuaFtsEntity>,
    val rawatib: List<RawatibEntity>,
    val fard: List<FardEntity>,
    val rejected: Map<String, String>,
)

/** Pure mapping from JSON files to database rows; unit-tested without Android. */
internal object ContentMapper {
    private val sourcesSerializer = ListSerializer(SourceRef.serializer())
    private val gradingsSerializer = ListSerializer(GradingJson.serializer())
    private val segmentsSerializer = ListSerializer(ListSerializer(Double.serializer()))
    private val intsSerializer = ListSerializer(Int.serializer())

    fun map(duas: DuasFile, rawatib: RawatibFile, taxonomy: TaxonomyFile, includeDrafts: Boolean): ContentBundle {
        val categories = taxonomy.categories.map { CategoryEntity(it.id, it.label, it.labelEn, it.order) }
        val events = taxonomy.categories.flatMap { c ->
            c.events.map { e -> EventEntity(e.id, c.id, e.label, e.labelEn, c.order * 1000 + e.order) }
        }
        val eventLabels = events.associate { it.id to it.label }
        val rejected = mutableMapOf<String, String>()
        val accepted = duas.duas.filter { d ->
            when (val r = ContentValidator.validate(d, includeDrafts)) {
                ContentValidator.Result.Ok -> d.event in eventLabels || run { rejected[d.id] = "unknown event"; false }
                is ContentValidator.Result.Rejected -> { rejected[d.id] = r.reason; false }
            }
        }
        val duaRows = accepted.map { d ->
            val segments = if (ContentValidator.segmentsUsable(d)) d.audio?.segments.orEmpty() else emptyList()
            DuaEntity(
                id = d.id,
                category = d.category,
                event = d.event,
                sortOrder = d.order,
                variantGroup = d.variantGroup,
                arabic = d.arabic,
                isQuran = d.isQuran,
                translationId = d.translations["id"].orEmpty(),
                translationEn = d.translations["en"].orEmpty(),
                transliteration = d.transliteration,
                count = d.count,
                sourcesJson = ContentJsonFormat.encodeToString(sourcesSerializer, d.sources.filter { it.book.isNotBlank() || it.no.isNotBlank() }),
                gradingGrade = d.grading?.grade?.takeIf { it.isNotBlank() },
                gradingGrader = d.grading?.grader?.takeIf { it.isNotBlank() },
                altGradingsJson = ContentJsonFormat.encodeToString(gradingsSerializer, d.altGradings),
                virtueText = d.virtue?.text?.takeIf { it.isNotBlank() },
                virtueSource = d.virtue?.source?.takeIf { it.isNotBlank() },
                audioFile = d.audio?.file?.takeIf { it.isNotBlank() },
                segmentsJson = ContentJsonFormat.encodeToString(segmentsSerializer, segments),
                phraseBreaksJson = ContentJsonFormat.encodeToString(intsSerializer, d.phraseBreaks),
                status = d.status.name,
            )
        }
        val fts = accepted.map { d ->
            DuaFtsEntity(
                duaId = d.id,
                arabic = ArabicNormalizer.normalize(d.arabic),
                translationId = ArabicNormalizer.normalize(d.translations["id"].orEmpty()),
                translationEn = ArabicNormalizer.normalize(d.translations["en"].orEmpty()),
                transliteration = ArabicNormalizer.normalize(d.transliteration),
                eventLabel = ArabicNormalizer.normalize(eventLabels[d.event].orEmpty()),
            )
        }
        val shipped = { status: id.haho.muslimdaily.core.model.ContentStatus ->
            includeDrafts || status == id.haho.muslimdaily.core.model.ContentStatus.PUBLISHED
        }
        val rawatibRows = rawatib.rawatib.filter { shipped(it.status) }.mapIndexed { i, r ->
            RawatibEntity(
                id = r.id,
                prayer = r.prayer?.name,
                position = r.position,
                rakah = r.rakah,
                tier = r.tier,
                evidenceJson = ContentJsonFormat.encodeToString(sourcesSerializer, r.evidence),
                notes = r.notes,
                hiddenWhenTravelling = r.hiddenWhenTravelling,
                variantGroup = r.variantGroup,
                variantLabel = r.variantLabel,
                defaultVariant = r.defaultVariant,
                sortOrder = i,
                status = r.status.name,
            )
        }
        val fardRows = rawatib.fard.filter { shipped(it.status) }.map { FardEntity(it.prayer.name, it.rakah, it.status.name) }
        return ContentBundle(
            contentVersion = duas.contentVersion,
            categories = categories,
            events = events,
            duas = duaRows,
            fts = fts,
            rawatib = rawatibRows,
            fard = fardRows,
            rejected = rejected,
        )
    }
}
