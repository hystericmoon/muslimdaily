package id.haho.muslimdaily.feature.settings

import id.haho.muslimdaily.BuildConfig
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.NavigationRow
import id.haho.design.component.SettingsGroup
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.core.ui.reportError
import id.haho.muslimdaily.data.settings.AppSettings

data class SettingsDestinations(
    val onPrayer: () -> Unit = {},
    val onLocation: () -> Unit = {},
    val onReminders: () -> Unit = {},
    val onDisplay: () -> Unit = {},
    val onAudio: () -> Unit = {},
    val onMySets: () -> Unit = {},
    val onMethodology: () -> Unit = {},
    val onReportError: () -> Unit = {},
    val onDiagnostics: () -> Unit = {},
    val onAbout: () -> Unit = {},
    val onUpdates: () -> Unit = {},
)

@Composable
fun SettingsHomeRoute(destinations: SettingsDestinations, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val version by viewModel.contentVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AdwamScreen(title = stringResource(R.string.nav_settings)) {
        settings?.let {
            SettingsHomeContent(it, destinations.copy(onReportError = { reportError(context, null, version) }))
        }
    }
}

@Composable
fun SettingsHomeContent(settings: AppSettings, d: SettingsDestinations) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item {
            SettingsGroup(stringResource(R.string.settings_group_prayer)) {
                NavigationRow(
                    title = stringResource(R.string.settings_prayer),
                    summary = stringResource(settings.calculation.method.labelRes()) +
                        if (settings.travelMode) " · " + stringResource(R.string.today_travel_chip) else "",
                    icon = R.drawable.ic_schedule,
                    onClick = d.onPrayer,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_location),
                    summary = settings.location?.name ?: stringResource(R.string.today_no_location_title),
                    icon = R.drawable.ic_location_on,
                    onClick = d.onLocation,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_reminders),
                    summary = stringResource(R.string.settings_reminders_summary),
                    icon = R.drawable.ic_notifications,
                    onClick = d.onReminders,
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_group_reading)) {
                NavigationRow(
                    title = stringResource(R.string.settings_display),
                    summary = stringResource(R.string.settings_display_summary),
                    icon = R.drawable.ic_text_fields,
                    onClick = d.onDisplay,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_audio),
                    summary = stringResource(R.string.settings_audio_summary),
                    icon = R.drawable.ic_headphones,
                    onClick = d.onAudio,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_my_sets),
                    summary = stringResource(R.string.settings_my_sets_summary),
                    icon = R.drawable.ic_ios_share,
                    onClick = d.onMySets,
                )
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_group_trust)) {
                NavigationRow(
                    title = stringResource(R.string.settings_methodology),
                    summary = stringResource(R.string.settings_methodology_summary),
                    icon = R.drawable.ic_verified,
                    onClick = d.onMethodology,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_report),
                    summary = stringResource(R.string.settings_report_summary),
                    icon = R.drawable.ic_flag,
                    onClick = d.onReportError,
                )
                NavigationRow(
                    title = stringResource(R.string.diag_title),
                    summary = stringResource(R.string.diag_summary),
                    icon = R.drawable.ic_bug_report,
                    onClick = d.onDiagnostics,
                )
                NavigationRow(
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    icon = R.drawable.ic_info,
                    onClick = d.onAbout,
                )
                if (BuildConfig.SELF_UPDATE) {
                    NavigationRow(
                        title = stringResource(R.string.update_title),
                        summary = stringResource(R.string.update_summary),
                        icon = R.drawable.ic_download,
                        onClick = d.onUpdates,
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.settings_no_ads),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(HahoTheme.dimens.screenGutter),
            )
        }
    }
}
