package id.haho.muslimdaily.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.content.AssetContentSource
import id.haho.muslimdaily.data.content.ContentImporter
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.location.CityRepository
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.feature.today.TodayStateBuilder
import id.haho.muslimdaily.feature.today.TodayUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LocationFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).allowMainThreadQueries().build()

    @Test
    fun pick_city_then_build_today() = runBlocking {
        val cities = CityRepository(context, db)
        val popular = cities.popular()
        assertTrue(popular.isNotEmpty())
        val found = cities.search("jakarta")
        assertTrue(found.isNotEmpty(), "search")
        cities.nearest(-6.2, 106.8)
        val city = found.first()
        val content = ContentRepository(db, ContentImporter(AssetContentSource(context), db))
        val settings = AppSettings(location = SavedLocation(city.name, city.latitude, city.longitude, city.timeZone, LocationSource.MANUAL, 0))
        val state = TodayStateBuilder.build(Instant.now(), settings, content.rawatib().first(), content.fard().first(), emptySet(), PrayerCalculator())
        assertTrue(state is TodayUiState.Ready)
    }

    @Test
    fun a_cancelled_search_does_not_roll_back_the_city_import() = runBlocking {
        val cities = CityRepository(context, db)
        // The picker cancels a search on every keystroke, often while the first one imports.
        val first = launch(Dispatchers.Default) { cities.popular() }
        delay(200)
        first.cancelAndJoin()
        val rows = context.assets.open("cities.tsv").bufferedReader().useLines { lines -> lines.count { it.split('\t').size >= 9 } }
        assertEquals(rows, db.cityDao().count())
    }
}
