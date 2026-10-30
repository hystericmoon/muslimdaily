package id.haho.muslimdaily.data.content

import id.haho.muslimdaily.core.arabic.ArabicNormalizer
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.db.DuaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Read access to duas, taxonomy and rawatib. Every query waits for the content import. */
@Singleton
class ContentRepository @Inject constructor(
    private val db: ContentDatabase,
    private val importer: ContentImporter,
) {
    private val dao get() = db.contentDao()

    private fun <T> afterImport(block: () -> Flow<T>): Flow<T> = flow {
        importer.ensureImported()
        block().collect { emit(it) }
    }

    /** Library categories. Events without any dua (and categories left empty) are not listed. */
    fun categories(): Flow<List<DuaCategory>> = afterImport {
        combine(dao.categories(), dao.eventsWithCounts()) { cats, events ->
            cats.map { c ->
                DuaCategory(
                    id = c.id,
                    labelId = c.label,
                    order = c.sortOrder,
                    events = events.filter { it.categoryId == c.id && it.duaCount > 0 }
                        .map { DuaEvent(it.id, it.categoryId, it.label, it.sortOrder, it.duaCount, it.labelEn) },
                    labelEn = c.labelEn,
                )
            }.filter { it.events.isNotEmpty() }
        }
    }

    fun event(eventId: String): Flow<DuaEvent?> = afterImport {
        dao.observeEvent(eventId).map { e -> e?.let { DuaEvent(it.id, it.categoryId, it.label, it.sortOrder, labelEn = it.labelEn) } }
    }

    suspend fun eventOnce(eventId: String): DuaEvent? {
        importer.ensureImported()
        return dao.event(eventId)?.let { DuaEvent(it.id, it.categoryId, it.label, it.sortOrder, labelEn = it.labelEn) }
    }

    fun duasForEvent(eventId: String): Flow<List<Dua>> = afterImport {
        dao.duasForEvent(eventId).map { list -> list.map { it.toModel() } }
    }

    suspend fun duasForEventOnce(eventId: String): List<Dua> {
        importer.ensureImported()
        return dao.duasForEventOnce(eventId).map { it.toModel() }
    }

    fun dua(id: String): Flow<Dua?> = afterImport { dao.observeDua(id).map { it?.toModel() } }

    suspend fun duaOnce(id: String): Dua? {
        importer.ensureImported()
        return dao.dua(id)?.toModel()
    }

    suspend fun duas(ids: List<String>): List<Dua> {
        importer.ensureImported()
        if (ids.isEmpty()) return emptyList()
        val byId = dao.duas(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toModel() }
    }

    fun allDuas(): Flow<List<Dua>> = afterImport { dao.allDuas().map { list -> list.map { it.toModel() } } }

    @Volatile private var twins: Map<String, List<String>>? = null

    /**
     * Duas with the same Arabic (morning and evening adhkar, ruku and sujud…), each id mapped to
     * its group in library order. Ids with no twin are left out. Content only changes with an
     * update, so the index is built once per process.
     */
    suspend fun twinsOnce(): Map<String, List<String>> {
        twins?.let { return it }
        importer.ensureImported()
        val categoryOrder = dao.categories().first().associate { it.id to it.sortOrder }
        val eventOrder = dao.eventsWithCounts().first().associate { it.id to it.sortOrder }
        val groups = dao.allDuas().first()
            .sortedWith(compareBy({ categoryOrder[it.category] ?: Int.MAX_VALUE }, { eventOrder[it.event] ?: Int.MAX_VALUE }, { it.sortOrder }, { it.id }))
            .groupBy { it.arabic.trim() }
            .values
            .filter { it.size > 1 }
            .map { group -> group.map { it.id } }
        return groups.flatMap { ids -> ids.map { it to ids } }.toMap().also { twins = it }
    }

    /**
     * Matches in the occasion's name come first, then in the transliteration (how people
     * remember a dua: "ayat kursi", "sayyidul istighfar"), then anywhere else.
     */
    suspend fun search(query: String): List<Dua> {
        importer.ensureImported()
        val fts = ArabicNormalizer.toFtsQuery(query) ?: return emptyList()
        val ranked = LinkedHashMap<String, DuaEntity>()
        for (column in SEARCH_TIERS) {
            val scoped = if (column == null) fts else fts.split(' ').joinToString(" ") { "$column:$it" }
            dao.search(scoped).forEach { ranked.putIfAbsent(it.id, it) }
            if (ranked.size >= SEARCH_LIMIT) break
        }
        return ranked.values.take(SEARCH_LIMIT).map { it.toModel() }
    }

    fun rawatib(): Flow<List<Rawatib>> = afterImport { dao.rawatib().map { list -> list.map { it.toModel() } } }

    fun fard(): Flow<List<Fard>> = afterImport { dao.fard().map { list -> list.map { it.toModel() } } }

    suspend fun contentVersion(): Int = importer.contentVersion()
}

private const val SEARCH_LIMIT = 100

/** FTS columns searched first, in order; null is every column. */
private val SEARCH_TIERS = listOf("eventLabel", "transliteration", null)
