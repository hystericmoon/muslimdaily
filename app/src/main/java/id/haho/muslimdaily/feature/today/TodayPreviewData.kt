package id.haho.muslimdaily.feature.today

import id.haho.muslimdaily.core.model.Fard
import id.haho.muslimdaily.core.model.Position
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.Rawatib
import id.haho.muslimdaily.core.model.Tier
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Structure-only sample data for previews and screenshot tests. Mirrors the rawatib structure in
 * content/source/rawatib.csv (from the product spec); no evidence text.
 */
object TodayPreviewData {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    val settings = AppSettings(
        location = SavedLocation("Jakarta", -6.2146, 106.8451, "Asia/Jakarta", LocationSource.GPS, 0L),
        iqamahOffsets = mapOf(Prayer.FAJR to 10, Prayer.DHUHR to 10, Prayer.ASR to 10, Prayer.MAGHRIB to 5, Prayer.ISHA to 10),
    )

    private fun r(
        id: String, prayer: Prayer?, position: Position, rakah: Int?, tier: Tier = Tier.MUAKKADAH,
        hidden: Boolean = true, group: String? = null, label: String? = null, default: Boolean = false,
    ) = Rawatib(id, prayer, position, rakah, tier, emptyList(), null, hidden, group, label, default)

    val rawatib = listOf(
        r("fajr_before", Prayer.FAJR, Position.BEFORE, 2, hidden = false),
        r("dhuhr_before_4", Prayer.DHUHR, Position.BEFORE, 4, group = "dhuhr_qabliyah", default = true),
        r("dhuhr_before_2", Prayer.DHUHR, Position.BEFORE, 2, group = "dhuhr_qabliyah"),
        r("dhuhr_after", Prayer.DHUHR, Position.AFTER, 2),
        r("maghrib_after", Prayer.MAGHRIB, Position.AFTER, 2),
        r("isha_after", Prayer.ISHA, Position.AFTER, 2),
        r("asr_before", Prayer.ASR, Position.BEFORE, 4, Tier.GHAIRU_MUAKKADAH),
        r("maghrib_before", Prayer.MAGHRIB, Position.BEFORE, 2, Tier.GHAIRU_MUAKKADAH),
        r("between_adhan_iqamah", null, Position.BEFORE, null, Tier.GHAIRU_MUAKKADAH),
        r("jumuah_after_home", Prayer.JUMUAH, Position.AFTER, 2, group = "jumuah_badiyah", label = "di rumah", default = true),
        r("jumuah_after_masjid", Prayer.JUMUAH, Position.AFTER, 4, group = "jumuah_badiyah", label = "di masjid", default = true),
    )

    val fard = listOf(
        Fard(Prayer.FAJR, 2), Fard(Prayer.DHUHR, 4),
        Fard(Prayer.ASR, 4), Fard(Prayer.MAGHRIB, 3),
        Fard(Prayer.ISHA, 4), Fard(Prayer.JUMUAH, 2),
    )

    /** Wednesday 21 Oct 2026, 04:40 in Jakarta: morning adhkar window, after-Fajr prohibition. */
    fun ready(
        at: LocalDateTime = LocalDateTime.of(2026, 10, 21, 4, 40),
        settings: AppSettings = this.settings,
        checked: Set<String> = setOf("rawatib:fajr_before", "fard:FAJR"),
        progress: Map<String, RunProgress> = mapOf("morning" to RunProgress(7, 18, false)),
    ): TodayUiState = TodayStateBuilder.build(
        now = at.atZone(jakarta).toInstant(),
        settings = settings,
        rawatib = rawatib,
        fard = fard,
        checked = checked,
        calculator = PrayerCalculator(),
        runProgress = progress,
    )
}
