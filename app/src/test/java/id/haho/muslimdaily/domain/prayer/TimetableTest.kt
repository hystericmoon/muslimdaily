package id.haho.muslimdaily.domain.prayer

import id.haho.muslimdaily.data.settings.CalculationSettings
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimetableTest {
    private val calculator = PrayerCalculator()
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val schedule = { d: LocalDate -> calculator.schedule(d, -6.2146, 106.8451, jakarta, CalculationSettings()) }

    @Test
    fun `a month has one row per day and each row equals the calculator's day`() {
        val rows = Timetable.month(YearMonth.of(2026, 2), 0, schedule)
        assertEquals(28, rows.size)
        assertEquals(LocalDate.of(2026, 2, 1), rows.first().date)
        assertEquals(LocalDate.of(2026, 2, 28), rows.last().date)
        assertEquals(schedule(LocalDate.of(2026, 2, 14)), rows[13].schedule)
    }

    @Test
    fun `ramadan from outside it is the next one, from inside it is the same one`() {
        val next = assertNotNull(Timetable.ramadan(LocalDate.of(2026, 11, 19), 0, schedule))
        assertTrue(next.size in 29..30, "days ${next.size}")
        assertEquals(1, next.first().hijri.day)
        assertTrue(next.all { it.hijri.month == 9 })
        assertEquals(next.indices.map { next.first().date.plusDays(it.toLong()) }, next.map { it.date })
        val inside = assertNotNull(Timetable.ramadan(next[12].date, 0, schedule))
        assertEquals(next.map { it.date }, inside.map { it.date })
    }

    @Test
    fun `the Hijri adjustment moves the first day of ramadan`() {
        val plain = assertNotNull(Timetable.ramadan(LocalDate.of(2026, 11, 19), 0, schedule))
        val later = assertNotNull(Timetable.ramadan(LocalDate.of(2026, 11, 19), -1, schedule))
        assertEquals(plain.first().date.plusDays(1), later.first().date)
    }
}
