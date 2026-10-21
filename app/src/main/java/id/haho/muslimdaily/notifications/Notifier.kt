package id.haho.muslimdaily.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Where a notification tap should land. */
enum class Destination { TODAY, MORNING_RUN, EVENING_RUN, SLEEP_EVENT, MEMORIZE }

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
            ),
        )
    }

    fun canPost(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

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

    private fun post(id: Int, channel: String, title: String, text: String, destination: Destination) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_DESTINATION, destination.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_ALARM)
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
        const val EXTRA_DESTINATION = "destination"
    }
}
