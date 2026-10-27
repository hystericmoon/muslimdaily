package id.haho.muslimdaily.data.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    private val repo = ReviewRepository(db)
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
}
