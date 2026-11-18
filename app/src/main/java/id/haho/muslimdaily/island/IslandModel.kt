package id.haho.muslimdaily.island

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.domain.prayer.PrayerSnapshot
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** What the island shows: the next prayer, when, how long is left and where. */
data class IslandModel(
    val prayer: Prayer,
    val time: Instant,
    val zone: ZoneId,
    val minutesLeft: Int,
    val place: String,
) {
    companion object {
        fun from(now: Instant, snapshot: PrayerSnapshot): IslandModel = IslandModel(
            prayer = snapshot.next.prayer,
            time = snapshot.next.time,
            zone = snapshot.zone,
            minutesLeft = minutesUntil(now, snapshot.next.time),
            place = snapshot.locationName,
        )

        /** Whole minutes left, rounded up so "1 min" shows until the time arrives; never negative. */
        fun minutesUntil(now: Instant, time: Instant): Int {
            val millis = Duration.between(now, time).toMillis()
            if (millis <= 0) return 0
            return ((millis + 59_999) / 60_000).toInt()
        }
    }
}

/** How wide the collapsed pill's camera gap is, from the cutout (null on a phone without one). */
object IslandGeometry {
    /** Space kept between the camera and the text on each side, in dp. */
    const val GAP_PADDING_DP = 6

    /** Width in dp of the empty middle of the pill. Zero without a cutout, so the pill stays slim. */
    fun gapDp(cutoutWidthPx: Int?, density: Float): Int {
        if (cutoutWidthPx == null || cutoutWidthPx <= 0 || density <= 0f) return 0
        return Math.ceil((cutoutWidthPx / density).toDouble()).toInt() + 2 * GAP_PADDING_DP
    }
}
