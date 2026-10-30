package id.haho.muslimdaily.data.myset

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.content.AssetContentSource
import id.haho.muslimdaily.data.content.ContentImporter
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.db.ContentDatabase
import id.haho.muslimdaily.data.db.EventItemEntity
import id.haho.muslimdaily.data.db.EventPrefEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.domain.myset.FormChoice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SetsToRoutinesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
    private val contentDb = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java).allowMainThreadQueries().build()
    private val content = ContentRepository(contentDb, ContentImporter(AssetContentSource(context), contentDb))
    private val mySets = MySetRepository(db)
    private val routines = RoutineRepository(db, Clock.systemUTC())
    private val convert = SetsToRoutines(db, content, mySets, routines)

    @After fun close() {
        db.close()
        contentDb.close()
    }

    private suspend fun keys(event: String) = content.duasForEventOnce(event).sortedBy { it.order }.map { it.id }

    @Test
    fun `an occasion read as its own set becomes a routine in that order`() = runTest {
        val all = keys("evening")
        // The last dua first, the second hidden.
        val order = listOf(all.last()) + all.dropLast(1)
        mySets.saveItems("evening", order.mapIndexed { i, k -> id.haho.muslimdaily.domain.myset.CustomEntry(k, k != all[1], i) })

        assertEquals(1, convert.run())

        val routine = routines.routines().first().single()
        assertEquals(content.eventOnce("evening")!!.label, routine.name)
        assertEquals(all.last(), routine.duaIds.first())
        assertTrue(all[1] !in routine.duaIds)
        assertEquals(all.size - 1, routine.duaIds.size)
        assertEquals(emptyList(), db.mySetDao().allItems())
        assertEquals(emptyList(), db.mySetDao().allPrefs())
        // Running again changes nothing.
        assertEquals(0, convert.run())
        assertEquals(1, routines.routines().first().size)
    }

    @Test
    fun `a set switched off or the same as the full one is cleared without a routine, and form choices stay`() = runTest {
        val all = keys("morning")
        db.mySetDao().upsertPref(EventPrefEntity("morning", false))
        db.mySetDao().insertItems(listOf(EventItemEntity("morning", all.first(), false, 0)))
        mySets.saveItems("wake_up_unused", emptyList())
        mySets.setChoice("evening", "g", FormChoice.Fixed("x"))
        mySets.setUseMySet("evening", true)

        assertEquals(0, convert.run())

        assertEquals(emptyList(), routines.routines().first())
        assertEquals(emptyList(), db.mySetDao().allItems())
        assertEquals(emptyList(), db.mySetDao().allPrefs())
        assertEquals(1, db.mySetDao().allChoices().size)
    }
}
