package id.haho.muslimdaily.data.routine

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.UserDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
    private val repo = RoutineRepository(db, Clock.systemUTC())

    @After fun close() = db.close()

    @Test
    fun `a dua added from its page goes at the end, once, and can be taken out`() = runTest {
        val id = repo.save(null, "Pagi kerja", listOf("a", "b"))
        repo.setIncluded(id, "c", true)
        repo.setIncluded(id, "c", true)
        assertEquals(listOf("a", "b", "c"), repo.routine(id)!!.duaIds)
        assertEquals("Pagi kerja", repo.routine(id)!!.name)

        repo.setIncluded(id, "a", false)
        assertEquals(listOf("b", "c"), repo.routine(id)!!.duaIds)
    }
}
