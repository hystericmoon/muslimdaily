package id.haho.muslimdaily.data.place

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PlaceRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
    private val settings = SettingsRepository(context)
    private val repo = PlaceRepository(db, settings, Clock.systemUTC())

    private fun at(name: String, lat: Double, lon: Double) = SavedLocation(name, lat, lon, "Asia/Jakarta", LocationSource.GPS, 0L, 100)

    @Before fun clean() = runTest { settings.restore(emptyMap()) }

    @After fun close() = db.close()

    @Test
    fun `home and work hold one place each, other places many`() = runTest {
        settings.update { setLocation(at("Bandung", -6.9, 107.6)) }
        assertTrue(repo.saveCurrent(PlaceKind.HOME))
        settings.update { setLocation(at("Jakarta", -6.2, 106.8)) }
        assertTrue(repo.saveCurrent(PlaceKind.HOME))
        assertTrue(repo.saveCurrent(PlaceKind.WORK))
        assertTrue(repo.saveCurrent(PlaceKind.OTHER, "Rumah ibu"))
        assertTrue(repo.saveCurrent(PlaceKind.OTHER, "Kos"))

        val all = db.placeDao().all()
        assertEquals(4, all.size)
        assertEquals("Jakarta", all.single { it.kind == "HOME" }.cityName)
        assertEquals(listOf("Rumah ibu", "Kos"), all.filter { it.kind == "OTHER" }.map { it.label })
    }

    @Test
    fun `an unnamed other place and a missing location save nothing`() = runTest {
        assertFalse(repo.saveCurrent(PlaceKind.HOME))
        settings.update { setLocation(at("Bandung", -6.9, 107.6)) }
        assertFalse(repo.saveCurrent(PlaceKind.OTHER, "  "))
        assertEquals(emptyList(), db.placeDao().all())
    }

    @Test
    fun `switching sets the current location, and only that place is active`() = runTest {
        settings.update { setLocation(at("Bandung", -6.9, 107.6)) }
        repo.saveCurrent(PlaceKind.HOME)
        settings.update { setLocation(at("Jakarta", -6.2, 106.8)) }
        repo.saveCurrent(PlaceKind.WORK)
        val home = db.placeDao().all().single { it.kind == "HOME" }
        val work = db.placeDao().all().single { it.kind == "WORK" }

        repo.switchTo(home.id)
        val now = assertNotNull(settings.current().location)
        assertEquals("Bandung", now.name)
        assertEquals(100, now.elevation)
        assertEquals(LocationSource.MANUAL, now.source)
        assertTrue(PlaceRepository.isActive(home, now))
        assertFalse(PlaceRepository.isActive(work, now))
    }
}
