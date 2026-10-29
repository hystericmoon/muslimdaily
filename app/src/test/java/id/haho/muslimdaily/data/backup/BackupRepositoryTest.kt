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
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
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
    private val settings = SettingsRepository(context)
    private val repo = BackupRepository(settings, MySetRepository(db), db, Clock.systemUTC(), ReviewRepository(db) { emptyMap() })

    @After fun close() = db.close()

    @Test
    fun `a backup restores settings, sets, reviews and ticks after everything is wiped`() = runTest {
        settings.update {
            palette = HahoPalette.LAVENDER
            remFasting = true
            setFastingTime(LocalTime.of(21, 15))
            arabicScale = 1.3f
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
        assertEquals(true, db.mySetDao().allPrefs().single().useMySet)
        assertEquals(3, db.reviewDao().item("wake_01")!!.box)
        assertEquals(listOf(ChecklistEntity(20_000, "fard:FAJR")), db.checklistDao().all())
        assertEquals(1, summary.reviews)
    }

    @Test
    fun `other files are refused`() = runTest {
        assertFailsWith<IllegalArgumentException> { repo.restore("""{"format":"muslimdaily.mysets","events":[]}""") }
        assertFailsWith<IllegalArgumentException> { repo.restore("hello") }
    }
}
