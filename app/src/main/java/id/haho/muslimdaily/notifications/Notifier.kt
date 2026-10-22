package id.haho.muslimdaily.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.domain.prayer.PrayerSnapshot
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Where a notification tap should land. */
enum class Destination { TODAY, MORNING_RUN, EVENING_RUN, SLEEP_EVENT, MEMORIZE, MOSQUES }

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ADHAN, context.getString(R.string.channel_adhan), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_adhan_desc)
                },
                NotificationChannel(CHANNEL_ADHAN_SILENT, context.getString(R.string.channel_adhan_silent), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_adhan_silent_desc)
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_reminders_desc)
                    setSound(null, null)
                },
                NotificationChannel(CHANNEL_PRE_PRAYER, context.getString(R.string.channel_pre_prayer), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_pre_prayer_desc)
                },
                NotificationChannel(CHANNEL_NEXT_PRAYER, context.getString(R.string.channel_next_prayer), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.channel_next_prayer_desc)
                    setShowBadge(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                },
            ),
        )
    }

    fun canPost(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun adhan(prayer: Prayer, at: Instant, zone: ZoneId, place: String, silent: Boolean, is24Hour: Boolean) {
        val name = context.getString(prayer.labelRes())
        post(
            id = ReminderType.adhanFor(prayer).requestCode,
            channel = if (silent) CHANNEL_ADHAN_SILENT else CHANNEL_ADHAN,
            title = context.getString(R.string.notif_adhan_title, name),
            text = context.getString(R.string.notif_adhan_text, name, TimeFormatter(is24Hour).format(at, zone), place),
            destination = Destination.TODAY,
        )
    }

    fun reminder(type: ReminderType, text: String) {
        val (title, destination) = when (type) {
            ReminderType.MORNING_ADHKAR -> R.string.adhkar_morning to Destination.MORNING_RUN
            ReminderType.EVENING_ADHKAR -> R.string.adhkar_evening to Destination.EVENING_RUN
            ReminderType.BEFORE_SLEEP -> R.string.notif_sleep_title to Destination.SLEEP_EVENT
            ReminderType.FRIDAY -> R.string.notif_friday_title to Destination.TODAY
            ReminderType.REVIEW -> R.string.notif_review_title to Destination.MEMORIZE
            else -> return
        }
        post(type.requestCode, CHANNEL_REMINDERS, context.getString(title), text, destination)
    }

    /** "Dzuhur in 10 minutes" with a tap through to the nearby-mosque list. */
    fun prePrayer(prayer: Prayer, at: Instant, zone: ZoneId, minutes: Int, is24Hour: Boolean) {
        val name = context.getString(prayer.labelRes())
        post(
            id = ReminderType.PRE_PRAYER.requestCode,
            channel = CHANNEL_PRE_PRAYER,
            title = context.resources.getQuantityString(R.plurals.notif_pre_prayer_title, minutes, name, minutes),
            text = context.getString(R.string.notif_pre_prayer_text, TimeFormatter(is24Hour).format(at, zone)),
            destination = Destination.MOSQUES,
        )
    }

    /**
     * The next prayer with a live countdown, kept on the lock screen. Quiet (low importance, no
     * sound); on Android 16 it asks to be promoted to a Live Update, which the user can turn off
     * per app.
     */
    fun nextPrayerOngoing(snapshot: PrayerSnapshot, is24Hour: Boolean) {
        if (!canPost()) return
        val next = snapshot.next
        val name = context.getString(next.prayer.labelRes())
        val time = TimeFormatter(is24Hour)
        val text = buildList {
            add(snapshot.locationName)
            next.iqamah?.let { add(context.getString(R.string.today_iqamah, time.format(it, snapshot.zone))) }
        }.joinToString(" · ")
        val notification = NotificationCompat.Builder(context, CHANNEL_NEXT_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.notif_next_prayer_title, name, time.format(next.time, snapshot.zone)))
            .setContentText(text)
            .setContentIntent(openIntent(ID_NEXT_PRAYER, Destination.TODAY))
            .setWhen(next.time.toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(time.format(next.time, snapshot.zone))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_NEXT_PRAYER, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    fun cancelNextPrayerOngoing() = NotificationManagerCompat.from(context).cancel(ID_NEXT_PRAYER)

    private fun openIntent(id: Int, destination: Destination): PendingIntent = PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_DESTINATION, destination.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(id: Int, channel: String, title: String, text: String, destination: Destination) {
        if (!canPost()) return
        val open = openIntent(id, destination)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_ADHAN || channel == CHANNEL_ADHAN_SILENT) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    companion object {
        const val CHANNEL_ADHAN = "adhan"
        const val CHANNEL_ADHAN_SILENT = "adhan_silent"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_PRE_PRAYER = "pre_prayer"
        const val CHANNEL_NEXT_PRAYER = "next_prayer"
        private const val ID_NEXT_PRAYER = 100
        const val EXTRA_DESTINATION = "destination"
    }
}
