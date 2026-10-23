package id.haho.muslimdaily.data.note

import id.haho.muslimdaily.data.db.DuaNoteEntity
import id.haho.muslimdaily.data.db.UserDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Private notes on duas. Kept on the device (and in backups); never shown to anyone else. */
@Singleton
class NoteRepository @Inject constructor(private val db: UserDatabase, private val clock: Clock) {
    private val dao get() = db.noteDao()

    fun note(duaId: String): Flow<String?> = dao.observe(duaId).map { it?.text }

    /** Ids of duas that have a note, for a marker in lists. */
    fun notedIds(): Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    /** A blank note deletes it. */
    suspend fun save(duaId: String, text: String) {
        val trimmed = text.trim().take(MAX_LENGTH)
        if (trimmed.isEmpty()) dao.delete(duaId) else dao.upsert(DuaNoteEntity(duaId, trimmed, clock.millis()))
    }

    companion object {
        const val MAX_LENGTH = 2000
    }
}
