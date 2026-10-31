package id.haho.muslimdaily.feature.whatsnew

import androidx.annotation.ArrayRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.R
import id.haho.design.glass.GlassAlertDialog
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.update.Versions

/**
 * Release highlights shown once after an update, written for readers rather than developers.
 * Add an entry (newest first) with each release that changes what people see.
 */
object WhatsNew {
    data class Release(val version: String, @ArrayRes val items: Int)

    val releases = listOf(
        Release("1.8.0", R.array.whats_new_1_8_0),
        Release("1.7.0", R.array.whats_new_1_7_0),
        Release("1.6.0", R.array.whats_new_1_6_0),
        Release("1.5.0", R.array.whats_new_1_5_0),
        Release("1.4.2", R.array.whats_new_1_4_2),
        Release("1.4.1", R.array.whats_new_1_4_1),
        Release("1.4.0", R.array.whats_new_1_4_0),
        Release("1.3.1", R.array.whats_new_1_3_1),
        Release("1.3.0", R.array.whats_new_1_3_0),
        Release("1.2.0", R.array.whats_new_1_2_0),
        Release("1.1.0", R.array.whats_new_1_1_0),
        Release("1.0.1", R.array.whats_new_1_0_1),
        Release("1.0.0", R.array.whats_new_1_0_0),
        Release("0.9.0", R.array.whats_new_0_9_0),
        Release("0.8.1", R.array.whats_new_0_8_1),
        Release("0.8.0", R.array.whats_new_0_8_0),
        Release("0.7.0", R.array.whats_new_0_7_0_final),
        Release("0.7.0-beta.4", R.array.whats_new_0_7_0_beta_4),
        Release("0.7.0-beta.3", R.array.whats_new_0_7_0_beta_3),
        Release("0.7.0-beta.2", R.array.whats_new_0_7_0_beta_2),
        Release("0.7.0-beta.1", R.array.whats_new_0_7_0),
        Release("0.6.0", R.array.whats_new_0_6_0),
        Release("0.5.3", R.array.whats_new_0_5_3),
        Release("0.5.2", R.array.whats_new_0_5_2),
    )

    /**
     * What to show when [current] runs after [seen]. Nothing on a fresh install ([seen] blank and
     * [firstRun]), at most the three latest releases after a long gap. A final release sums up its
     * betas, so their entries are left out once it ships.
     */
    fun since(seen: String, current: String, firstRun: Boolean): List<Release> {
        if (firstRun) return emptyList()
        return releases
            .filter { !Versions.isNewer(it.version, current) }
            .filter { Versions.isPreRelease(current) || !Versions.isPreRelease(it.version) }
            .filter { seen.isBlank() || Versions.isNewer(it.version, seen) }
            .take(3)
    }
}

@Composable
fun WhatsNewDialog(releases: List<WhatsNew.Release>, onDismiss: () -> Unit) {
    if (releases.isEmpty()) return
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, releases.first().version)) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
            ) {
                releases.forEachIndexed { i, release ->
                    if (i > 0) {
                        Text(
                            stringResource(R.string.whats_new_earlier, release.version),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    stringArrayResource(release.items).forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp).size(HahoTheme.dimens.smallIcon),
                            )
                            Spacer(Modifier.width(HahoTheme.spacing.sm))
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.whats_new_ok)) } },
    )
}
