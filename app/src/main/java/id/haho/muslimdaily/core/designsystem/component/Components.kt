package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.layout.Box
import id.haho.muslimdaily.core.designsystem.glass.popOn
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.Tier

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
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(AppTheme.dimens.smallIcon))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    when (tier) {
        Tier.MUAKKADAH -> Badge(
            text = stringResource(R.string.tier_muakkadah),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = modifier,
        )
        Tier.GHAIRU_MUAKKADAH -> Badge(
            text = stringResource(R.string.tier_ghairu),
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

/**
 * A whole-row toggle with a trailing checkbox. The row is the touch target (≥ 48dp) and TalkBack
 * reads it as one checkbox with [label].
 *
 * The checkbox sits at the top, so it stays level with the row's title however many lines
 * (rak'ah count, badges, notes) follow. Pad the title with [checkRowTitleInset] to centre its
 * first line on the checkbox.
 */
@Composable
fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = AppTheme.dimens.minTouchTarget,
    /** Top puts the checkbox level with the first line; CenterVertically centres it on the whole item. */
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    // TalkBack says "checked" but stays silent when unchecked, so both states are named.
    val state = stringResource(if (checked) R.string.check_state_done else R.string.check_state_not_done)
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
        // CheckRowAnchor, where titles (and timeline dots) are centred.
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
fun InfoNote(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int = R.drawable.ic_info) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.smallIcon),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
