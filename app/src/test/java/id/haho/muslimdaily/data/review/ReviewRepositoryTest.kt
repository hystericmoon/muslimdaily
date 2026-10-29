package id.haho.muslimdaily.data.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.ReviewItemEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.domain.memorize.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ReviewRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
    private val pair = listOf("morning_02", "evening_02")
    private val repo = ReviewRepository(db) { pair.associateWith { pair } }
    private val today = LocalDate.of(2026, 10, 27)

    @After fun close() = db.close()

    @Test
    fun `a dua saved to learn later is listed but not due until its first rating`() = runTest {
        repo.add("wake_01", today)
        val item = repo.items().first().single()
        assertTrue(item.notStarted)
        assertEquals(0, repo.dueCountOnce(today))
        assertEquals(emptyList(), repo.due(today))

        repo.rate("wake_01", Rating.HARD, today)
        assertTrue(!repo.items().first().single().notStarted)
        assertEquals(1, repo.dueCountOnce(today.plusDays(1)))
    }

    @Test
    fun `saving again leaves a dua being reviewed as it is`() = runTest {
        repo.rate("wake_01", Rating.EASY, today)
        repo.add("wake_01", today.plusDays(1))
        val item = repo.items().first().single()
        assertEquals(2, item.state.box)
        assertEquals(today, item.lastReviewed)
    }

    @Test
    fun `twins share one review, whichever of them is rated`() = runTest {
        repo.add("evening_02", today)
        repo.rate("evening_02", Rating.EASY, today)
        val item = repo.items().first().single()
        assertEquals("morning_02", item.duaId)
        assertEquals(2, item.state.box)
        assertEquals(item, repo.item("evening_02").first())
        assertEquals(setOf("morning_02", "evening_02", "wake_01"), repo.withTwins(setOf("morning_02", "wake_01")))

        repo.remove("evening_02")
        assertTrue(repo.items().first().isEmpty())
    }

    @Test
    fun `rows saved separately are folded into the one reviewed last`() = runTest {
        val day = today.toEpochDay()
        db.reviewDao().insertAll(
            listOf(
                ReviewItemEntity("morning_02", box = 4, dueEpochDay = day + 7, lastReviewedEpochDay = day - 7, addedEpochDay = day - 30),
                ReviewItemEntity("evening_02", box = 2, dueEpochDay = day + 1, lastReviewedEpochDay = day - 1, addedEpochDay = day - 10),
                ReviewItemEntity("wake_01", box = 3, dueEpochDay = day, lastReviewedEpochDay = day - 3, addedEpochDay = day - 20),
            ),
        )
        val items = repo.items().first().associateBy { it.duaId }
        assertEquals(setOf("morning_02", "wake_01"), items.keys)
        assertEquals(2, items.getValue("morning_02").state.box)
        assertEquals(today.minusDays(1), items.getValue("morning_02").lastReviewed)
        assertEquals(day - 30, db.reviewDao().item("morning_02")?.addedEpochDay)
        assertEquals(3, items.getValue("wake_01").state.box)
    }
}
