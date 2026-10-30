package id.haho.muslimdaily.feature.settings

import id.haho.design.glass.GlassAlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R
import id.haho.muslimdaily.feature.whatsnew.WhatsNew
import id.haho.muslimdaily.feature.whatsnew.WhatsNewDialog
import id.haho.muslimdaily.update.Versions
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.InfoRow
import id.haho.design.component.NavigationRow
import id.haho.design.component.SettingsGroup
import id.haho.design.theme.HahoTheme

private data class License(val name: String, val detail: String, val asset: String?)

private val Licenses = listOf(
    License("Plus Jakarta Sans", "SIL Open Font License 1.1", "licenses/plus_jakarta_sans_OFL.txt"),
    License("Noto Naskh Arabic", "SIL Open Font License 1.1", "licenses/noto_naskh_arabic_OFL.txt"),
    License("Scheherazade New", "SIL Open Font License 1.1", "licenses/scheherazade_new_OFL.txt"),
    License("Amiri", "SIL Open Font License 1.1", "licenses/amiri_OFL.txt"),
    License("Amiri Quran", "SIL Open Font License 1.1", "licenses/amiri_quran_OFL.txt"),
    License("Noto Sans Arabic", "SIL Open Font License 1.1", "licenses/noto_sans_arabic_OFL.txt"),
    License("Material Symbols", "Apache License 2.0", null),
    License("GeoNames", "Creative Commons Attribution 4.0", null),
    License("Audio Hisnul Muslim: Hamad ad-Duraihim", "IslamHouse.com. Boleh disebarkan dengan menyebut sumber, tanpa mengubah isi.", null),
    License("Tanzil Quran Text (teks Al-Qur'an)", "Tanzil Project, tanzil.net. Boleh disalin tanpa diubah dengan menyebut sumber.", null),
    License("QuranEnc.com (terjemah Al-Qur'an)", "Terjemah Kementerian Agama RI (indonesian_affairs v1.0.1) dan Saheeh International. Dimuat tanpa diubah dengan menyebut sumber.", null),
    License("OpenStreetMap (data masjid)", "© kontributor OpenStreetMap, Open Database License 1.0", null),
    License("Bulan di widget: NASA CGI Moon Kit", "NASA's Scientific Visualization Studio, peta warna LRO LROC. Domain publik.", null),
    License("Matahari di widget: NASA/SDO", "Courtesy of NASA/SDO and the HMI science team.", null),
    License("Jetpack Glance", "Apache License 2.0", null),
    License("adhan-kotlin (Batoul Apps)", "MIT License", null),
    License("AndroidX, Media3, Hilt, kotlinx", "Apache License 2.0", null),
)

@Composable
fun AboutRoute(onBack: () -> Unit) {
    AdwamScreen(title = stringResource(R.string.settings_about), onBack = onBack) { AboutContent() }
}

/** [versionName] is a parameter so screenshots stay the same when the version changes. */
@Composable
fun AboutContent(versionName: String = BuildConfig.VERSION_NAME) {
    val context = LocalContext.current
    var shown by remember { mutableStateOf<License?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
    ) {
        Column(Modifier.padding(HahoTheme.dimens.screenGutter), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.methodology_app_version, versionName), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.about_body), style = HahoTheme.reading.bodyLarge)
            Text(stringResource(R.string.settings_no_ads), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var whatsNew by remember { mutableStateOf(false) }
        SettingsGroup(null) {
            NavigationRow(
                title = stringResource(R.string.whats_new_settings),
                summary = stringResource(R.string.whats_new_settings_summary),
                icon = R.drawable.ic_info,
                onClick = { whatsNew = true },
            )
        }
        if (whatsNew) {
            WhatsNewDialog(WhatsNew.releases.filter { !Versions.isNewer(it.version, versionName) }.take(3)) { whatsNew = false }
        }
        SettingsGroup(stringResource(R.string.about_licenses)) {
            Licenses.forEach { license ->
                // Only licences with a full text open one; the rest are complete as shown.
                if (license.asset != null) {
                    NavigationRow(title = license.name, summary = license.detail, onClick = { shown = license })
                } else {
                    InfoRow(title = license.name, summary = license.detail)
                }
            }
        }
    }
    shown?.let { license ->
        val text = remember(license) {
            license.asset?.let { runCatching { context.assets.open(it).bufferedReader().readText() }.getOrNull() } ?: license.detail
        }
        GlassAlertDialog(
            onDismissRequest = { shown = null },
            title = { Text(license.name) },
            text = { Text(text, modifier = Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { shown = null }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}
