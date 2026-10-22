package id.haho.muslimdaily.domain.prayer

import id.haho.muslimdaily.data.settings.CalculationSettings
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Compares the Kemenag preset with the official schedule for five cities across the year. The
 * official tables use Kemenag's own city coordinates (we use GeoNames centres), so up to one
 * minute of difference is expected; anything larger means the preset is wrong.
 */
class KemenagReferenceTest {
    @Test
    fun `kemenag preset matches the official schedule within one minute`() {
        val calculator = PrayerCalculator()
        val rows = javaClass.classLoader!!.getResource("kemenag_reference.tsv")!!.readText()
            .lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val deltas = mutableMapOf<String, MutableList<Long>>()
        rows.forEach { row ->
            val f = row.split('\t')
            val zone = ZoneId.of(f[3])
            val s = calculator.schedule(LocalDate.parse(f[4]), f[1].toDouble(), f[2].toDouble(), zone, CalculationSettings())
            val ours = listOf(s.fajr, s.sunrise, s.dhuhr, s.asr, s.maghrib, s.isha).map { it.atZone(zone).toLocalTime() }
            listOf("subuh", "terbit", "dzuhur", "ashar", "maghrib", "isya").forEachIndexed { i, name ->
                val official = LocalTime.parse(f[5 + i])
                deltas.getOrPut(name) { mutableListOf() } += Duration.between(official, ours[i]).toMinutes()
            }
        }
        deltas.forEach { (name, d) -> println("$name deltas (ours - official): $d") }
        val worst = deltas.values.flatten().maxOf { abs(it) }
        assertTrue(worst <= 1, "max difference $worst minutes")
    }
}
