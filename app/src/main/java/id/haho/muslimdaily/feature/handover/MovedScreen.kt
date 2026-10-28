package id.haho.muslimdaily.feature.handover

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import id.haho.muslimdaily.R
import id.haho.design.component.EmptyState
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassOutlinedButton

/**
 * Shown instead of the app once it has handed its data to the new Adwam app ([newPackage]).
 * Removing this app goes through the system's App info page: asking Android to uninstall
 * directly would need another permission.
 */
@Composable
fun MovedScreen(newPackage: String) {
    val context = LocalContext.current
    val open = context.packageManager.getLaunchIntentForPackage(newPackage)
    Column(Modifier.fillMaxSize().statusBarsPadding(), verticalArrangement = Arrangement.Center) {
        EmptyState(
            icon = R.drawable.ic_launcher_monochrome,
            title = stringResource(R.string.handover_moved_title),
            body = stringResource(R.string.handover_moved_body),
        ) {
            if (open != null) {
                GlassButton(onClick = { context.startActivity(open) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.handover_open_new))
                }
            }
            GlassOutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.handover_remove_old))
            }
            Text(
                stringResource(R.string.handover_remove_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
