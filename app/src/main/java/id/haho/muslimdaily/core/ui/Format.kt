package id.haho.muslimdaily.core.ui

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.data.settings.AsrMethod
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.domain.prayer.HijriDate
import id.haho.muslimdaily.domain.prayer.WindowType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The locale the app's text is in: English when the app (or device) language is English,
 * otherwise Indonesian. Read on use, so a language change applies without a restart.
 */
val AppLocale: Locale
    get() = Locale.getDefault().takeIf { it.language == "en" } ?: Indonesian

private val Indonesian: Locale = Locale.forLanguageTag("id-ID")

/** True when the app's text is English. */
val isEnglish: Boolean get() = AppLocale.language == "en"

/** Clock pattern: Indonesian writes 04.32, English 04:32. */
fun clockPattern(is24Hour: Boolean): String = when {
    isEnglish -> if (is24Hour) "HH:mm" else "h:mm a"
    else -> if (is24Hour) "HH.mm" else "h.mm a"
}

/** True at 150% system font and above, where side-by-side controls switch to stacked layouts. */
@Composable
fun isLargeFontScale(): Boolean = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f

/** Formats clock times following the device's 12/24-hour preference, in the app's language style. */
class TimeFormatter(is24Hour: Boolean) {
    private val formatter = DateTimeFormatter.ofPattern(clockPattern(is24Hour), AppLocale)
    fun format(instant: Instant, zone: ZoneId): String = formatter.format(instant.atZone(zone))
}

@Composable
fun rememberTimeFormatter(): TimeFormatter {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    return remember(is24) { TimeFormatter(is24) }
}

private val LongDate get() = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", AppLocale)

fun formatLongDate(date: LocalDate): String = LongDate.format(date)

@Composable
fun hijriLabel(h: HijriDate): String {
    val months = stringArrayResource(R.array.hijri_months)
    return stringResource(R.string.hijri_date, h.day, months[(h.month - 1).coerceIn(0, 11)], h.year)
}

/** "1 jam 23 menit", "23 menit", "kurang dari 1 menit". Rounded up so it never reads 0. */
@Composable
fun durationLabel(d: Duration): String {
    val totalMinutes = ((d.seconds + 59) / 60).coerceAtLeast(0)
    return when {
        d.seconds < 60 -> stringResource(R.string.duration_under_minute)
        totalMinutes >= 60 -> stringResource(R.string.duration_hours_minutes, totalMinutes / 60, totalMinutes % 60)
        else -> pluralStringResource(R.plurals.duration_minutes, totalMinutes.toInt(), totalMinutes.toInt())
    }
}

@StringRes
fun Prayer.labelRes(): Int = when (this) {
    Prayer.FAJR -> R.string.prayer_fajr
    Prayer.DHUHR -> R.string.prayer_dhuhr
    Prayer.ASR -> R.string.prayer_asr
    Prayer.MAGHRIB -> R.string.prayer_maghrib
    Prayer.ISHA -> R.string.prayer_isha
    Prayer.JUMUAH -> R.string.prayer_jumuah
}

@StringRes
fun PrayerTimeSlot.labelRes(): Int = when (this) {
    PrayerTimeSlot.FAJR -> R.string.prayer_fajr
    PrayerTimeSlot.SUNRISE -> R.string.prayer_sunrise
    PrayerTimeSlot.DHUHR -> R.string.prayer_dhuhr
    PrayerTimeSlot.ASR -> R.string.prayer_asr
    PrayerTimeSlot.MAGHRIB -> R.string.prayer_maghrib
    PrayerTimeSlot.ISHA -> R.string.prayer_isha
}

@StringRes
fun WindowType.labelRes(): Int = when (this) {
    WindowType.MORNING_ADHKAR -> R.string.window_morning_adhkar
    WindowType.EVENING_ADHKAR -> R.string.window_evening_adhkar
    WindowType.PROHIBITED_AFTER_FAJR -> R.string.window_prohibited_after_fajr
    WindowType.PROHIBITED_ZAWAL -> R.string.window_prohibited_zawal
    WindowType.PROHIBITED_AFTER_ASR -> R.string.window_prohibited_after_asr
    WindowType.DUHA -> R.string.window_duha
    WindowType.WITR -> R.string.window_witr
}

@StringRes
fun CalcMethod.labelRes(): Int = when (this) {
    CalcMethod.KEMENAG -> R.string.method_kemenag
    CalcMethod.MWL -> R.string.method_mwl
    CalcMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
    CalcMethod.CUSTOM -> R.string.method_custom
}

@StringRes
fun CalcMethod.descriptionRes(): Int = when (this) {
    CalcMethod.KEMENAG -> R.string.method_kemenag_desc
    CalcMethod.MWL -> R.string.method_mwl_desc
    CalcMethod.UMM_AL_QURA -> R.string.method_umm_al_qura_desc
    CalcMethod.CUSTOM -> R.string.method_custom_desc
}

@StringRes
fun AsrMethod.labelRes(): Int = when (this) {
    AsrMethod.STANDARD -> R.string.asr_standard
    AsrMethod.HANAFI -> R.string.asr_hanafi
}
