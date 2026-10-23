package id.haho.muslimdaily.data.backup

import androidx.room.withTransaction
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.data.db.ChecklistEntity
import id.haho.muslimdaily.data.db.DuaNoteEntity
import id.haho.muslimdaily.data.db.ReviewItemEntity
import id.haho.muslimdaily.data.db.RoutineEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One file with everything the user has made: settings, personal sets, memorisation progress
 * and checklist ticks. Restoring replaces all of it. A sideloaded app gets no Play backup, so
 * this is how data survives a new phone or a reinstall.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val mySets: MySetRepository,
    private val db: UserDatabase,
    private val clock: Clock,
) {
    @Serializable
    data class Setting(val type: String, val value: String)

    @Serializable
    data class Review(val duaId: String, val box: Int, val dueEpochDay: Long, val lastReviewedEpochDay: Long?, val addedEpochDay: Long)

    @Serializable
    data class Tick(val epochDay: Long, val itemKey: String)

    @Serializable
    data class NoteJson(val duaId: String, val text: String, val updatedAt: Long)

    @Serializable
    data class RoutineJson(val id: String, val name: String, val position: Int, val updatedAt: Long, val duaIds: List<String>)

    @Serializable
    data class BackupFile(
        val format: String = FORMAT,
        val version: Int = VERSION,
        val createdAt: String,
        val appVersion: String,
        val settings: Map<String, Setting>,
        /** The 'Set saya' export, as its own JSON document. */
        val mySets: String,
        val reviews: List<Review>,
        val checklist: List<Tick>,
        /** Since version 2. */
        val routines: List<RoutineJson> = emptyList(),
        /** Since version 3. */
        val notes: List<NoteJson> = emptyList(),
    )

    /** What a restore brought back, for the confirmation message. */
    data class Summary(val settings: Int, val sets: Int, val reviews: Int, val checklist: Int)

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(): String {
        val file = BackupFile(
            createdAt = Instant.now(clock).toString(),
            appVersion = BuildConfig.VERSION_NAME,
            settings = settings.export().mapValues { (_, v) -> Setting(v.first, v.second) },
            mySets = mySets.exportJson(),
            reviews = db.reviewDao().all().map { Review(it.duaId, it.box, it.dueEpochDay, it.lastReviewedEpochDay, it.addedEpochDay) },
            checklist = db.checklistDao().all().map { Tick(it.epochDay, it.itemKey) },
            routines = db.routineDao().let { dao ->
                val items = dao.allItems().groupBy { it.routineId }
                dao.all().map { r -> RoutineJson(r.id, r.name, r.position, r.updatedAt, items[r.id].orEmpty().sortedBy { it.position }.map { it.duaId }) }
            },
            notes = db.noteDao().all().map { NoteJson(it.duaId, it.text, it.updatedAt) },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Throws [IllegalArgumentException] for a file that is not a Muslim Daily backup. */
    suspend fun restore(text: String): Summary {
        val file = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("Not a backup file", it) }
        require(file.format == FORMAT) { "Not a backup file" }
        require(file.version <= VERSION) { "Backup from a newer version" }
        // One transaction, and not cancellable: leaving the screen mid-restore must not leave
        // the data half replaced.
        val sets = withContext(NonCancellable) {
            db.withTransaction {
                val sets = mySets.importJson(file.mySets, replaceAll = true)
                db.reviewDao().deleteAll()
                db.reviewDao().insertAll(file.reviews.map { ReviewItemEntity(it.duaId, it.box, it.dueEpochDay, it.lastReviewedEpochDay, it.addedEpochDay) })
                db.checklistDao().deleteAll()
                db.checklistDao().insertAll(file.checklist.map { ChecklistEntity(it.epochDay, it.itemKey) })
                db.routineDao().deleteAllItems()
                db.routineDao().deleteAll()
                file.routines.forEach { r ->
                    db.routineDao().save(RoutineEntity(r.id, r.name, r.position, r.updatedAt), r.duaIds)
                }
                db.noteDao().deleteAll()
                db.noteDao().insertAll(file.notes.map { DuaNoteEntity(it.duaId, it.text, it.updatedAt) })
                sets
            }.also {
                // Settings last: they trigger alarm rescheduling and widget redraws.
                settings.restore(file.settings.mapValues { (_, s) -> s.type to s.value })
            }
        }
        return Summary(file.settings.size, sets, file.reviews.size, file.checklist.size)
    }

    companion object {
        const val FORMAT = "muslimdaily.backup"
        const val VERSION = 3
    }
}
