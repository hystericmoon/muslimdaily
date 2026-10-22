package id.haho.muslimdaily.notifications

import id.haho.muslimdaily.data.settings.SettingsRepository
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
 * (including a new GPS fix), calculation method or offsets.
 */
@Singleton
class ReminderSync @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        notifier.createChannels()
        scope.launch {
            settings.settings
                .map { Triple(it.reminders, it.location, it.calculation) }
                .distinctUntilChanged()
                .collect { scheduler.rescheduleAll() }
        }
    }
}
