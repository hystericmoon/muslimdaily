package id.haho.muslimdaily.feature.dhikr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.design.component.HahoTextField
import id.haho.design.glass.GlassCard
import id.haho.design.glass.GlassTonalButton
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.data.settings.DHIKR_MAX
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The tally and the target the reader set (0 = none), with the haptics switch from Settings. */
data class DhikrState(val count: Int, val target: Int, val haptics: Boolean)

/**
 * A neutral tally: it adds one per tap, can take one back or reset, and stops nothing at the
 * target (it only marks it). It writes no wording of its own and keeps no history, streak or total.
 */
@HiltViewModel
class DhikrViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<DhikrState?> = settings.settings
        .map { DhikrState(it.dhikrCount, it.dhikrTarget, it.haptics) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun tap() = viewModelScope.launch {
        settings.update { dhikrCount = (dhikrCount + 1).coerceAtMost(DHIKR_MAX) }
    }

    fun back() = viewModelScope.launch {
        settings.update { dhikrCount = (dhikrCount - 1).coerceAtLeast(0) }
    }

    fun reset() = viewModelScope.launch { settings.update { dhikrCount = 0 } }

    fun setTarget(target: Int) = viewModelScope.launch {
        settings.update { dhikrTarget = target.coerceIn(0, DHIKR_MAX) }
    }
}

@Composable
fun DhikrRoute(onBack: () -> Unit, viewModel: DhikrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.dhikr_title), onBack = onBack) {
        state?.let {
            DhikrContent(it, viewModel::tap, viewModel::back, viewModel::reset, viewModel::setTarget)
        }
    }
}

@Composable
fun DhikrContent(
    state: DhikrState,
    onTap: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onSetTarget: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var editing by remember { mutableStateOf(false) }
    val reached = state.target > 0 && state.count >= state.target
    val tally = if (state.target > 0) {
        stringResource(R.string.dhikr_count_of, state.count, state.target)
    } else {
        state.count.toString()
    }
    val reachedNote = stringResource(R.string.dhikr_reached)
    Column(
        Modifier.fillMaxSize().padding(HahoTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        // The whole card is the button: a large target for a thumb, no need to look at it.
        GlassCard(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 240.dp)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.dhikr_tap)) {
                    if (state.haptics) {
                        val next = state.count + 1
                        haptics.performHapticFeedback(
                            if (state.target > 0 && next == state.target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick,
                        )
                    }
                    onTap()
                },
        ) {
            Column(
                Modifier.fillMaxSize().padding(HahoTheme.spacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tally,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    // Each tap is announced; the reached note is part of the same announcement.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        if (reached) contentDescription = "$tally, $reachedNote"
                    },
                )
                if (reached) {
                    Text(
                        reachedNote,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = HahoTheme.spacing.sm).clearAndSetSemantics {},
                    )
                } else {
                    Text(
                        stringResource(R.string.dhikr_tap),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = HahoTheme.spacing.sm).clearAndSetSemantics {},
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            GlassTonalButton(onClick = onBack, enabled = state.count > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dhikr_minus))
            }
            GlassTonalButton(onClick = onReset, enabled = state.count > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.dhikr_reset))
            }
            GlassTonalButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) {
                Text(
                    if (state.target > 0) stringResource(R.string.dhikr_target_value, state.target)
                    else stringResource(R.string.dhikr_target_set),
                )
            }
        }
    }
    if (editing) TargetDialog(state.target, onDismiss = { editing = false }, onConfirm = { onSetTarget(it); editing = false })
}

@Composable
private fun TargetDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dhikr_target_title)) },
        text = {
            HahoTextField(
                value = text,
                onValueChange = { v -> text = v.filter(Char::isDigit).take(DHIKR_MAX.toString().length) },
                label = { Text(stringResource(R.string.dhikr_target_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
