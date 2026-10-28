package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import id.haho.design.component.CheckRow
import id.haho.design.theme.HahoTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.haho.design.component.Badge
import id.haho.design.component.HahoScreen
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.Tier

/**
 * The shared [HahoScreen] with Adwam's own back label, so it follows Adwam's language (Indonesian
 * by default) rather than the library's.
 */
@Composable
fun AdwamScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) = HahoScreen(
    title = title,
    modifier = modifier,
    onBack = onBack,
    backContentDescription = stringResource(R.string.action_back),
    actions = actions,
    snackbarHostState = snackbarHostState,
    floatingActionButton = floatingActionButton,
    bottomBar = bottomBar,
    content = content,
)

/** The shared [CheckRow] with Adwam's state labels ("Sudah dikerjakan" / "Belum dikerjakan"). */
@Composable
fun AdwamCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = HahoTheme.dimens.minTouchTarget,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) = CheckRow(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    minHeight = minHeight,
    verticalAlignment = verticalAlignment,
    checkedDescription = stringResource(R.string.check_state_done),
    uncheckedDescription = stringResource(R.string.check_state_not_done),
    content = content,
)

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
