package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.haho.muslimdaily.R
import id.haho.design.component.SegmentedRow
import id.haho.design.component.StepperRow
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PauseMode
import id.haho.muslimdaily.feature.duas.components.speedLabel
import kotlin.math.roundToInt

/** Loops per chunk (1–10), pause length (auto ≈1.2× chunk or fixed seconds), speed 0.5–1.0×. */
@Composable
fun MemorizeSettingsControls(
    settings: MemorizeSettings,
    onLoops: (Int) -> Unit,
    onPauseMode: (PauseMode) -> Unit,
    onPauseSeconds: (Int) -> Unit,
    onSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val loopsTitle = stringResource(R.string.memorize_loops)
        StepperRow(
            title = loopsTitle,
            valueText = "${settings.loopsPerChunk}×",
            onDecrement = { onLoops(settings.loopsPerChunk - 1) },
            onIncrement = { onLoops(settings.loopsPerChunk + 1) },
            decrementDescription = stringResource(R.string.settings_decrease, loopsTitle),
            incrementDescription = stringResource(R.string.settings_increase, loopsTitle),
            canDecrement = settings.loopsPerChunk > 1,
            canIncrement = settings.loopsPerChunk < 10,
        )
        SegmentedRow(
            title = stringResource(R.string.memorize_pause),
            summary = stringResource(R.string.memorize_pause_summary),
            options = PauseMode.entries,
            selected = settings.pauseMode,
            onSelect = onPauseMode,
            label = { stringResource(if (it == PauseMode.AUTO) R.string.memorize_pause_auto else R.string.memorize_pause_manual) },
        )
        if (settings.pauseMode == PauseMode.MANUAL) {
            val pauseTitle = stringResource(R.string.memorize_pause_seconds)
            StepperRow(
                title = pauseTitle,
                valueText = stringResource(R.string.memorize_seconds, settings.manualPauseSeconds),
                onDecrement = { onPauseSeconds(settings.manualPauseSeconds - 1) },
                onIncrement = { onPauseSeconds(settings.manualPauseSeconds + 1) },
                decrementDescription = stringResource(R.string.settings_decrease, pauseTitle),
                incrementDescription = stringResource(R.string.settings_increase, pauseTitle),
                canDecrement = settings.manualPauseSeconds > 1,
                canIncrement = settings.manualPauseSeconds < 30,
            )
        }
        val speedTitle = stringResource(R.string.memorize_speed)
        val step = 0.05f
        StepperRow(
            title = speedTitle,
            valueText = speedLabel(settings.speed),
            onDecrement = { onSpeed(((settings.speed - step) * 100).roundToInt() / 100f) },
            onIncrement = { onSpeed(((settings.speed + step) * 100).roundToInt() / 100f) },
            decrementDescription = stringResource(R.string.settings_decrease, speedTitle),
            incrementDescription = stringResource(R.string.settings_increase, speedTitle),
            canDecrement = settings.speed > 0.5f,
            canIncrement = settings.speed < 1f,
        )
    }
}
