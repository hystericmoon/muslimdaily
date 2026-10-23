package id.haho.muslimdaily.core.diagnostics

import android.content.Context
import androidx.core.content.edit
import java.time.Duration
import java.time.Instant

/**
 * The last reminders that actually fired, with when they were due. "The adhan didn't ring" then
 * becomes something checkable: never fired (killed by the OEM), fired late (no exact alarms,
 * Doze), or fired on time (the notification itself was blocked or muted).
 */
object ReminderLog {
    private const val PREFS = "reminder_log"
    private const val KEY = "entries"
    private const val KEEP = 30

    data class Entry(val type: String, val due: Instant, val fired: Instant, val exact: Boolean) {
        val late: Duration get() = Duration.between(due, fired)
        fun encode() = "$type|${due.toEpochMilli()}|${fired.toEpochMilli()}|$exact"

        companion object {
            fun decode(line: String): Entry? = runCatching {
                val (t, d, f, e) = line.split('|')
                Entry(t, Instant.ofEpochMilli(d.toLong()), Instant.ofEpochMilli(f.toLong()), e.toBooleanStrict())
            }.getOrNull()
        }
    }

    @Synchronized
    fun record(context: Context, entry: Entry) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lines = listOf(entry.encode()) + prefs.getString(KEY, "").orEmpty().lines().filter { it.isNotBlank() }
        prefs.edit { putString(KEY, lines.take(KEEP).joinToString("\n")) }
    }

    /** Newest first. */
    fun entries(context: Context): List<Entry> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
            .lines().mapNotNull { Entry.decode(it) }
}
