package id.haho.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import id.haho.design.R
import id.haho.design.glass.GlassAlertDialog
import id.haho.design.theme.HahoTheme

/** One entry in [OpenSourceNoticesDialog]: a heading (name and licence) and the licence text. */
@Immutable
data class OpenSourceNotice(val title: String, val text: String)

/**
 * Open-source notices as plain, scrollable text in a glass dialog. Always lists the Plus Jakarta
 * Sans licence (SIL OFL 1.1), which every app using the library ships; pass [extra] for the app's
 * own. Open it from a settings row titled `haho_notices_title`.
 */
@Composable
fun OpenSourceNoticesDialog(onDismiss: () -> Unit, extra: List<OpenSourceNotice> = emptyList()) {
    val resources = LocalResources.current
    val font = stringResource(R.string.haho_notices_font)
    val notices = remember(resources, font, extra) {
        val ofl = resources.openRawResource(R.raw.haho_plus_jakarta_sans_ofl).bufferedReader().use { it.readText() }
        listOf(OpenSourceNotice(font, ofl)) + extra
    }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = HahoTheme.dimens.minTouchTarget)) {
                Text(stringResource(R.string.haho_action_close))
            }
        },
        title = { Text(stringResource(R.string.haho_notices_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
            ) {
                for (n in notices) {
                    Text(n.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                    Text(n.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}
