package id.haho.muslimdaily.core.designsystem.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
 */
@Composable
fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = AppTheme.dimens.minTouchTarget,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.popOn(checked))
    }
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
