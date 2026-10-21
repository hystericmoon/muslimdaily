package id.haho.muslimdaily.data.run

import id.haho.muslimdaily.data.db.RunSessionEntity
import id.haho.muslimdaily.data.db.UserDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** One card in a run session. [target] is the Sunnah count, or null when no count is specified. */
@Serializable
data class RunItem(val duaId: String, val target: Int? = null) {
    /** Taps needed to complete the card: the count, or a single "read" tap. */
    val taps: Int get() = target ?: 1
}

data class RunSession(
    val eventId: String,
    val day: LocalDate,
    val items: List<RunItem>,
    val counts: List<Int>,
    val index: Int,
    val completed: Boolean,
) {
    val done: Int get() = items.indices.count { counts.getOrElse(it) { 0 } >= items[it].taps }
    val total: Int get() = items.size
}

data class RunSessionProgress(val done: Int, val total: Int, val completed: Boolean)

/**
 * Persists adhkar run sessions so they resume after an interruption. Sessions belong to a day and
 * are discarded the next day. These are worship counts, kept apart from memorisation loops.
 */
@Singleton
class RunSessionRepository @Inject constructor(private val db: UserDatabase) {
    private val dao get() = db.runSessionDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsSerializer = ListSerializer(RunItem.serializer())
    private val countsSerializer = ListSerializer(Int.serializer())

    fun progress(eventId: String, day: LocalDate): Flow<RunSessionProgress?> = dao.observe(eventId).map { e ->
        e?.takeIf { it.epochDay == day.toEpochDay() }?.toModel()?.let { RunSessionProgress(it.done, it.total, it.completed) }
    }

    suspend fun load(eventId: String, day: LocalDate): RunSession? =
        dao.session(eventId)?.takeIf { it.epochDay == day.toEpochDay() }?.toModel()

    suspend fun save(session: RunSession) {
        dao.upsert(
            RunSessionEntity(
                eventId = session.eventId,
                epochDay = session.day.toEpochDay(),
                itemsJson = json.encodeToString(itemsSerializer, session.items),
                countsJson = json.encodeToString(countsSerializer, session.counts),
                currentIndex = session.index,
                completed = session.completed,
            ),
        )
    }

    suspend fun clear(eventId: String) = dao.delete(eventId)

    suspend fun pruneBefore(day: LocalDate) = dao.deleteBefore(day.toEpochDay())

    private fun RunSessionEntity.toModel() = RunSession(
        eventId = eventId,
        day = LocalDate.ofEpochDay(epochDay),
        items = json.decodeFromString(itemsSerializer, itemsJson),
        counts = json.decodeFromString(countsSerializer, countsJson),
        index = currentIndex,
        completed = completed,
    )
}
