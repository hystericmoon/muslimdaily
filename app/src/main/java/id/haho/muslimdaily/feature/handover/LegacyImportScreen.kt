package id.haho.muslimdaily.feature.handover

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.design.component.EmptyState
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassOutlinedButton
import id.haho.muslimdaily.data.handover.LegacyImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegacyImportViewModel @Inject constructor(private val legacy: LegacyImport) : ViewModel() {
    enum class Step { HIDDEN, OFFER, MANUAL, WORKING, DONE, FAILED }

    private val _step = MutableStateFlow(
        when (legacy.offer()) {
            LegacyImport.Offer.NONE -> Step.HIDDEN
            LegacyImport.Offer.DIRECT -> Step.OFFER
            LegacyImport.Offer.MANUAL -> Step.MANUAL
        },
    )
    val step: StateFlow<Step> = _step

    val legacyPackage: String? get() = legacy.legacyPackage

    fun move() {
        _step.value = Step.WORKING
        viewModelScope.launch {
            _step.value = runCatching { legacy.run() }.fold({ Step.DONE }, { Step.FAILED })
        }
    }

    /** Start fresh, or carry on after the backup instructions or the move. */
    fun finish() {
        if (_step.value != Step.DONE) legacy.dismiss()
        _step.value = Step.HIDDEN
    }
}

/**
 * First launch of the app under its new package, with the previous app still installed: offers to
 * move its data over, then says what has to be set up again (Android keeps permissions and placed
 * widgets per package).
 */
@Composable
fun LegacyImportScreen(step: LegacyImportViewModel.Step, legacyPackage: String?, onMove: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            LegacyImportViewModel.Step.OFFER, LegacyImportViewModel.Step.FAILED -> EmptyState(
                icon = R.drawable.ic_launcher_monochrome,
                title = stringResource(R.string.legacy_import_title),
                body = stringResource(if (step == LegacyImportViewModel.Step.FAILED) R.string.legacy_import_failed else R.string.legacy_import_body),
            ) {
                GlassButton(onClick = onMove, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.legacy_import_move)) }
                TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.legacy_import_skip)) }
            }
            LegacyImportViewModel.Step.WORKING -> CircularProgressIndicator()
            LegacyImportViewModel.Step.MANUAL -> EmptyState(
                icon = R.drawable.ic_launcher_monochrome,
                title = stringResource(R.string.legacy_import_manual_title),
                body = stringResource(R.string.legacy_import_manual_body),
            ) {
                legacyPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }?.let { open ->
                    GlassOutlinedButton(onClick = { context.startActivity(open) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.legacy_import_open_old))
                    }
                }
                GlassButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.legacy_import_continue)) }
            }
            LegacyImportViewModel.Step.DONE -> EmptyState(
                icon = R.drawable.ic_launcher_monochrome,
                title = stringResource(R.string.legacy_import_done_title),
                body = stringResource(R.string.legacy_import_done_body),
            ) {
                GlassButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.legacy_import_continue)) }
                legacyPackage?.let { legacy ->
                    GlassOutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$legacy".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.legacy_import_remove_old)) }
                    Text(
                        stringResource(R.string.handover_remove_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            LegacyImportViewModel.Step.HIDDEN -> Unit
        }
    }
}
