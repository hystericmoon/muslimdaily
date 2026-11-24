package id.haho.design.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.haho.design.R
import id.haho.design.glass.popOn
import id.haho.design.theme.HahoTheme

/** True at 150% font scale and above, where layouts should stack instead of sitting side by side. */
@Composable
fun isLargeFontScale(): Boolean = LocalDensity.current.fontScale >= 1.5f

/** A small text label on a tinted pill. Always text, so it never relies on colour alone. */
@Composable
fun Badge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = HahoTheme.spacing.sm, vertical = HahoTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * A whole-row toggle with a trailing checkbox. The row is the touch target (at least 48dp) and
 * TalkBack reads it as one checkbox.
 *
 * The checkbox sits at the top, so it stays level with the row's title however many lines
 * follow. Pad the title with [checkRowTitleInset] to centre its first line on the checkbox.
 *
 * @param checkedDescription / [uncheckedDescription] are read as the state; TalkBack is silent
 *   when unchecked otherwise, so both states are named.
 */
@Composable
fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = HahoTheme.dimens.minTouchTarget,
    /** Top puts the checkbox level with the first line; CenterVertically centres it on the whole item. */
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    checkedDescription: String = stringResource(R.string.haho_state_checked),
    uncheckedDescription: String = stringResource(R.string.haho_state_not_checked),
    content: @Composable RowScope.() -> Unit,
) {
    val state = if (checked) checkedDescription else uncheckedDescription
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics { stateDescription = state },
        verticalAlignment = verticalAlignment,
    ) {
        content()
        // A display-only Checkbox has no touch size of its own; the 48dp slot puts its centre on
        // CheckRowAnchor, where titles are centred.
        Box(Modifier.size(CheckRowAnchor * 2), contentAlignment = Alignment.Center) {
            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.popOn(checked))
        }
    }
}

/** Centre of the checkbox's 48dp touch area, measured from the top of a [CheckRow]. */
val CheckRowAnchor: Dp = 24.dp

/** Top padding that centres the first line of [style] on the [CheckRow] checkbox. */
@Composable
fun checkRowTitleInset(style: TextStyle): Dp {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    return (CheckRowAnchor - lineHeight / 2).coerceAtLeast(0.dp)
}

/** A quiet explanatory note with a leading icon. */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int = R.drawable.haho_ic_info) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(HahoTheme.dimens.smallIcon),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
