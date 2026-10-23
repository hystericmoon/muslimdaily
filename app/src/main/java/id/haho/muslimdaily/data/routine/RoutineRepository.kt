package id.haho.muslimdaily.data.routine

import id.haho.muslimdaily.data.db.RoutineEntity
import id.haho.muslimdaily.data.db.UserDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A user-built routine: a name and dua ids in reading order. */
data class Routine(val id: String, val name: String, val duaIds: List<String>) {
    /** The run-mode key, so routines share run mode (and its resume) with the occasions. */
    val runKey: String get() = RUN_PREFIX + id

    companion object {
        const val RUN_PREFIX = "routine:"
        fun idFromRunKey(key: String): String? = key.takeIf { it.startsWith(RUN_PREFIX) }?.removePrefix(RUN_PREFIX)
    }
}

@Singleton
class RoutineRepository @Inject constructor(private val db: UserDatabase, private val clock: Clock) {
    private val dao get() = db.routineDao()

    fun routines(): Flow<List<Routine>> = combine(dao.observeRoutines(), dao.observeItems()) { routines, items ->
        val byRoutine = items.groupBy { it.routineId }
        routines.map { r -> Routine(r.id, r.name, byRoutine[r.id].orEmpty().sortedBy { it.position }.map { it.duaId }) }
    }

    suspend fun routine(id: String): Routine? {
        val r = dao.routine(id) ?: return null
        return Routine(r.id, r.name, dao.items(id).map { it.duaId })
    }

    /** Creates the routine when [id] is null. Returns its id. */
    suspend fun save(id: String?, name: String, duaIds: List<String>): String {
        val existing = id?.let { dao.routine(it) }
        val routine = RoutineEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            position = existing?.position ?: dao.nextPosition(),
            updatedAt = clock.millis(),
        )
        dao.save(routine, duaIds)
        return routine.id
    }

    suspend fun delete(id: String) = dao.remove(id)
}
