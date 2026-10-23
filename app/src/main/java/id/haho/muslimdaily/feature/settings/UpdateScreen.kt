package id.haho.muslimdaily.feature.settings

import android.text.format.DateUtils
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SettingsGroup
import id.haho.muslimdaily.core.designsystem.component.SwitchRow
import id.haho.muslimdaily.core.designsystem.glass.GlassAlertDialog
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassOutlinedButton
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.update.UpdateStatus
import id.haho.muslimdaily.update.UpdateUiState
import id.haho.muslimdaily.update.UpdateViewModel

data class UpdateActions(
    val onCheck: () -> Unit = {},
    val onDownload: () -> Unit = {},
    val onInstall: () -> Unit = {},
    val onAutoCheck: (Boolean) -> Unit = {},
    val onBetaChannel: (Boolean) -> Unit = {},
)

/** The activity-scoped updater, shared by the start-up prompt and this screen. */
@Composable
fun activityUpdateViewModel(): UpdateViewModel =
    hiltViewModel(viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner)

@Composable
fun UpdatesRoute(onBack: () -> Unit) {
    val viewModel = activityUpdateViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AppScreen(title = stringResource(R.string.update_title), onBack = onBack) {
        UpdatesContent(
            state,
            UpdateActions(
                onCheck = viewModel::checkNow,
                onDownload = viewModel::download,
                onInstall = { viewModel.installIntent()?.let(context::startActivity) },
                onAutoCheck = viewModel::setAutoCheck,
                onBetaChannel = viewModel::setBetaChannel,
            ),
        )
    }
}

@Composable
fun UpdatesContent(state: UpdateUiState, actions: UpdateActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = AppTheme.spacing.xxl),
    ) {
        SettingsGroup(null, Modifier.padding(top = AppTheme.spacing.md)) {
            Column(
                Modifier.padding(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Text(
                    stringResource(R.string.methodology_app_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    lastCheckedLabel(state.lastCheckMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UpdateStatusBlock(state.status, actions)
            }
        }
        SettingsGroup(null, Modifier.padding(top = AppTheme.spacing.md)) {
            SwitchRow(
                title = stringResource(R.string.update_auto),
                summary = stringResource(R.string.update_auto_summary),
                checked = state.autoCheck,
                onCheckedChange = actions.onAutoCheck,
            )
            SwitchRow(
                title = stringResource(R.string.update_beta),
                summary = stringResource(R.string.update_beta_summary),
                checked = state.betaChannel,
                onCheckedChange = actions.onBetaChannel,
            )
        }
        InfoNote(
            stringResource(R.string.update_privacy),
            Modifier.padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.md),
            icon = R.drawable.ic_lock,
        )
    }
}

@Composable
private fun lastCheckedLabel(millis: Long): String =
    if (millis <= 0L) {
        stringResource(R.string.update_never_checked)
    } else {
        stringResource(
            R.string.update_last_checked,
            DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
        )
    }

/** Status text and the one action that fits it; changes cross-fade. */
@Composable
private fun UpdateStatusBlock(status: UpdateStatus, actions: UpdateActions) {
    val motion = AppTheme.motion
    AnimatedContent(
        targetState = status,
        contentKey = { it::class },
        transitionSpec = { fadeIn(motion.fade()) togetherWith fadeOut(motion.fade()) },
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        label = "updateStatus",
    ) { s ->
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            when (s) {
                UpdateStatus.Idle -> GlassButton(onClick = actions.onCheck) { Text(stringResource(R.string.update_check)) }
                UpdateStatus.Checking -> {
                    Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                UpdateStatus.UpToDate -> {
                    Text(stringResource(R.string.update_up_to_date), style = MaterialTheme.typography.bodyLarge)
                    GlassOutlinedButton(onClick = actions.onCheck) { Text(stringResource(R.string.update_check)) }
                }
                is UpdateStatus.Available -> {
                    Text(stringResource(R.string.update_available, s.update.version), style = MaterialTheme.typography.bodyLarge)
                    ReleaseNotes(s.update.notes)
                    GlassButton(onClick = actions.onDownload) { Text(stringResource(R.string.update_download)) }
                }
                is UpdateStatus.Downloading -> DownloadProgress(s.progress)
                is UpdateStatus.Ready -> {
                    Text(stringResource(R.string.update_ready, s.update.version), style = MaterialTheme.typography.bodyLarge)
                    GlassButton(onClick = actions.onInstall) { Text(stringResource(R.string.update_install)) }
                }
                is UpdateStatus.Failed -> {
                    Text(stringResource(R.string.update_failed), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    GlassOutlinedButton(onClick = if (s.update != null) actions.onDownload else actions.onCheck) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Float) {
    Text(
        if (progress >= 0f) {
            stringResource(R.string.update_downloading_percent, (progress * 100).toInt())
        } else {
            stringResource(R.string.update_downloading)
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    if (progress >= 0f) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReleaseNotes(notes: String) {
    if (notes.isBlank()) return
    Text(
        notes,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
    )
}

/** Shown on start when the daily check finds a newer build. */
@Composable
fun UpdatePromptDialog(state: UpdateUiState, actions: UpdateActions, onDismiss: () -> Unit) {
    val status = state.status
    val update = when (status) {
        is UpdateStatus.Available -> status.update
        is UpdateStatus.Downloading -> status.update
        is UpdateStatus.Ready -> status.update
        is UpdateStatus.Failed -> status.update
        else -> null
    } ?: return
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available, update.version)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                when (status) {
                    is UpdateStatus.Downloading -> DownloadProgress(status.progress)
                    is UpdateStatus.Failed -> Text(stringResource(R.string.update_failed), color = MaterialTheme.colorScheme.error)
                    else -> ReleaseNotes(update.notes.ifBlank { stringResource(R.string.update_prompt_body) })
                }
            }
        },
        confirmButton = {
            when (status) {
                is UpdateStatus.Ready -> TextButton(onClick = actions.onInstall) { Text(stringResource(R.string.update_install)) }
                is UpdateStatus.Downloading -> {}
                is UpdateStatus.Failed -> TextButton(onClick = actions.onDownload) { Text(stringResource(R.string.action_retry)) }
                else -> TextButton(onClick = actions.onDownload) { Text(stringResource(R.string.update_download)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } },
    )
}
