package id.haho.design.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import id.haho.design.R
import id.haho.design.theme.HahoTheme

/**
 * An outlined filter chip. Selection shows a check mark as well as the fill, so it never relies on
 * colour alone. The label wraps rather than truncating (longer languages, large font scales).
 */
@Composable
fun HahoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = when {
            selected -> { { Icon(painterResource(R.drawable.haho_ic_check), null, Modifier.size(HahoTheme.dimens.smallIcon)) } }
            icon != null -> { { Icon(painterResource(icon), null, Modifier.size(HahoTheme.dimens.smallIcon)) } }
            else -> null
        },
    )
}

/** An outlined action chip with an optional leading icon. */
@Composable
fun HahoAssistChip(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = maxLines) },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = icon?.let { { Icon(painterResource(it), null, Modifier.size(HahoTheme.dimens.smallIcon)) } },
    )
}
