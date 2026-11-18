package id.haho.muslimdaily.island

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class IslandModelTest {
    private val now = Instant.parse("2026-11-18T08:00:00Z")

    @Test fun roundsPartialMinutesUp() {
        assertEquals(1, IslandModel.minutesUntil(now, now.plusSeconds(1)))
        assertEquals(1, IslandModel.minutesUntil(now, now.plusSeconds(60)))
        assertEquals(2, IslandModel.minutesUntil(now, now.plusSeconds(61)))
        assertEquals(42, IslandModel.minutesUntil(now, now.plusSeconds(42 * 60L - 5)))
    }

    @Test fun neverNegative() {
        assertEquals(0, IslandModel.minutesUntil(now, now))
        assertEquals(0, IslandModel.minutesUntil(now, now.minusSeconds(300)))
    }

    @Test fun gapFollowsTheCutout() {
        // A 90 px camera area at 3x density is 30 dp, plus 6 dp each side.
        assertEquals(42, IslandGeometry.gapDp(90, 3f))
        // A partial dp rounds up so the camera is never covered.
        assertEquals(42, IslandGeometry.gapDp(88, 3f))
    }

    @Test fun noCutoutMeansNoGap() {
        assertEquals(0, IslandGeometry.gapDp(null, 3f))
        assertEquals(0, IslandGeometry.gapDp(0, 3f))
        assertEquals(0, IslandGeometry.gapDp(90, 0f))
    }
}

class IslandAlertTest {
    private val time = java.time.Instant.parse("2026-11-18T08:00:00Z")

    @Test fun alertsOnceInsideTheLeadWindow() {
        assertEquals(false, IslandAlert.shouldAlert(11, time, null, first = false))
        assertEquals(true, IslandAlert.shouldAlert(10, time, null, first = false))
        assertEquals(true, IslandAlert.shouldAlert(7, time, null, first = false))
        assertEquals(false, IslandAlert.shouldAlert(7, time, time, first = false))
        assertEquals(false, IslandAlert.shouldAlert(0, time, null, first = false))
    }

    @Test fun nextPrayerAlertsAgain() {
        assertEquals(true, IslandAlert.shouldAlert(10, time.plusSeconds(3600), time, first = false))
    }

    @Test fun startingLateStaysQuiet() {
        assertEquals(false, IslandAlert.shouldAlert(4, time, null, first = true))
        assertEquals(true, IslandAlert.shouldAlert(10, time, null, first = true))
    }
}

class IslandLeadTest {
    private val time = java.time.Instant.parse("2026-11-18T08:00:00Z")

    @Test fun leadIsConfigurable() {
        assertEquals(false, IslandAlert.shouldAlert(12, time, null, first = false, lead = 10))
        assertEquals(true, IslandAlert.shouldAlert(12, time, null, first = false, lead = 15))
        assertEquals(true, IslandAlert.shouldAlert(15, time, null, first = true, lead = 15))
        assertEquals(false, IslandAlert.shouldAlert(14, time, null, first = true, lead = 15))
    }

    @Test fun gapAdjustmentNeverGoesNegative() {
        assertEquals(30, IslandGeometry.withAdjust(40, -10))
        assertEquals(0, IslandGeometry.withAdjust(10, -24))
        assertEquals(24, IslandGeometry.withAdjust(0, 24))
    }
}
