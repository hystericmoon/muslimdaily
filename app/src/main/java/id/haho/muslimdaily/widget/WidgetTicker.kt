package id.haho.muslimdaily.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import id.haho.muslimdaily.core.diagnostics.WidgetLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Redraws the widget each minute so its countdown ("7 jam 6 menit") stays true. The alarm does
 * not wake the phone: while the screen is off ticks wait, and the first one after waking redraws.
 * Exact minute ticks need the exact-alarm permission; without it Android stretches the alarm to a
 * ten-minute window, so the widget ticks about every ten minutes and rounds its countdown ("±").
 * Stops once no widget is placed.
 */
object WidgetTicker {
    private const val MINUTE_MS = 60_000L
    private const val COARSE_MS = 10 * MINUTE_MS

    fun precise(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        if (!isWidgetPlaced(context)) {
            alarms.cancel(pending)
            return
        }
        val now = System.currentTimeMillis()
        if (precise(context)) {
            val at = (now / MINUTE_MS + 1) * MINUTE_MS
            alarms.setExact(AlarmManager.RTC, at, pendingIntent(context, due = at))
        } else {
            // Due at the end of the window, so the diagnostics count only lateness beyond it.
            alarms.setWindow(AlarmManager.RTC, now + MINUTE_MS, COARSE_MS, pendingIntent(context, due = now + MINUTE_MS + COARSE_MS))
        }
    }

    private fun pendingIntent(context: Context, due: Long = 0L): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WidgetTickReceiver::class.java).putExtra(EXTRA_DUE, due),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val REQUEST_CODE = 9100
    internal const val EXTRA_DUE = "due"
}

private val tickScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

@AndroidEntryPoint
class WidgetTickReceiver : BroadcastReceiver() {
    @Inject lateinit var glance: GlanceRefresher

    override fun onReceive(context: Context, intent: Intent) {
        val due = intent.getLongExtra(WidgetTicker.EXTRA_DUE, 0L)
        if (due > 0) WidgetLog.tick(context, Instant.ofEpochMilli(due), Instant.now())
        val pending = goAsync()
        tickScope.launch {
            try {
                glance.refreshWidgets()
                WidgetTicker.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
