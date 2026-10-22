package id.haho.muslimdaily.notifications

import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.widget.GlanceRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reschedules reminders whenever something they depend on changes: reminder toggles, location
 * (including a new GPS fix), calculation method or offsets. Redraws widgets and the lock-screen
 * notification when what they show changes.
 */
@Singleton
class ReminderSync @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val glance: GlanceRefresher,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        notifier.createChannels()
        scope.launch {
            settings.settings
                .map { listOf(it.reminders, it.location, it.calculation, it.hijriAdjustment) }
                .distinctUntilChanged()
                .collect { scheduler.rescheduleAll() }
        }
        scope.launch {
            // Everything the widgets and the lock-screen notification draw.
            settings.settings
                .map { listOf(it.location, it.calculation, it.iqamahOffsets, it.palette, it.themeMode, it.reminders.nextPrayerOngoing, it.hijriAdjustment) }
                .distinctUntilChanged()
                .collect { glance.refresh() }
        }
    }
}
