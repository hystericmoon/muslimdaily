package id.haho.muslimdaily.notifications

import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.ReminderSettings
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.feature.today.TodayPreviewData
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderPlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val calculator = PrayerCalculator()
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant()

    private fun planAll(reminders: ReminderSettings, now: java.time.Instant = at(2026, 10, 21, 12, 0)) =
        ReminderPlanner.plan(now, TodayPreviewData.settings.copy(reminders = reminders), calculator)

    /** Notifications only; the always-on glance refresh is tested on its own. */
    private fun plan(reminders: ReminderSettings, now: java.time.Instant = at(2026, 10, 21, 12, 0)) =
        planAll(reminders, now).filter { it.type != ReminderType.REFRESH }

    @Test
    fun `no notification is scheduled by default, only the glance refresh`() {
        assertEquals(listOf(ReminderType.REFRESH), planAll(ReminderSettings()).map { it.type })
    }

    @Test
    fun `refresh fires at the next of the six times`() {
        val refresh = planAll(ReminderSettings()).single { it.type == ReminderType.REFRESH }
        val asr = calculator.schedule(java.time.LocalDate.of(2026, 10, 21), TodayPreviewData.settings.location!!.latitude, TodayPreviewData.settings.location!!.longitude, zone, TodayPreviewData.settings.calculation).asr
        assertEquals(asr, refresh.at)
    }

    @Test
    fun `no refresh without a location`() {
        val p = ReminderPlanner.plan(at(2026, 10, 21, 12, 0), TodayPreviewData.settings.copy(location = null), calculator)
        assertTrue(p.isEmpty())
    }

    @Test
    fun `pre-prayer reminder comes the chosen minutes before the next prayer`() {
        val pre = plan(ReminderSettings(mosqueBefore = true, mosqueBeforeMinutes = 10)).single { it.type == ReminderType.PRE_PRAYER }
        val refresh = planAll(ReminderSettings()).single { it.type == ReminderType.REFRESH } // Asr at noon
        assertEquals(Duration.ofMinutes(10), Duration.between(pre.at, refresh.at))
    }

    @Test
    fun `adhan fires at the next prayer time, tomorrow if passed`() {
        val p = plan(ReminderSettings(adhan = mapOf(Prayer.FAJR to AdhanMode.SOUND, Prayer.ASR to AdhanMode.SILENT)))
        val fajr = p.single { it.type == ReminderType.ADHAN_FAJR }
        val asr = p.single { it.type == ReminderType.ADHAN_ASR }
        assertEquals(22, fajr.at.atZone(zone).dayOfMonth) // today's Fajr has passed at noon
        assertEquals(21, asr.at.atZone(zone).dayOfMonth)
        assertTrue(asr.silent)
        assertTrue(!fajr.silent)
    }

    @Test
    fun `morning adhkar is fajr plus offset`() {
        val now = at(2026, 10, 21, 2, 0)
        val p = plan(ReminderSettings(morningAdhkar = true, morningOffsetMinutes = 20), now)
        val schedule = calculator.schedule(now.atZone(zone).toLocalDate(), -6.2146, 106.8451, zone, TodayPreviewData.settings.calculation)
        assertEquals(schedule.fajr.plus(Duration.ofMinutes(20)), p.single().at)
    }

    @Test
    fun `daily reminder rolls to tomorrow once the time has passed`() {
        val p = plan(ReminderSettings(beforeSleep = true, beforeSleepTime = LocalTime.of(9, 0)))
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 0), p.single().at.atZone(zone).toLocalDateTime())
    }

    @Test
    fun `friday reminder lands on the next friday`() {
        val p = plan(ReminderSettings(friday = true, fridayTime = LocalTime.of(7, 0)))
        val local = p.single().at.atZone(zone).toLocalDateTime()
        assertEquals(DayOfWeek.FRIDAY, local.dayOfWeek)
        assertEquals(23, local.dayOfMonth)
        // On Friday after the time: the following Friday.
        val later = plan(ReminderSettings(friday = true, fridayTime = LocalTime.of(7, 0)), at(2026, 10, 23, 8, 0))
        assertEquals(30, later.single().at.atZone(zone).dayOfMonth)
    }

    @Test
    fun `prayer reminders need a location`() {
        val p = ReminderPlanner.plan(
            at(2026, 10, 21, 12, 0),
            TodayPreviewData.settings.copy(location = null, reminders = ReminderSettings(adhan = mapOf(Prayer.ASR to AdhanMode.SOUND), reviewQueue = true)),
            calculator,
        )
        assertEquals(listOf(ReminderType.REVIEW), p.map { it.type })
    }

    @Test
    fun `fasting reminder comes the evening before the next sunnah fast`() {
        // Wednesday noon: Thursday is a weekly fast, so tonight at 20.00.
        val p = plan(ReminderSettings(fasting = true, fastingTime = LocalTime.of(20, 0)))
        assertEquals(LocalDateTime.of(2026, 10, 21, 20, 0), p.single { it.type == ReminderType.FASTING }.at.atZone(zone).toLocalDateTime())
        // Without the weekly fasts it waits for the next special day, so later than tonight.
        val special = plan(ReminderSettings(fasting = true, fastingWeekly = false)).single { it.type == ReminderType.FASTING }
        assertTrue(special.at.isAfter(at(2026, 10, 21, 20, 0)))
    }
}
