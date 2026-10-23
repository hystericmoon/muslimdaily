package id.haho.muslimdaily.feature.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.glass.GlassTonalButton
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.data.routine.Routine

/**
 * The user's routines: each one starts in run mode like the morning adhkar. Empty, it says what
 * a routine is and offers to make one, so the feature is found where the duas are.
 */
@Composable
fun RoutinesCard(
    routines: List<Routine>,
    onStart: (Routine) -> Unit,
    onEdit: (Routine) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
    /** Today shows only existing routines, without the explanation or "new". */
    compact: Boolean = false,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = AppTheme.spacing.sm)) {
            if (routines.isEmpty()) {
                Column(
                    Modifier.padding(horizontal = AppTheme.dimens.cardPadding, vertical = AppTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    Text(stringResource(R.string.routine_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.routine_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GlassTonalButton(onClick = onNew) {
                        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(AppTheme.dimens.smallIcon))
                        Spacer(Modifier.width(AppTheme.spacing.sm))
                        Text(stringResource(R.string.routine_new))
                    }
                }
                return@Column
            }
            routines.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppTheme.dimens.largeTouchTarget)
                        .padding(start = AppTheme.dimens.cardPadding, end = AppTheme.spacing.sm, top = AppTheme.spacing.xs, bottom = AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            pluralStringResource(R.plurals.routine_count, r.duaIds.size, r.duaIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!compact) {
                        IconButton(onClick = { onEdit(r) }) {
                            Icon(painterResource(R.drawable.ic_tune), stringResource(R.string.routine_edit_named, r.name))
                        }
                    }
                    GlassButton(onClick = { onStart(r) }, enabled = r.duaIds.isNotEmpty()) {
                        Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.size(AppTheme.dimens.smallIcon))
                        Spacer(Modifier.width(AppTheme.spacing.xs))
                        Text(stringResource(R.string.action_start))
                    }
                }
            }
            if (!compact) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                androidx.compose.material3.TextButton(onClick = onNew, modifier = Modifier.padding(horizontal = AppTheme.spacing.sm)) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(AppTheme.dimens.smallIcon))
                    Spacer(Modifier.width(AppTheme.spacing.sm))
                    Text(stringResource(R.string.routine_new))
                }
            }
        }
    }
}
