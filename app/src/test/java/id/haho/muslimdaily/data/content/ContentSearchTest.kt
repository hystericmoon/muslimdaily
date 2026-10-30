package id.haho.muslimdaily.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.ContentDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Search over the bundled content: ranking, the source index, and re-importing. */
@RunWith(AndroidJUnit4::class)
class ContentSearchTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).allowMainThreadQueries().build()
    private val importer = ContentImporter(AssetContentSource(context), db)
    private val content = ContentRepository(db, importer)

    @Test
    fun occasion_name_ranks_first() = runBlocking {
        val results = content.search("dzikir pagi")
        assertTrue(results.isNotEmpty())
        val morning = results.takeWhile { it.event == "morning" }
        assertTrue(morning.isNotEmpty(), "the morning adhkar lead")
        assertEquals(results.count { it.event == "morning" }, morning.size, "and are not interleaved with others")
    }

    @Test
    fun finds_by_source_book() = runBlocking {
        val results = content.search("bukhari")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { d -> d.sources.any { it.book.contains("Bukhari", ignoreCase = true) } }, "a dua narrated by al-Bukhari")
    }

    @Test
    fun no_duplicates_across_tiers() = runBlocking {
        val ids = content.search("allah").map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun importing_twice_keeps_one_copy() = runBlocking {
        importer.ensureImported()
        val first = db.contentDao().allDuas().first().size
        importer.ensureImported()
        assertEquals(first, db.contentDao().allDuas().first().size)
    }
}
