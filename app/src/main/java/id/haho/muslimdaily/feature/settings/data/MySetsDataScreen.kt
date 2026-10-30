package id.haho.muslimdaily.feature.settings.data

import id.haho.design.glass.GlassOutlinedButton
import id.haho.design.glass.GlassButton
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.data.backup.BackupRepository
import id.haho.design.glass.GlassAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MySetsDataViewModel @Inject constructor(
    private val backup: BackupRepository,
) : ViewModel() {
    suspend fun backupJson(): String = backup.export()
    suspend fun restore(text: String): BackupRepository.Summary = backup.restore(text)
}

@Composable
fun MySetsDataRoute(onBack: () -> Unit, viewModel: MySetsDataViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun say(res: Int, vararg args: Any) = scope.launch { snackbar.showSnackbar(resources.getString(res, *args)) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { write(context, uri, viewModel.backupJson()) }.isSuccess
            say(if (ok) R.string.backup_saved else R.string.data_failed)
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { viewModel.restore(read(context, uri)) }
                .onSuccess { say(R.string.backup_restored, it.sets, it.reviews) }
                .onFailure { say(R.string.backup_restore_failed) }
        }
    }
    var confirmRestore by remember { mutableStateOf(false) }

    AdwamScreen(title = stringResource(R.string.settings_my_sets), onBack = onBack, snackbarHostState = snackbar) {
        MySetsDataContent(
            onBackup = { saveBackup.launch("muslimdaily-cadangan-${LocalDate.now()}.json") },
            onRestore = { confirmRestore = true },
        )
    }
    if (confirmRestore) {
        GlassAlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { androidx.compose.material3.Text(stringResource(R.string.backup_restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { androidx.compose.material3.Text(stringResource(R.string.backup_restore_choose)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { androidx.compose.material3.Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
fun MySetsDataContent(
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    run {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HahoTheme.dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
        ) {
            androidx.compose.material3.Text(
                stringResource(R.string.backup_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            InfoNote(stringResource(R.string.backup_intro))
            GlassButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_backup), null)
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(stringResource(R.string.backup_create))
            }
            GlassOutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_settings_backup_restore), null)
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(stringResource(R.string.backup_restore))
            }
        }
    }
}

@Composable
private fun Text(text: String) = androidx.compose.material3.Text(text)

private suspend fun write(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
}

private suspend fun read(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
}
