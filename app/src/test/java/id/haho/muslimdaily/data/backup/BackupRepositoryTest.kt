package id.haho.muslimdaily.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.design.theme.HahoPalette
import id.haho.muslimdaily.data.db.ChecklistEntity
import id.haho.muslimdaily.data.db.EventItemEntity
import id.haho.muslimdaily.data.db.EventPrefEntity
import id.haho.muslimdaily.data.db.ReviewItemEntity
import id.haho.muslimdaily.data.db.SavedPlaceEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.content.AssetContentSource
import id.haho.muslimdaily.data.content.ContentImporter
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.myset.SetsToRoutines
import id.haho.muslimdaily.data.routine.RoutineRepository
import kotlinx.coroutines.flow.first
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
    private val contentDb = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).allowMainThreadQueries().build()
    private val content = ContentRepository(contentDb, ContentImporter(AssetContentSource(context), contentDb))
    private val settings = SettingsRepository(context)
    private val routines = RoutineRepository(db, Clock.systemUTC())
    private val repo = BackupRepository(
        settings, MySetRepository(db), db, Clock.systemUTC(), ReviewRepository(db, { emptyMap() }, { emptySet() }),
        SetsToRoutines(db, content, MySetRepository(db), routines),
    )

    @After fun close() {
        db.close()
        contentDb.close()
    }

    @Test
    fun `a backup restores settings, sets, reviews and ticks after everything is wiped`() = runTest {
        settings.update {
            palette = HahoPalette.LAVENDER
            remFasting = true
            setFastingTime(LocalTime.of(21, 15))
            arabicScale = 1.3f
            setAdvanced("wake_01", true)
            setAdvanced("sleep_01", true)
            setAdvanced("sleep_01", false)
        }
        db.mySetDao().upsertPref(EventPrefEntity("morning", true))
        db.mySetDao().insertItems(listOf(EventItemEntity("morning", "morning_01", false, 0)))
        db.reviewDao().upsert(ReviewItemEntity("wake_01", 3, 20_000, 19_990, 19_900))
        db.checklistDao().insert(ChecklistEntity(20_000, "fard:FAJR"))

        val text = repo.export()

        settings.restore(emptyMap())
        db.clearAllTables()
        assertEquals(HahoPalette.TEAL, settings.current().palette)

        val summary = repo.restore(text)
        val s = settings.current()
        assertEquals(HahoPalette.LAVENDER, s.palette)
        assertEquals(true, s.reminders.fasting)
        assertEquals(LocalTime.of(21, 15), s.reminders.fastingTime)
        assertEquals(1.3f, s.arabicScale)
        // The per-dua Advanced switch is a setting, so it travels with them.
        assertEquals(setOf("wake_01"), s.memorize.advancedDuas)
        // The old backup's morning set (first dua hidden) comes back as a routine.
        val routine = routines.routines().first().single()
        assertEquals(content.eventOnce("morning")!!.label, routine.name)
        assertEquals(false, "morning_01" in routine.duaIds)
        assertEquals(true, "morning_02" in routine.duaIds)
        assertEquals(emptyList(), db.mySetDao().allPrefs())
        assertEquals(3, db.reviewDao().item("wake_01")!!.box)
        assertEquals(listOf(ChecklistEntity(20_000, "fard:FAJR")), db.checklistDao().all())
        assertEquals(1, summary.reviews)
    }

    @Test
    fun `saved places and the dhikr counter come back after a wipe`() = runTest {
        val place = SavedPlaceEntity("p1", "HOME", "", "Bandung", -6.9175, 107.6191, "Asia/Jakarta", 768, 0, 5L)
        db.placeDao().upsert(place)
        db.placeDao().upsert(SavedPlaceEntity("p2", "OTHER", "Rumah ibu", "Garut", -7.2, 107.9, "Asia/Jakarta", 700, 1, 6L))
        settings.update { dhikrCount = 17; dhikrTarget = 33 }

        val text = repo.export()
        settings.restore(emptyMap())
        db.clearAllTables()
        repo.restore(text)

        assertEquals(listOf(place.id, "p2"), db.placeDao().all().map { it.id })
        assertEquals(place, db.placeDao().place("p1"))
        assertEquals(17, settings.current().dhikrCount)
        assertEquals(33, settings.current().dhikrTarget)
    }

    @Test
    fun `a version 3 backup, made before saved places, still restores`() = runTest {
        db.placeDao().upsert(SavedPlaceEntity("p1", "WORK", "", "Jakarta", -6.2, 106.8, "Asia/Jakarta", 8, 0, 1L))
        // A file as version 3 wrote it: no "places" key at all.
        val v3 = buildJsonObject {
            put("format", BackupRepository.FORMAT)
            put("version", 3)
            put("createdAt", "2026-11-19T00:00:00Z")
            put("appVersion", "1.22.0")
            put("settings", buildJsonObject {})
            put("mySets", MySetRepository(db).exportJson())
            put("reviews", JsonArray(emptyList()))
            put("checklist", JsonArray(emptyList()))
        }.toString()
        repo.restore(v3)
        // A backup from before places holds none, so a restore replaces them with none.
        assertEquals(emptyList(), db.placeDao().all())
    }

    @Test
    fun `other files are refused`() = runTest {
        assertFailsWith<IllegalArgumentException> { repo.restore("""{"format":"muslimdaily.mysets","events":[]}""") }
        assertFailsWith<IllegalArgumentException> { repo.restore("hello") }
    }
}
