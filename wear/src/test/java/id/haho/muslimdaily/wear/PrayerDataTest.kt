package id.haho.muslimdaily.wear

import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerDataTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val thursday = LocalDate.of(2026, 10, 22)
    private val clock = listOf("04:13", "05:26", "11:41", "14:50", "17:49", "18:59")
    private val data = PrayerData(
        place = "Jakarta",
        zone = zone,
        firstDay = thursday,
        times = (0L..1L).flatMap { d -> clock.map { thursday.plusDays(d).atTime(LocalTime.parse(it)).atZone(zone).toInstant() } },
        is24 = true,
    )

    @Test
    fun `upcoming skips sunrise and past prayers`() {
        val now = thursday.atTime(12, 0).atZone(zone).toInstant()
        val up = data.upcoming(now)
        assertEquals(listOf(3, 4, 5, 0, 2, 3, 4, 5), up.map { it.slot })
        assertTrue(up.none { it.slot == 1 })
    }

    @Test
    fun `Friday's Dhuhr is Jumuah`() {
        val friday = data.slots.single { it.day == thursday.plusDays(1) && it.slot == 2 }
        assertTrue(friday.isFridayDhuhr)
        assertTrue(data.slots.none { it.day == thursday && it.isFridayDhuhr })
    }
}
