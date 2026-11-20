package id.haho.muslimdaily.island

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
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
    /** Today's five prayers in order: Subuh, Dzuhur, Ashar, Maghrib, Isya. */
    val five: List<Instant> = emptyList(),
    /** True when the next prayer is tomorrow's Subuh (after Isya). */
    val nextIsTomorrow: Boolean = false,
    val now: Instant = time,
) {
    /** Index into [five] of the next prayer; Subuh (0) when it is tomorrow's. */
    val nextIndex: Int get() = if (nextIsTomorrow) 0 else five.indexOf(time).coerceAtLeast(0)

    /** Where now sits on the arc from Subuh to Isya, 0 before Subuh and 1 after Isya. */
    val arcProgress: Float get() = arcProgress(now, five)

    companion object {
        fun from(now: Instant, snapshot: PrayerSnapshot): IslandModel {
            val five = snapshot.today.filter { it.slot in FIVE_SLOTS }.map { it.time }
            return IslandModel(
                prayer = snapshot.next.prayer,
                time = snapshot.next.time,
                zone = snapshot.zone,
                minutesLeft = minutesUntil(now, snapshot.next.time),
                place = snapshot.locationName,
                five = five,
                nextIsTomorrow = snapshot.nextSlot == null,
                now = now,
            )
        }

        private val FIVE_SLOTS = setOf(
            PrayerTimeSlot.FAJR, PrayerTimeSlot.DHUHR, PrayerTimeSlot.ASR, PrayerTimeSlot.MAGHRIB, PrayerTimeSlot.ISHA,
        )

        /** Where [now] is between the first and last of [five], clamped to 0..1; 0 when there are no times. */
        fun arcProgress(now: Instant, five: List<Instant>): Float {
            if (five.size < 2) return 0f
            val span = Duration.between(five.first(), five.last()).toMillis()
            if (span <= 0) return 0f
            return (Duration.between(five.first(), now).toMillis().toFloat() / span).coerceIn(0f, 1f)
        }

        /** Each prayer's place on the arc, 0 for the first and 1 for the last. */
        fun arcTicks(five: List<Instant>): List<Float> {
            if (five.size < 2) return five.map { 0f }
            val span = Duration.between(five.first(), five.last()).toMillis().toFloat()
            return five.map { if (span <= 0f) 0f else (Duration.between(five.first(), it).toMillis() / span).coerceIn(0f, 1f) }
        }

        /** How far through the time the alert has been open [shownAt] to [time], 0..1. */
        fun alertProgress(now: Instant, shownAt: Instant, time: Instant): Float {
            val span = Duration.between(shownAt, time).toMillis()
            if (span <= 0) return 1f
            return (Duration.between(shownAt, now).toMillis().toFloat() / span).coerceIn(0f, 1f)
        }

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

    /** The measured gap plus the user's adjustment, never below zero. */
    fun withAdjust(baseDp: Int, adjustDp: Int): Int = (baseDp + adjustDp).coerceAtLeast(0)
}

/** When the island opens its big card on its own: once per prayer, [lead] minutes before. */
object IslandAlert {
    const val DEFAULT_LEAD_MINUTES = 10

    /**
     * [alertedFor] is the prayer time already announced. [first] is the first refresh after the
     * service starts: it only announces at exactly [lead] minutes, so turning the island on or a
     * reboot at 4 minutes does not pop a big card.
     */
    fun shouldAlert(
        minutesLeft: Int,
        time: Instant,
        alertedFor: Instant?,
        first: Boolean,
        lead: Int = DEFAULT_LEAD_MINUTES,
    ): Boolean {
        if (minutesLeft <= 0 || minutesLeft > lead) return false
        if (alertedFor == time) return false
        return !first || minutesLeft == lead
    }
}
