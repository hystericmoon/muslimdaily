package id.haho.muslimdaily.core.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DiagnosticsLogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clean() {
        CrashLog.clear(context)
        context.getSharedPreferences("reminder_log", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("widget_log", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `widget log keeps every prayer-time redraw but only late minute ticks`() {
        val due = Instant.parse("2026-10-30T04:40:00Z")
        assertNull(WidgetLog.lastDrawn(context))
        WidgetLog.tick(context, due, due.plusSeconds(90))
        WidgetLog.tick(context, due, due.plusSeconds(79 * 60))
        WidgetLog.prayer(context, due, due.plusSeconds(2))
        WidgetLog.drawn(context, due.plusSeconds(3))
        val entries = WidgetLog.entries(context)
        assertEquals(listOf(WidgetLog.Kind.PRAYER, WidgetLog.Kind.TICK), entries.map { it.kind })
        assertEquals(Duration.ofMinutes(79), entries.last().late)
        assertEquals(due.plusSeconds(3), WidgetLog.lastDrawn(context))
    }

    @Test
    fun `reminder log keeps the newest entries first, with lateness`() {
        val due = Instant.parse("2026-10-23T04:13:00Z")
        repeat(35) { i ->
            ReminderLog.record(context, ReminderLog.Entry("ADHAN_FAJR", due.plusSeconds(i * 86_400L), due.plusSeconds(i * 86_400L + 180), exact = false))
        }
        val entries = ReminderLog.entries(context)
        assertEquals(30, entries.size)
        assertEquals(due.plusSeconds(34 * 86_400L), entries.first().due)
        assertEquals(Duration.ofMinutes(3), entries.first().late)
    }

    @Test
    fun `a crash is written once, offered once, and the oldest are dropped`() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> } // keep the test runner alive
        try {
            CrashLog.install(context)
            val handler = Thread.getDefaultUncaughtExceptionHandler()!!
            repeat(7) { handler.uncaughtException(Thread.currentThread(), IllegalStateException("boom $it")); Thread.sleep(2) }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        val files = CrashLog.files(context)
        assertEquals(5, files.size)
        val text = files.first().readText()
        assertTrue("boom 6" in text && "Adwam" in text)
        assertNotNull(CrashLog.unseen(context))
        CrashLog.markSeen(context)
        assertNull(CrashLog.unseen(context))
    }
}
