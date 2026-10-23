package id.haho.muslimdaily.feature.routines

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class RoutineTest {
    private val a = DuaPreviewData.dua("a", 1)
    private val b = DuaPreviewData.dua("b", 2)
    private val c = DuaPreviewData.dua("c", 3)

    @Test
    fun `toggle adds at the end and removes`() {
        val list = RoutineEdits.toggle(RoutineEdits.toggle(listOf(a), c), b)
        assertEquals(listOf("a", "c", "b"), list.map { it.id })
        assertEquals(listOf("a", "b"), RoutineEdits.toggle(list, c).map { it.id })
    }

    @Test
    fun `move and reorder keep every dua once`() {
        val list = listOf(a, b, c)
        assertEquals(listOf("b", "a", "c"), RoutineEdits.move(list, "a", 1).map { it.id })
        assertEquals(list, RoutineEdits.move(list, "a", -1))
        assertEquals(listOf("c", "a", "b"), RoutineEdits.reorder(list, listOf("c", "a", "b")).map { it.id })
    }

    @Test
    fun `run key round-trips and occasions are not routines`() {
        assertEquals("x1", Routine.idFromRunKey(Routine("x1", "n", emptyList()).runKey))
        assertNull(Routine.idFromRunKey("morning"))
    }

    @Test
    fun `repository saves order, renames and deletes`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
        val repo = RoutineRepository(db, Clock.systemUTC())
        val id = repo.save(null, "  Pagi kerja ", listOf("b", "a"))
        assertEquals(Routine(id, "Pagi kerja", listOf("b", "a")), repo.routine(id))
        repo.save(id, "Kerja", listOf("a", "c", "b"))
        assertEquals(listOf(Routine(id, "Kerja", listOf("a", "c", "b"))), repo.routines().first())
        repo.delete(id)
        assertEquals(emptyList(), repo.routines().first())
        db.close()
    }

    /**
     * Builds a real version-1 database from the exported schema, then opens it with the current
     * Room setup, which runs the auto-migration and validates the result against version 2.
     */
    @Test
    fun `version 1 user database migrates to 2 with its data`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migrate.db").apply { parentFile?.mkdirs(); delete() }
        val schema = org.json.JSONObject(java.io.File("schemas/id.haho.muslimdaily.data.db.UserDatabase/1.json").readText()).getJSONObject("database")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO checklist (epochDay, itemKey) VALUES (20000, 'fard:FAJR')")
            db.version = 1
        }
        val room = Room.databaseBuilder(context, UserDatabase::class.java, "migrate.db").allowMainThreadQueries().build()
        assertEquals(1, room.checklistDao().all().size)
        val repo = RoutineRepository(room, Clock.systemUTC())
        val id = repo.save(null, "Setelah migrasi", listOf("a"))
        assertEquals(listOf("a"), repo.routine(id)?.duaIds)
        room.close()
    }
}
