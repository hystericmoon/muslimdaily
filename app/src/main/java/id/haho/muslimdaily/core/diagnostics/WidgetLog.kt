package id.haho.muslimdaily.core.diagnostics

import android.content.Context
import androidx.core.content.edit
import java.time.Duration
import java.time.Instant

/**
 * When the prayer-times widget last redrew, and the redraws that came late. "The countdown is
 * wrong" then becomes checkable: the prayer-time redraw never came or came late (the OEM stopped
 * the app), or only the minute tick stalled (Android's standby limits on a non-waking alarm).
 */
object WidgetLog {
    private const val PREFS = "widget_log"
    private const val KEY_DRAWN = "drawn"
    private const val KEY_ENTRIES = "entries"
    private const val KEEP = 30

    /** A minute tick this much later than due is logged; shorter ones are the alarm's normal slack. */
    val TICK_GAP: Duration = Duration.ofMinutes(3)

    enum class Kind {
        /** The redraw at a prayer time (a waking alarm): late or missing means the app was stopped. */
        PRAYER,

        /** The minute tick, which waits while the screen is off: logged only when late. */
        TICK,
    }

    data class Entry(val kind: Kind, val due: Instant, val fired: Instant) {
        val late: Duration get() = Duration.between(due, fired)
        fun encode() = "${kind.name}|${due.toEpochMilli()}|${fired.toEpochMilli()}"

        companion object {
            fun decode(line: String): Entry? = runCatching {
                val (k, d, f) = line.split('|')
                Entry(Kind.valueOf(k), Instant.ofEpochMilli(d.toLong()), Instant.ofEpochMilli(f.toLong()))
            }.getOrNull()
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The redraw at a prayer time fired; [due] is when it was set for. */
    fun prayer(context: Context, due: Instant, fired: Instant) = record(context, Entry(Kind.PRAYER, due, fired))

    /** A minute tick fired; logged when it came more than [TICK_GAP] after the minute it was set for. */
    fun tick(context: Context, due: Instant, fired: Instant) {
        if (Duration.between(due, fired) > TICK_GAP) record(context, Entry(Kind.TICK, due, fired))
    }

    /** The widgets were redrawn at [at]. */
    fun drawn(context: Context, at: Instant) = prefs(context).edit { putLong(KEY_DRAWN, at.toEpochMilli()) }

    fun lastDrawn(context: Context): Instant? = prefs(context).getLong(KEY_DRAWN, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    @Synchronized
    private fun record(context: Context, entry: Entry) {
        val prefs = prefs(context)
        val lines = listOf(entry.encode()) + prefs.getString(KEY_ENTRIES, "").orEmpty().lines().filter { it.isNotBlank() }
        prefs.edit { putString(KEY_ENTRIES, lines.take(KEEP).joinToString("\n")) }
    }

    /** Newest first. */
    fun entries(context: Context): List<Entry> =
        prefs(context).getString(KEY_ENTRIES, "").orEmpty().lines().mapNotNull { Entry.decode(it) }
}
