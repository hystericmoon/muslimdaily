package id.haho.muslimdaily.feature.settings.data

import id.haho.muslimdaily.core.designsystem.glass.GlassOutlinedButton
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.backup.BackupRepository
import id.haho.muslimdaily.core.designsystem.glass.GlassAlertDialog
import id.haho.muslimdaily.core.designsystem.glass.GlassTonalButton
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MySetsDataViewModel @Inject constructor(
    private val repository: MySetRepository,
    private val backup: BackupRepository,
) : ViewModel() {
    suspend fun exportJson(): String = repository.exportJson()
    suspend fun importJson(text: String): Int = repository.importJson(text)
    suspend fun backupJson(): String = backup.export()
    suspend fun restore(text: String): BackupRepository.Summary = backup.restore(text)
}

private const val FILE_NAME = "muslimdaily-set-saya.json"

@Composable
fun MySetsDataRoute(onBack: () -> Unit, viewModel: MySetsDataViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun say(res: Int, vararg args: Any) = scope.launch { snackbar.showSnackbar(resources.getString(res, *args)) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { write(context, uri, viewModel.exportJson()) }.isSuccess
            say(if (ok) R.string.data_saved else R.string.data_failed)
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { viewModel.importJson(read(context, uri)) }
                .onSuccess { say(R.string.data_imported, it) }
                .onFailure { say(R.string.data_import_failed) }
        }
    }

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

    AppScreen(title = stringResource(R.string.settings_my_sets), onBack = onBack, snackbarHostState = snackbar) {
        MySetsDataContent(
            onShare = { scope.launch { share(context, viewModel.exportJson()) } },
            onSave = { save.launch(FILE_NAME) },
            onImport = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
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
    onShare: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
) {
    run {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppTheme.dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
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
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.backup_create))
            }
            GlassOutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_settings_backup_restore), null)
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.backup_restore))
            }
            androidx.compose.material3.Text(
                stringResource(R.string.data_my_sets_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = AppTheme.spacing.lg).semantics { heading() },
            )
            InfoNote(stringResource(R.string.data_intro))
            // One primary action per screen: the full backup. Sharing a set is secondary.
            GlassTonalButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_ios_share), null)
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.data_share))
            }
            GlassOutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_download), null)
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.data_save))
            }
            GlassOutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_upload), null)
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.data_import))
            }
            InfoNote(stringResource(R.string.data_import_note))
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

private suspend fun share(context: Context, json: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "export").apply { mkdirs() }.resolve(FILE_NAME).apply { writeText(json) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.data_share)))
}
