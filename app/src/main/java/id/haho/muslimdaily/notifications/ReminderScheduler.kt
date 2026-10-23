package id.haho.muslimdaily.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one alarm per reminder type pointing at its next occurrence. Uses exact alarms when the
 * user allows them; otherwise falls back to inexact alarms that may arrive a few minutes late.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val calculator: PrayerCalculator,
    private val clock: Clock,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    suspend fun rescheduleAll() = mutex.withLock {
        val planned = ReminderPlanner.plan(Instant.now(clock), settings.current(), calculator)
        val byType = planned.associateBy { it.type }
        ReminderType.entries.forEach { type ->
            val next = byType[type]
            val pending = pendingIntent(type, next?.silent ?: false, next?.at?.toEpochMilli() ?: 0L)
            if (next == null) {
                alarms.cancel(pending)
            } else {
                schedule(next.at.toEpochMilli(), pending)
            }
        }
        ensureSafetyNet(planned.isNotEmpty())
    }

    private fun schedule(atMillis: Long, pending: PendingIntent) {
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            // An inexact one-shot alarm may be deferred by up to 75% of the time until it is due
            // (hours, for an alarm set in the morning); a window bounds that to a few minutes.
            alarms.setWindow(AlarmManager.RTC_WAKEUP, atMillis, INEXACT_WINDOW_MS, pending)
        }
    }

    private fun pendingIntent(type: ReminderType, silent: Boolean, dueMillis: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            type.requestCode,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_REMINDER)
                .putExtra(EXTRA_TYPE, type.name)
                .putExtra(EXTRA_SILENT, silent)
                .putExtra(EXTRA_DUE, dueMillis),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** A periodic re-check covers missed broadcasts and OEM alarm clean-ups. */
    private fun ensureSafetyNet(active: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (active) {
            wm.enqueueUniquePeriodicWork(
                RescheduleWorker.NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RescheduleWorker>(12, TimeUnit.HOURS).build(),
            )
        } else {
            wm.cancelUniqueWork(RescheduleWorker.NAME)
        }
    }

    companion object {
        /** The shortest window apps without the exact-alarm permission may ask for. */
        private const val INEXACT_WINDOW_MS = 10 * 60 * 1000L
        const val ACTION_REMINDER = "id.haho.muslimdaily.action.REMINDER"
        const val EXTRA_TYPE = "type"
        const val EXTRA_SILENT = "silent"
        const val EXTRA_DUE = "due"
    }
}
