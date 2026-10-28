package id.haho.muslimdaily.data.settings

import id.haho.muslimdaily.core.designsystem.theme.ArabicFont
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
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
    /** Metres above sea level, for Kemenag's sunrise and Maghrib horizon correction. */
    val elevation: Int = 0,
)

data class CalculationSettings(
    val method: CalcMethod = CalcMethod.KEMENAG,
    val customFajrAngle: Double = 20.0,
    val customIshaAngle: Double = 18.0,
    val asr: AsrMethod = AsrMethod.STANDARD,
    val offsets: PrayerOffsets = PrayerOffsets(),
    /** Minutes after sunrise until the sun is considered fully risen. */
    val sunriseRiseMinutes: Int = 15,
    /** Length of the prohibited window before Dhuhr at zawal. */
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
    /** Notify this many minutes before each prayer, with a way to find a nearby mosque. */
    val mosqueBefore: Boolean = false,
    val mosqueBeforeMinutes: Int = 10,
    /** A quiet ongoing notification with the next prayer and a countdown, shown on the lock screen. */
    val nextPrayerOngoing: Boolean = false,
    /** The evening before a sunnah fast. */
    val fasting: Boolean = false,
    val fastingTime: LocalTime = LocalTime.of(20, 0),
    /** Whether Mondays and Thursdays count for the fasting reminder. */
    val fastingWeekly: Boolean = true,
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
    val palette: AppPalette = AppPalette.entries.first(),
    /** Frosted glass surfaces. Off gives solid surfaces (lighter on older phones). */
    val glassEffect: Boolean = true,
    val translationMode: TranslationMode = TranslationMode.INDONESIAN,
    val showTransliteration: Boolean = false,
    val arabicScale: Float = 1f,
    val arabicFont: ArabicFont = ArabicFont.NASKH,
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
    /** Run mode hides the Arabic of duas in "My hifz" until revealed, to recite them from memory. */
    val runFromMemory: Boolean = true,
    /** How opaque the home-screen widgets' glass is, 0.4–1. The default keeps text AA over any wallpaper. */
    val widgetOpacity: Float = WIDGET_OPACITY_DEFAULT,
    /** The first-launch setup was finished or skipped. */
    val onboardingDone: Boolean = false,
    /** The app version whose "what's new" the reader has seen; blank on a fresh install. */
    val whatsNewSeen: String = "",
    /** Old saved locations were given an elevation once (see LocationRepository.backfillElevation). */
    val elevationChecked: Boolean = false,
    /** The Hijri month-end ("yyyy-mm" of the month asked about) the user already answered. */
    val hijriConfirmedFor: String = "",
    /** The reader dismissed Today's note asking for exact alarms so the widget changes on time. */
    val widgetAlarmNoteDismissed: Boolean = false,
    val memorize: MemorizeSettings = MemorizeSettings(),
    val reminders: ReminderSettings = ReminderSettings(),
)

const val WIDGET_OPACITY_DEFAULT = 0.85f
const val WIDGET_OPACITY_MIN = 0.4f
