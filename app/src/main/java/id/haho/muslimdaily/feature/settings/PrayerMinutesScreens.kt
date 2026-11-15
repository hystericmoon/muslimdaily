package id.haho.muslimdaily.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.design.component.InfoNote
import id.haho.design.component.SettingsGroup
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import java.util.Locale

@Composable
fun PrayerOffsetsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_offsets), onBack = onBack) {
        settings?.let { PrayerOffsetsContent(it, viewModel::update) }
    }
}

@Composable
fun PrayerOffsetsContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
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
                        value = settings.calculation.offsets.of(slot),
                        range = -30..30,
                        signed = true,
                    ) { v -> update { setOffset(key, v) } }
                }
            }
        }
    }
}

@Composable
fun IqamahRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_iqamah), onBack = onBack) {
        settings?.let { IqamahContent(it, viewModel::update) }
    }
}

@Composable
fun IqamahContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
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
    }
}
