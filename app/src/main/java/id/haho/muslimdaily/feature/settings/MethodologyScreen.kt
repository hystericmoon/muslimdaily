package id.haho.muslimdaily.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.theme.HahoTheme

@Composable
fun MethodologyRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val version by viewModel.contentVersion.collectAsStateWithLifecycle()
    AdwamScreen(title = stringResource(R.string.settings_methodology), onBack = onBack) {
        MethodologyContent(version)
    }
}

@Composable
fun MethodologyContent(contentVersion: Int?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HahoTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
    ) {
        Section(R.string.methodology_scope_title, R.string.methodology_scope_body)
        Section(R.string.methodology_sources_title, R.string.methodology_sources_body)
        Section(R.string.methodology_grading_title, R.string.methodology_grading_body)
        Section(R.string.methodology_counts_title, R.string.methodology_counts_body)
        Section(R.string.methodology_audio_title, R.string.methodology_audio_body)
        Section(R.string.methodology_review_title, R.string.methodology_reviewer)
        Section(R.string.methodology_prayer_title, R.string.methodology_prayer_body)
        Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
            Text(stringResource(R.string.methodology_version_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.methodology_content_version, contentVersion?.toString() ?: "–"), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.methodology_app_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Section(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = HahoTheme.reading.bodyLarge)
    }
}
