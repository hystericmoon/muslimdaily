package id.haho.muslimdaily.feature.settings

import id.haho.muslimdaily.core.ui.AppLocale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.InfoNote
import id.haho.design.component.RadioGroup
import id.haho.design.component.SegmentedRow
import id.haho.design.component.SettingsGroup
import id.haho.design.component.StepperRow
import id.haho.design.component.SwitchRow
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.ui.descriptionRes
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.AsrMethod
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.data.settings.PrayerOffsets
import id.haho.muslimdaily.data.settings.SettingsEditor
import java.util.Locale

@Composable
fun PrayerSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_prayer), onBack = onBack) {
        settings?.let { PrayerSettingsContent(it, viewModel::update) }
    }
}

@Composable
fun PrayerSettingsContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    val calc = settings.calculation
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item {
            SettingsGroup(stringResource(R.string.settings_method)) {
                RadioGroup(
                    options = CalcMethod.entries,
                    selected = calc.method,
                    onSelect = { m -> update { calcMethod = m } },
                    label = { stringResource(it.labelRes()) },
                    description = { stringResource(it.descriptionRes()) },
                )
                if (calc.method == CalcMethod.CUSTOM) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AngleStepper(R.string.settings_fajr_angle, calc.customFajrAngle) { v -> update { customFajrAngle = v } }
                    AngleStepper(R.string.settings_isha_angle, calc.customIshaAngle) { v -> update { customIshaAngle = v } }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_asr)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_asr_shadow),
                    options = AsrMethod.entries,
                    selected = calc.asr,
                    onSelect = { a -> update { asr = a } },
                    label = { stringResource(if (it == AsrMethod.STANDARD) R.string.settings_asr_standard else R.string.settings_asr_hanafi) },
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_offsets)) {
                InfoNote(
                    stringResource(R.string.settings_offsets_note),
                    Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
                )
                PrayerTimeSlot.entries.forEach { slot ->
                    val key = slot.name.lowercase(Locale.ROOT)
                    MinuteStepper(
                        title = stringResource(slot.labelRes()),
                        value = calc.offsets.of(slot),
                        range = -30..30,
                        signed = true,
                    ) { v -> update { setOffset(key, v) } }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_iqamah)) {
                InfoNote(
                    stringResource(R.string.settings_iqamah_note),
                    Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
                )
                (FIVE_PRAYERS + Prayer.JUMUAH).forEach { prayer ->
                    MinuteStepper(
                        title = stringResource(prayer.labelRes()),
                        value = settings.iqamahOffsets[prayer] ?: 0,
                        range = 0..60,
                        signed = false,
                        zeroText = stringResource(R.string.settings_off),
                    ) { v -> update { setIqamah(prayer, v) } }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_sunnah)) {
                SwitchRow(
                    title = stringResource(R.string.settings_travel_mode),
                    summary = stringResource(R.string.settings_travel_mode_summary),
                    checked = settings.travelMode,
                    onCheckedChange = { v -> update { travelMode = v } },
                    icon = R.drawable.ic_luggage,
                )
                SegmentedRow(
                    title = stringResource(R.string.settings_dhuhr_qabliyah),
                    summary = stringResource(R.string.settings_dhuhr_qabliyah_summary),
                    options = listOf(4, 2),
                    selected = settings.dhuhrQabliyahRakah,
                    onSelect = { v -> update { dhuhrQabliyahRakah = v } },
                    label = { androidx.compose.ui.res.pluralStringResource(R.plurals.rakah_count, it, it) },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_checklist_history),
                    summary = stringResource(R.string.settings_checklist_history_summary),
                    checked = settings.keepChecklistHistory,
                    onCheckedChange = { v -> update { keepChecklistHistory = v } },
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_hijri)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_hijri_adjust),
                    summary = stringResource(R.string.settings_hijri_adjust_summary),
                    options = listOf(-1, 0, 1),
                    selected = settings.hijriAdjustment,
                    onSelect = { v -> update { hijriAdjustment = v } },
                    label = { if (it > 0) "+$it" else it.toString() },
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_windows)) {
                InfoNote(
                    stringResource(R.string.settings_windows_note),
                    Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
                )
                MinuteStepper(
                    title = stringResource(R.string.settings_sunrise_rise),
                    value = calc.sunriseRiseMinutes,
                    range = 5..30,
                    signed = false,
                ) { v -> update { sunriseRiseMinutes = v } }
                MinuteStepper(
                    title = stringResource(R.string.settings_zawal),
                    value = calc.zawalMinutes,
                    range = 3..20,
                    signed = false,
                ) { v -> update { zawalMinutes = v } }
            }
        }
    }
}

private fun PrayerOffsets.of(slot: PrayerTimeSlot): Int = when (slot) {
    PrayerTimeSlot.FAJR -> fajr
    PrayerTimeSlot.SUNRISE -> sunrise
    PrayerTimeSlot.DHUHR -> dhuhr
    PrayerTimeSlot.ASR -> asr
    PrayerTimeSlot.MAGHRIB -> maghrib
    PrayerTimeSlot.ISHA -> isha
}

@Composable
private fun MinuteStepper(
    title: String,
    value: Int,
    range: IntRange,
    signed: Boolean,
    zeroText: String? = null,
    onChange: (Int) -> Unit,
) {
    val text = when {
        value == 0 && zeroText != null -> zeroText
        signed && value > 0 -> stringResource(R.string.settings_minutes_plus, value)
        else -> stringResource(R.string.settings_minutes, value)
    }
    StepperRow(
        title = title,
        valueText = text,
        onDecrement = { onChange((value - 1).coerceIn(range)) },
        onIncrement = { onChange((value + 1).coerceIn(range)) },
        decrementDescription = stringResource(R.string.settings_decrease, title),
        incrementDescription = stringResource(R.string.settings_increase, title),
        canDecrement = value > range.first,
        canIncrement = value < range.last,
    )
}

@Composable
private fun AngleStepper(titleRes: Int, value: Double, onChange: (Double) -> Unit) {
    val title = stringResource(titleRes)
    StepperRow(
        title = title,
        valueText = String.format(AppLocale, "%.1f°", value),
        onDecrement = { onChange((value - 0.5).coerceIn(10.0, 22.0)) },
        onIncrement = { onChange((value + 0.5).coerceIn(10.0, 22.0)) },
        decrementDescription = stringResource(R.string.settings_decrease, title),
        incrementDescription = stringResource(R.string.settings_increase, title),
        canDecrement = value > 10.0,
        canIncrement = value < 22.0,
    )
}
