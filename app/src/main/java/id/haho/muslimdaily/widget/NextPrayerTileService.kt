package id.haho.muslimdaily.widget

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import dagger.hilt.android.AndroidEntryPoint
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.notifications.Destination
import id.haho.muslimdaily.notifications.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Quick Settings tile: the next prayer and its time, with the countdown and place underneath.
 * Recomputed whenever the shade opens; tapping opens Today.
 */
@AndroidEntryPoint
class NextPrayerTileService : TileService() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var calculator: PrayerCalculator
    @Inject lateinit var clock: Clock

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
        s.launch { render() }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    private suspend fun render() {
        val tile = qsTile ?: return
        val now = Instant.now(clock)
        val snapshot = PrayerSnapshots.at(now, settings.current(), calculator)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_mosque)
        tile.state = Tile.STATE_INACTIVE
        if (snapshot == null) {
            tile.label = getString(R.string.tile_next_prayer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = getString(R.string.tile_no_location)
        } else {
            val next = snapshot.next
            val time = TimeFormatter(DateFormat.is24HourFormat(this)).format(next.time, snapshot.zone)
            tile.label = getString(R.string.notif_next_prayer_title, getString(next.prayer.labelRes()), time)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle, shortDuration(Duration.between(now, next.time)), snapshot.locationName)
            }
            tile.contentDescription = getString(R.string.widget_next_description, getString(next.prayer.labelRes()), time, snapshot.locationName)
        }
        tile.updateTile()
    }

    private fun shortDuration(d: Duration): String {
        val minutes = ((d.seconds + 59) / 60).coerceAtLeast(0)
        return if (minutes >= 60) getString(R.string.tile_hours_minutes, minutes / 60, minutes % 60) else getString(R.string.tile_minutes, minutes)
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Notifier.EXTRA_DESTINATION, Destination.TODAY.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
