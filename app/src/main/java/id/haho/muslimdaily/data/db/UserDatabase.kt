package id.haho.muslimdaily.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The user's own data: My set choices, run-mode sessions, the private daily checklist and the
 * memorisation review boxes. Worship counts (run sessions) and memorisation loops are stored in
 * separate tables and never mixed.
 */
@Database(
    entities = [
        EventPrefEntity::class, EventItemEntity::class, VariantChoiceEntity::class,
        RunSessionEntity::class, ChecklistEntity::class, ReviewItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun mySetDao(): MySetDao
    abstract fun runSessionDao(): RunSessionDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun reviewDao(): ReviewDao
}

/** Whether an event uses the full canonical set or the user's customised set. */
@Entity(tableName = "event_prefs")
data class EventPrefEntity(@PrimaryKey val eventId: String, val useMySet: Boolean)

/**
 * One row per item in a customised set. [itemKey] is a dua id, or "vg:<group>" for a variant
 * group shown as a single row.
 */
@Entity(tableName = "event_items", primaryKeys = ["eventId", "itemKey"])
data class EventItemEntity(val eventId: String, val itemKey: String, val enabled: Boolean, val position: Int)

/** Chosen form for a variant group: a dua id, or [ROTATE] to cycle forms each session. */
@Entity(tableName = "variant_choices", primaryKeys = ["eventId", "variantGroup"])
data class VariantChoiceEntity(
    val eventId: String,
    val variantGroup: String,
    val choice: String,
    val rotateIndex: Int,
) {
    companion object {
        const val ROTATE = "rotate"
    }
}

/** A run-mode (adhkar) session, kept so it can resume after an interruption. */
@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey val eventId: String,
    val epochDay: Long,
    /** JSON list of dua ids in session order. */
    val itemsJson: String,
    /** JSON list of counts made so far, parallel to itemsJson. */
    val countsJson: String,
    val currentIndex: Int,
    val completed: Boolean,
)

/** Private daily checklist tick. Rows from earlier days are deleted unless history is enabled. */
@Entity(tableName = "checklist", primaryKeys = ["epochDay", "itemKey"])
data class ChecklistEntity(val epochDay: Long, val itemKey: String)

/** Leitner box for a dua being memorised. */
@Entity(tableName = "review_items")
data class ReviewItemEntity(
    @PrimaryKey val duaId: String,
    val box: Int,
    val dueEpochDay: Long,
    val lastReviewedEpochDay: Long?,
    val addedEpochDay: Long,
)

@Dao
interface MySetDao {
    @Query("SELECT * FROM event_prefs WHERE eventId = :eventId")
    fun observePref(eventId: String): Flow<EventPrefEntity?>

    @Query("SELECT * FROM event_prefs")
    suspend fun allPrefs(): List<EventPrefEntity>

    @Upsert suspend fun upsertPref(pref: EventPrefEntity)

    @Query("SELECT * FROM event_items WHERE eventId = :eventId ORDER BY position")
    fun observeItems(eventId: String): Flow<List<EventItemEntity>>

    @Query("SELECT * FROM event_items ORDER BY eventId, position")
    suspend fun allItems(): List<EventItemEntity>

    @Query("SELECT * FROM variant_choices WHERE eventId = :eventId")
    fun observeChoices(eventId: String): Flow<List<VariantChoiceEntity>>

    @Query("SELECT * FROM variant_choices")
    suspend fun allChoices(): List<VariantChoiceEntity>

    @Query("SELECT * FROM variant_choices WHERE eventId = :eventId")
    suspend fun choices(eventId: String): List<VariantChoiceEntity>

    @Upsert suspend fun upsertChoice(choice: VariantChoiceEntity)

    @Query("DELETE FROM event_items WHERE eventId = :eventId") suspend fun clearItems(eventId: String)
    @Query("DELETE FROM variant_choices WHERE eventId = :eventId") suspend fun clearChoices(eventId: String)
    @Query("DELETE FROM event_prefs WHERE eventId = :eventId") suspend fun clearPref(eventId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertItems(items: List<EventItemEntity>)

    @Transaction
    suspend fun replaceItems(eventId: String, items: List<EventItemEntity>) {
        clearItems(eventId)
        insertItems(items)
    }

    @Transaction
    suspend fun reset(eventId: String) {
        clearItems(eventId)
        clearChoices(eventId)
        clearPref(eventId)
    }
}

@Dao
interface RunSessionDao {
    @Query("SELECT * FROM run_sessions WHERE eventId = :eventId")
    suspend fun session(eventId: String): RunSessionEntity?

    @Query("SELECT * FROM run_sessions WHERE eventId = :eventId")
    fun observe(eventId: String): Flow<RunSessionEntity?>

    @Upsert suspend fun upsert(session: RunSessionEntity)

    @Query("DELETE FROM run_sessions WHERE eventId = :eventId") suspend fun delete(eventId: String)

    @Query("DELETE FROM run_sessions WHERE epochDay < :epochDay") suspend fun deleteBefore(epochDay: Long)
}

@Dao
interface ChecklistDao {
    @Query("SELECT itemKey FROM checklist WHERE epochDay = :epochDay")
    fun observeDay(epochDay: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(item: ChecklistEntity)

    @Query("DELETE FROM checklist WHERE epochDay = :epochDay AND itemKey = :itemKey")
    suspend fun delete(epochDay: Long, itemKey: String)

    @Query("DELETE FROM checklist WHERE epochDay < :epochDay") suspend fun deleteBefore(epochDay: Long)

    @Query("SELECT * FROM checklist ORDER BY epochDay, itemKey") suspend fun all(): List<ChecklistEntity>

    @Query("DELETE FROM checklist") suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ChecklistEntity>)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_items ORDER BY dueEpochDay, duaId")
    fun observeAll(): Flow<List<ReviewItemEntity>>

    @Query("SELECT COUNT(*) FROM review_items WHERE dueEpochDay <= :today")
    fun observeDueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_items WHERE dueEpochDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT * FROM review_items WHERE dueEpochDay <= :today ORDER BY dueEpochDay, duaId")
    suspend fun due(today: Long): List<ReviewItemEntity>

    @Query("SELECT * FROM review_items WHERE duaId = :duaId")
    suspend fun item(duaId: String): ReviewItemEntity?

    @Query("SELECT * FROM review_items WHERE duaId = :duaId")
    fun observeItem(duaId: String): Flow<ReviewItemEntity?>

    @Upsert suspend fun upsert(item: ReviewItemEntity)

    @Query("DELETE FROM review_items WHERE duaId = :duaId") suspend fun delete(duaId: String)

    @Query("SELECT * FROM review_items ORDER BY duaId") suspend fun all(): List<ReviewItemEntity>

    @Query("DELETE FROM review_items") suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ReviewItemEntity>)
}
