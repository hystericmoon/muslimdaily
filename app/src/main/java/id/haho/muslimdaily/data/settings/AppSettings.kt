package id.haho.muslimdaily.data.settings

import id.haho.muslimdaily.core.designsystem.theme.ThemeMode
import id.haho.muslimdaily.core.model.Prayer
import java.time.LocalTime

enum class CalcMethod { KEMENAG, MWL, UMM_AL_QURA, CUSTOM }

enum class AsrMethod { STANDARD, HANAFI }

enum class TranslationMode { INDONESIAN, ENGLISH, BOTH }

enum class AdhanMode { OFF, SOUND, SILENT }

enum class PauseMode { AUTO, MANUAL }

enum class LocationSource { GPS, MANUAL }

data class PrayerOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
)

data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val source: LocationSource,
    val updatedAtMillis: Long,
)

data class CalculationSettings(
    val method: CalcMethod = CalcMethod.KEMENAG,
    val customFajrAngle: Double = 20.0,
    val customIshaAngle: Double = 18.0,
    val asr: AsrMethod = AsrMethod.STANDARD,
    val offsets: PrayerOffsets = PrayerOffsets(),
    /** Minutes after sunrise until the sun is considered fully risen. Ustadz to confirm the default. */
    val sunriseRiseMinutes: Int = 15,
    /** Length of the prohibited window before Dhuhr at zawal. Ustadz to confirm the default. */
    val zawalMinutes: Int = 10,
)

data class ReminderSettings(
    val adhan: Map<Prayer, AdhanMode> = Prayer.entries.associateWith { AdhanMode.OFF },
    val morningAdhkar: Boolean = false,
    val morningOffsetMinutes: Int = 15,
    val eveningAdhkar: Boolean = false,
    val eveningOffsetMinutes: Int = 15,
    val beforeSleep: Boolean = false,
    val beforeSleepTime: LocalTime = LocalTime.of(21, 30),
    val friday: Boolean = false,
    val fridayTime: LocalTime = LocalTime.of(7, 0),
    val reviewQueue: Boolean = false,
    val reviewTime: LocalTime = LocalTime.of(20, 0),
)

data class MemorizeSettings(
    val loopsPerChunk: Int = 3,
    val pauseMode: PauseMode = PauseMode.AUTO,
    val manualPauseSeconds: Int = 3,
    val speed: Float = 0.85f,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val translationMode: TranslationMode = TranslationMode.INDONESIAN,
    val showTransliteration: Boolean = false,
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val calculation: CalculationSettings = CalculationSettings(),
    val iqamahOffsets: Map<Prayer, Int> = emptyMap(),
    val hijriAdjustment: Int = 0,
    val location: SavedLocation? = null,
    val travelMode: Boolean = false,
    /** Which authentic form of the qabliyah Dhuhr to show: 4 (default) or the narration of 2. */
    val dhuhrQabliyahRakah: Int = 4,
    val keepChecklistHistory: Boolean = false,
    val audioSpeed: Float = 1f,
    val reciterId: String = "default",
    val autoAdvance: Boolean = true,
    val haptics: Boolean = true,
    val memorize: MemorizeSettings = MemorizeSettings(),
    val reminders: ReminderSettings = ReminderSettings(),
)
