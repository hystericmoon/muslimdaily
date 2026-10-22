package id.haho.muslimdaily.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.notifications.Notifier
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Redraws everything glanceable: both home-screen widgets, the lock-screen next-prayer
 * notification and the Wear OS tile's data. Called when the next prayer changes (REFRESH
 * alarm), on boot and time changes, and when settings they show change.
 */
@Singleton
class GlanceRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val calculator: PrayerCalculator,
    private val notifier: Notifier,
    private val clock: Clock,
    private val wear: WearSync,
) {
    suspend fun refresh() {
        val manager = GlanceAppWidgetManager(context)
        val stamp = System.currentTimeMillis()
        for (widget in listOf(NextPrayerWidget(), DayScheduleWidget())) {
            manager.getGlanceIds(widget.javaClass).forEach { id ->
                updateAppWidgetState(context, id) { it[WidgetRefreshKey] = stamp }
                widget.update(context, id)
            }
        }
        val s = settings.current()
        val snapshot = PrayerSnapshots.at(Instant.now(clock), s, calculator)
        wear.push(s, Instant.now(clock))
        if (s.reminders.nextPrayerOngoing && snapshot != null) {
            notifier.nextPrayerOngoing(snapshot, DateFormat.is24HourFormat(context))
        } else {
            notifier.cancelNextPrayerOngoing()
        }
    }
}
