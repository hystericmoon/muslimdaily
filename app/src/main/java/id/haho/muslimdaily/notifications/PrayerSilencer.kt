package id.haho.muslimdaily.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.settings.SettingsRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silences the phone while a prayer is prayed: Do Not Disturb "alarms only" from the prayer's
 * time for N minutes, then the filter it was before. It never touches a phone that is already in
 * Do Not Disturb, and only restores a filter it set itself and the reader has not changed since.
 * What it did is kept in a private file (not in settings, so a backup never replays it).
 */
@Singleton
class PrayerSilencer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val state = context.getSharedPreferences("prayer_silence", Context.MODE_PRIVATE)

    fun hasAccess(): Boolean = manager.isNotificationPolicyAccessGranted

    /** Called at a prayer's time. A late alarm that lands after the window has nothing to silence. */
    suspend fun start() {
        reconcile() // a window left open by a missed end alarm is closed first
        val reminders = settings.current().reminders
        val minutes = reminders.silenceMinutes
        if (!reminders.silence || !hasAccess()) return
        if (state.contains(KEY_END)) return // already silenced by us
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        val end = clock.millis() + Duration.ofMinutes(minutes.toLong()).toMillis()
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        state.edit(commit = true) { putLong(KEY_END, end) }
    }

    /** Called at the end of the window, and by every re-plan: restores when due or switched off. */
    suspend fun reconcile(force: Boolean = false) {
        if (!state.contains(KEY_END)) return
        val due = clock.millis() >= state.getLong(KEY_END, 0L)
        val off = force || !settings.current().reminders.silence
        if (!due && !off) return
        restore()
    }

    private fun restore() {
        // Only undo our own filter: if the reader changed it meanwhile, leave their choice.
        if (hasAccess() && manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        state.edit(commit = true) { remove(KEY_END) }
    }

    private companion object {
        const val KEY_END = "end_millis"
    }
}
