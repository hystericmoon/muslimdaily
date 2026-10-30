package id.haho.muslimdaily.feature.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import id.haho.design.glass.GlassBottomSheet
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.data.routine.Routine

/** The reader's routines with [duaId] ticked where it is in them, and a new routine that starts with it. */
@Composable
fun RoutineSheet(
    duaId: String,
    routines: List<Routine>,
    onSetIncluded: (routineId: String, included: Boolean) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = HahoTheme.spacing.xxl)) {
            Text(
                stringResource(R.string.dua_add_to_routine),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm).semantics { heading() },
            )
            routines.forEach { routine ->
                val checked = duaId in routine.duaIds
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                        .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onSetIncluded(routine.id, it) })
                        .padding(horizontal = HahoTheme.dimens.screenGutter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Spacer(Modifier.width(HahoTheme.spacing.md))
                    Text(routine.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                    .clickable(role = Role.Button) { onDismiss(); onNew() }
                    .padding(horizontal = HahoTheme.dimens.screenGutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_add), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(HahoTheme.spacing.md))
                Text(stringResource(R.string.routine_new), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
