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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MySetsDataViewModel @Inject constructor(private val repository: MySetRepository) : ViewModel() {
    suspend fun exportJson(): String = repository.exportJson()
    suspend fun importJson(text: String): Int = repository.importJson(text)
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

    AppScreen(title = stringResource(R.string.settings_my_sets), onBack = onBack, snackbarHostState = snackbar) {
        MySetsDataContent(
            onShare = { scope.launch { share(context, viewModel.exportJson()) } },
            onSave = { save.launch(FILE_NAME) },
            onImport = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
    }
}

@Composable
fun MySetsDataContent(onShare: () -> Unit, onSave: () -> Unit, onImport: () -> Unit) {
    run {
        Column(
            Modifier.fillMaxSize().padding(AppTheme.dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            InfoNote(stringResource(R.string.data_intro))
            GlassButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
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
