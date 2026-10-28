package id.haho.muslimdaily.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.InfoRow
import id.haho.design.component.SettingsGroup
import id.haho.design.component.StepperRow
import id.haho.design.component.SwitchRow
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.feature.duas.components.speedLabel
import id.haho.muslimdaily.feature.memorize.MemorizeSettingsControls
import kotlin.math.roundToInt

@Composable
fun AudioSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_audio), onBack = onBack) {
        settings?.let { AudioSettingsContent(it, viewModel::update) }
    }
}

@Composable
fun AudioSettingsContent(settings: AppSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item {
            SettingsGroup(stringResource(R.string.settings_audio_playback)) {
                val title = stringResource(R.string.settings_audio_speed)
                StepperRow(
                    title = title,
                    valueText = speedLabel(settings.audioSpeed),
                    onDecrement = { update { audioSpeed = ((settings.audioSpeed - 0.05f) * 100).roundToInt() / 100f } },
                    onIncrement = { update { audioSpeed = ((settings.audioSpeed + 0.05f) * 100).roundToInt() / 100f } },
                    decrementDescription = stringResource(R.string.settings_decrease, title),
                    incrementDescription = stringResource(R.string.settings_increase, title),
                    canDecrement = settings.audioSpeed > 0.5f,
                    canIncrement = settings.audioSpeed < 1.25f,
                )
                // One reciter for now, so this is information rather than a choice.
                InfoRow(
                    title = stringResource(R.string.settings_reciter),
                    summary = stringResource(R.string.settings_reciter_default),
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_run_mode)) {
                SwitchRow(
                    title = stringResource(R.string.run_auto_advance),
                    summary = stringResource(R.string.settings_auto_advance_summary),
                    checked = settings.autoAdvance,
                    onCheckedChange = { v -> update { autoAdvance = v } },
                )
                SwitchRow(
                    title = stringResource(R.string.settings_haptics),
                    summary = stringResource(R.string.settings_haptics_summary),
                    checked = settings.haptics,
                    onCheckedChange = { v -> update { haptics = v } },
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_memorize)) {
                MemorizeSettingsControls(
                    settings = settings.memorize,
                    onLoops = { n -> update { memLoops = n.coerceIn(1, 10) } },
                    onPauseMode = { m -> update { memPauseMode = m } },
                    onPauseSeconds = { s -> update { memPauseSeconds = s.coerceIn(1, 30) } },
                    onSpeed = { s -> update { memSpeed = s.coerceIn(0.5f, 1f) } },
                )
            }
        }
    }
}
