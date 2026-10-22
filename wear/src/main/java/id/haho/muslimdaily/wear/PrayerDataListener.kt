package id.haho.muslimdaily.wear

import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** New times from the phone: ask the system to redraw the tile. */
class PrayerDataListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.release()
        TileService.getUpdater(this).requestUpdate(PrayerTileService::class.java)
    }
}
