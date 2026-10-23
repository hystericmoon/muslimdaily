package id.haho.muslimdaily.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.bidiIsolate
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import id.haho.muslimdaily.domain.fasting.FastingCalendar
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.feature.fasting.labelRes
import id.haho.muslimdaily.widget.GlanceRefresher
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Fires a reminder, then points the alarm at the next occurrence. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var reviews: ReviewRepository
    @Inject lateinit var calculator: PrayerCalculator
    @Inject lateinit var clock: Clock
    @Inject lateinit var glance: GlanceRefresher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE)?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() } ?: return
        val silent = intent.getBooleanExtra(ReminderScheduler.EXTRA_SILENT, false)
        val pending = goAsync()
        receiverScope.launch {
            try {
                deliver(context, type, silent)
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun deliver(context: Context, type: ReminderType, silent: Boolean) {
        val s = settings.current()
        val location = s.location
        val zone = location?.timeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val now = Instant.now(clock)
        val today = now.atZone(zone).toLocalDate()
        if (type == ReminderType.REFRESH) {
            glance.refresh()
            return
        }
        if (type == ReminderType.PRE_PRAYER) {
            val snapshot = PrayerSnapshots.at(now, s, calculator) ?: return
            val minutes = Duration.between(now, snapshot.next.time).toMinutes().toInt() + 1
            // A late alarm (Doze, inexact fallback) that lands after the prayer has nothing to say.
            if (minutes > s.reminders.mosqueBeforeMinutes + 5) return
            notifier.prePrayer(snapshot.next.prayer, snapshot.next.time, snapshot.zone, minutes.coerceAtMost(s.reminders.mosqueBeforeMinutes), DateFormat.is24HourFormat(context))
            return
        }
        val prayer = type.prayer
        if (prayer != null && location != null) {
            val schedule = calculator.schedule(today, location.latitude, location.longitude, zone, s.calculation, location.elevation)
            val shown = if (prayer == Prayer.DHUHR && today.dayOfWeek == DayOfWeek.FRIDAY) {
                Prayer.JUMUAH
            } else {
                prayer
            }
            notifier.adhan(shown, schedule.time(prayer), zone, bidiIsolate(location.name), silent, DateFormat.is24HourFormat(context))
            return
        }
        val text = when (type) {
            ReminderType.MORNING_ADHKAR -> context.getString(R.string.notif_morning_text)
            ReminderType.EVENING_ADHKAR -> context.getString(R.string.notif_evening_text)
            ReminderType.BEFORE_SLEEP -> context.getString(R.string.notif_sleep_text)
            ReminderType.FRIDAY -> context.getString(R.string.notif_friday_text)
            ReminderType.FASTING -> {
                val tomorrow = FastingCalendar.day(today.plusDays(1), s.hijriAdjustment)
                val kinds = tomorrow.sunnah(s.reminders.fastingWeekly)
                if (kinds.isEmpty()) return
                val names = kinds.joinToString(", ") { context.getString(it.labelRes()) }
                val fajr = location?.let {
                    calculator.schedule(tomorrow.date, it.latitude, it.longitude, zone, s.calculation, it.elevation).fajr
                }
                if (fajr != null) {
                    context.getString(R.string.notif_fasting_text_fajr, names, id.haho.muslimdaily.core.ui.TimeFormatter(DateFormat.is24HourFormat(context)).format(fajr, zone))
                } else {
                    context.getString(R.string.notif_fasting_text, names)
                }
            }
            ReminderType.REVIEW -> {
                val due = reviews.dueCountOnce(today)
                if (due == 0) return // Only remind when something is due.
                context.resources.getQuantityString(R.plurals.memorize_review_due, due, due)
            }
            else -> return
        }
        notifier.reminder(type, text)
    }
}

/** Re-plans every alarm after reboot, clock or time-zone changes, app update and permission changes. */
@AndroidEntryPoint
class SystemEventsReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var glance: GlanceRefresher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                scheduler.rescheduleAll()
                glance.refresh()
            } finally {
                pending.finish()
            }
        }
    }
}

private val HANDLED = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
)

@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        scheduler.rescheduleAll()
        return Result.success()
    }

    companion object {
        const val NAME = "reschedule-reminders"
    }
}
