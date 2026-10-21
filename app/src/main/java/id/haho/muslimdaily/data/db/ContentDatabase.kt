package id.haho.muslimdaily.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Read-only content, rebuilt from bundled assets whenever their checksum changes.
 * Not backed up (see res/xml/backup_rules.xml); user data lives in [UserDatabase].
 */
@Database(
    entities = [
        CategoryEntity::class, EventEntity::class, DuaEntity::class, DuaFtsEntity::class,
        RawatibEntity::class, FardEntity::class, CityEntity::class, CityFtsEntity::class, MetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun cityDao(): CityDao
}

@Entity(tableName = "categories")
data class CategoryEntity(@PrimaryKey val id: String, val label: String, val labelEn: String, val sortOrder: Int)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val label: String,
    val labelEn: String,
    val sortOrder: Int,
)

@Entity(tableName = "duas")
data class DuaEntity(
    @PrimaryKey val id: String,
    val category: String,
    val event: String,
    val sortOrder: Int,
    val variantGroup: String?,
    val arabic: String,
    val isQuran: Boolean,
    val translationId: String,
    val translationEn: String,
    val transliteration: String,
    val count: Int?,
    val sourcesJson: String,
    val gradingGrade: String?,
    val gradingGrader: String?,
    val altGradingsJson: String,
    val virtueText: String?,
    val virtueSource: String?,
    val audioFile: String?,
    val segmentsJson: String,
    val phraseBreaksJson: String,
    val status: String,
)

/** Search index. Text columns hold normalised text (see ArabicNormalizer). */
@Fts4(notIndexed = ["duaId"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "duas_fts")
data class DuaFtsEntity(
    val duaId: String,
    val arabic: String,
    val translationId: String,
    val translationEn: String,
    val transliteration: String,
    val eventLabel: String,
)

@Entity(tableName = "rawatib")
data class RawatibEntity(
    @PrimaryKey val id: String,
    val prayer: String?,
    val position: String,
    val rakah: Int?,
    val tier: String,
    val evidenceJson: String,
    val notes: String?,
    val hiddenWhenTravelling: Boolean,
    val variantGroup: String?,
    val variantLabel: String?,
    val defaultVariant: Boolean,
    val sortOrder: Int,
    val status: String,
)

@Entity(tableName = "fard")
data class FardEntity(@PrimaryKey val prayer: String, val rakah: Int, val status: String)

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val asciiName: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val population: Long,
)

@Fts4(notIndexed = ["cityId"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "cities_fts")
data class CityFtsEntity(val cityId: Long, val name: String, val region: String)

@Entity(tableName = "meta")
data class MetaEntity(@PrimaryKey val key: String, val value: String)

data class EventWithCount(
    val id: String,
    val categoryId: String,
    val label: String,
    val sortOrder: Int,
    val duaCount: Int,
)

@Dao
interface ContentDao {
    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: MetaEntity)

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun categories(): Flow<List<CategoryEntity>>

    @Query(
        """SELECT e.id, e.categoryId, e.label, e.sortOrder,
           (SELECT COUNT(*) FROM duas d WHERE d.event = e.id) AS duaCount
           FROM events e ORDER BY e.sortOrder""",
    )
    fun eventsWithCounts(): Flow<List<EventWithCount>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun event(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeEvent(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM duas WHERE event = :eventId ORDER BY sortOrder, id")
    fun duasForEvent(eventId: String): Flow<List<DuaEntity>>

    @Query("SELECT * FROM duas WHERE event = :eventId ORDER BY sortOrder, id")
    suspend fun duasForEventOnce(eventId: String): List<DuaEntity>

    @Query("SELECT * FROM duas WHERE id = :id")
    fun observeDua(id: String): Flow<DuaEntity?>

    @Query("SELECT * FROM duas WHERE id = :id")
    suspend fun dua(id: String): DuaEntity?

    @Query("SELECT * FROM duas WHERE id IN (:ids)")
    suspend fun duas(ids: List<String>): List<DuaEntity>

    @Query("SELECT * FROM duas ORDER BY category, event, sortOrder")
    fun allDuas(): Flow<List<DuaEntity>>

    @Query(
        """SELECT d.* FROM duas d JOIN duas_fts f ON d.id = f.duaId
           WHERE duas_fts MATCH :query LIMIT 100""",
    )
    suspend fun search(query: String): List<DuaEntity>

    @Query("SELECT * FROM rawatib ORDER BY sortOrder")
    fun rawatib(): Flow<List<RawatibEntity>>

    @Query("SELECT * FROM fard")
    fun fard(): Flow<List<FardEntity>>

    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM events") suspend fun clearEvents()
    @Query("DELETE FROM duas") suspend fun clearDuas()
    @Query("DELETE FROM duas_fts") suspend fun clearDuasFts()
    @Query("DELETE FROM rawatib") suspend fun clearRawatib()
    @Query("DELETE FROM fard") suspend fun clearFard()

    @Insert suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert suspend fun insertEvents(items: List<EventEntity>)
    @Insert suspend fun insertDuas(items: List<DuaEntity>)
    @Insert suspend fun insertDuasFts(items: List<DuaFtsEntity>)
    @Insert suspend fun insertRawatib(items: List<RawatibEntity>)
    @Insert suspend fun insertFard(items: List<FardEntity>)

    @Transaction
    suspend fun replaceAll(
        categories: List<CategoryEntity>,
        events: List<EventEntity>,
        duas: List<DuaEntity>,
        fts: List<DuaFtsEntity>,
        rawatib: List<RawatibEntity>,
        fard: List<FardEntity>,
        meta: MetaEntity,
    ) {
        clearCategories(); clearEvents(); clearDuas(); clearDuasFts(); clearRawatib(); clearFard()
        insertCategories(categories)
        insertEvents(events)
        insertDuas(duas)
        insertDuasFts(fts)
        insertRawatib(rawatib)
        insertFard(fard)
        putMeta(meta)
    }
}

@Dao
interface CityDao {
    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int

    @Insert suspend fun insertCities(items: List<CityEntity>)
    @Insert suspend fun insertFts(items: List<CityFtsEntity>)

    @Query(
        """SELECT c.* FROM cities c JOIN cities_fts f ON c.id = f.cityId
           WHERE cities_fts MATCH :query ORDER BY (c.country = :preferredCountry) DESC, c.population DESC LIMIT 50""",
    )
    suspend fun search(query: String, preferredCountry: String): List<CityEntity>

    @Query(
        """SELECT * FROM cities WHERE latitude BETWEEN :minLat AND :maxLat
           AND longitude BETWEEN :minLon AND :maxLon ORDER BY population DESC LIMIT 200""",
    )
    suspend fun near(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<CityEntity>
}
