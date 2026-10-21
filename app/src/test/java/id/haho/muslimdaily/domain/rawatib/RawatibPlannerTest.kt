package id.haho.muslimdaily.domain.rawatib

import id.haho.muslimdaily.core.model.ContentStatus
import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Position
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.core.model.Tier
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawatibPlannerTest {

    private fun r(
        id: String, prayer: Prayer?, position: Position, rakah: Int?, tier: Tier = Tier.MUAKKADAH,
        hidden: Boolean = true, group: String? = null, default: Boolean = false,
    ) = Rawatib(id, prayer, position, rakah, tier, emptyList(), null, hidden, group, null, default, ContentStatus.DRAFT)

    // Mirrors the structure in content/source/rawatib.csv (taken from the product spec).
    private val data = listOf(
        r("fajr_before", Prayer.FAJR, Position.BEFORE, 2, hidden = false),
        r("dhuhr_before_4", Prayer.DHUHR, Position.BEFORE, 4, group = "dhuhr_qabliyah", default = true),
        r("dhuhr_before_2", Prayer.DHUHR, Position.BEFORE, 2, group = "dhuhr_qabliyah"),
        r("dhuhr_after", Prayer.DHUHR, Position.AFTER, 2),
        r("maghrib_after", Prayer.MAGHRIB, Position.AFTER, 2),
        r("isha_after", Prayer.ISHA, Position.AFTER, 2),
        r("asr_before", Prayer.ASR, Position.BEFORE, 4, Tier.GHAIRU_MUAKKADAH),
        r("maghrib_before", Prayer.MAGHRIB, Position.BEFORE, 2, Tier.GHAIRU_MUAKKADAH),
        r("between", null, Position.BEFORE, null, Tier.GHAIRU_MUAKKADAH),
        r("jumuah_home", Prayer.JUMUAH, Position.AFTER, 2, group = "jumuah_badiyah", default = true),
        r("jumuah_masjid", Prayer.JUMUAH, Position.AFTER, 4, group = "jumuah_badiyah", default = true),
    )
    private val fard = listOf(
        Fard(Prayer.FAJR, 2, ContentStatus.DRAFT), Fard(Prayer.DHUHR, 4, ContentStatus.DRAFT),
        Fard(Prayer.JUMUAH, 2, ContentStatus.DRAFT), Fard(Prayer.ISHA, 4, ContentStatus.DRAFT),
    )

    private fun plan(prayer: Prayer, friday: Boolean = false, travel: Boolean = false, dhuhr: Int = 4) =
        RawatibPlanner.plan(prayer, friday, travel, dhuhr, data, fard)

    private fun ids(lines: List<RawatibLine>) = lines.flatMap { line -> line.entries.map { it.id } }

    @Test
    fun `no badiyah after fajr or asr`() {
        assertTrue(plan(Prayer.FAJR).after.isEmpty())
        assertTrue(plan(Prayer.ASR).after.isEmpty())
    }

    @Test
    fun `dhuhr shows four before by default and two when chosen`() {
        assertEquals(listOf("dhuhr_before_4"), ids(plan(Prayer.DHUHR).before))
        assertEquals(listOf("dhuhr_before_2"), ids(plan(Prayer.DHUHR, dhuhr = 2).before))
        assertEquals(listOf("dhuhr_after"), ids(plan(Prayer.DHUHR).after))
    }

    @Test
    fun `friday dhuhr becomes jumuah without qabliyah`() {
        val p = plan(Prayer.DHUHR, friday = true)
        assertEquals(Prayer.JUMUAH, p.prayer)
        assertTrue(p.before.isEmpty())
        assertTrue(p.showJumuahNote)
        assertEquals(2, p.fard?.rakah)
    }

    @Test
    fun `jumuah badiyah shows home and masjid forms together`() {
        val p = plan(Prayer.DHUHR, friday = true)
        assertEquals(1, p.after.size)
        assertEquals(listOf("jumuah_home", "jumuah_masjid"), p.after.single().entries.map { it.id })
    }

    @Test
    fun `other prayers are unchanged on friday`() {
        assertEquals(Prayer.ASR, plan(Prayer.ASR, friday = true).prayer)
        assertTrue(plan(Prayer.ASR, friday = true).before.isNotEmpty())
    }

    @Test
    fun `travel mode keeps only fajr sunnah`() {
        assertEquals(listOf("fajr_before"), ids(plan(Prayer.FAJR, travel = true).before))
        listOf(Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).forEach { prayer ->
            val p = plan(prayer, travel = true)
            assertTrue(p.before.isEmpty() && p.after.isEmpty(), "$prayer still shows rawatib in travel mode")
        }
        assertTrue(plan(Prayer.DHUHR, friday = true, travel = true).after.isEmpty())
    }

    @Test
    fun `travel mode keeps fard`() {
        assertEquals(4, plan(Prayer.DHUHR, travel = true).fard?.rakah)
    }

    @Test
    fun `between adhan and iqamah appears only where there is no specific qabliyah`() {
        assertEquals(listOf("between"), ids(plan(Prayer.ISHA).before))
        assertEquals(listOf("fajr_before"), ids(plan(Prayer.FAJR).before))
        assertEquals(listOf("maghrib_before"), ids(plan(Prayer.MAGHRIB).before))
        assertTrue("between" !in ids(plan(Prayer.DHUHR, friday = true).before))
    }

    @Test
    fun `muakkadah set totals twelve rakah`() {
        val total = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.MAGHRIB, Prayer.ISHA)
            .map { plan(it) }
            .flatMap { it.before + it.after }
            .map { it.primary }
            .filter { it.tier == Tier.MUAKKADAH }
            .sumOf { it.rakah ?: 0 }
        assertEquals(12, total)
    }
}
