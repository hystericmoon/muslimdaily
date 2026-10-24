package id.haho.muslimdaily.feature.whatsnew

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.note.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class WhatsNewTest {
    @Test
    fun `nothing on a fresh install`() = assertTrue(WhatsNew.since("", "0.6.0", firstRun = true).isEmpty())

    @Test
    fun `after an update, the releases since the last one seen`() {
        assertEquals(listOf("0.6.0"), WhatsNew.since("0.5.3", "0.6.0", firstRun = false).map { it.version })
        assertEquals(listOf("0.6.0", "0.5.3", "0.5.2"), WhatsNew.since("0.5.0", "0.6.0", firstRun = false).map { it.version })
        assertTrue(WhatsNew.since("0.6.0", "0.6.0", firstRun = false).isEmpty())
        // An existing user from before this screen existed sees the latest highlights.
        assertEquals("0.6.0", WhatsNew.since("", "0.6.0", firstRun = false).first().version)
    }

    @Test
    fun `a final release sums up its betas`() {
        // From the last stable release, or from a beta, a final 0.7.0 shows its own highlights only.
        assertEquals(listOf("0.7.0"), WhatsNew.since("0.6.0", "0.7.0", firstRun = false).map { it.version })
        assertEquals(listOf("0.7.0"), WhatsNew.since("0.7.0-beta.4", "0.7.0", firstRun = false).map { it.version })
        // A beta still lists the betas before it.
        assertEquals(listOf("0.7.0-beta.4", "0.7.0-beta.3"), WhatsNew.since("0.7.0-beta.2", "0.7.0-beta.4", firstRun = false).map { it.version })
    }

    @Test
    fun `releases are listed newest first`() {
        val versions = WhatsNew.releases.map { it.version }
        assertEquals(versions, versions.sortedWith { a, b -> if (id.haho.muslimdaily.update.Versions.isNewer(a, b)) -1 else 1 })
    }

    @Test
    fun `notes save, trim, and delete when blank`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java).allowMainThreadQueries().build()
        val notes = NoteRepository(db, Clock.systemUTC())
        notes.save("wake_01", "  Dibaca bersama anak-anak  ")
        assertEquals("Dibaca bersama anak-anak", notes.note("wake_01").first())
        assertEquals(setOf("wake_01"), notes.notedIds().first())
        notes.save("wake_01", "   ")
        assertNull(notes.note("wake_01").first())
        db.close()
    }
}
