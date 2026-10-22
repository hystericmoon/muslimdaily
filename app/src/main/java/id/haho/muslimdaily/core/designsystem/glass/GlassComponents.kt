package id.haho.muslimdaily.core.designsystem.glass

import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProvideTextStyle
import id.haho.muslimdaily.core.designsystem.theme.AppTheme

private val ButtonMinHeight = 48.dp

/** Primary and tonal glass buttons fade their sheen out above the label so text contrast holds. */
private const val ButtonSheenStop = 0.38f

/**
 * A glass card: translucent fill over the backdrop, a top-lit rim and a soft sheen. Pass [onClick]
 * for a tappable card; it then scales slightly while pressed.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = Glass.colors.card,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val g = Glass.colors
    val border = if (g.enabled) g.rimStroke() else null
    if (onClick == null) {
        Surface(
            modifier = modifier.glassFill(shape, color, g),
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColor,
            border = border,
            content = content,
        )
    } else {
        val source = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(source, pressed = 0.985f).glassFill(shape, color, g),
            shape = shape,
            color = Color.Transparent,
            contentColor = contentColor,
            border = border,
            interactionSource = source,
            content = content,
        )
    }
}

@Composable
private fun GlassButtonBase(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Color,
    contentColor: Color,
    sheen: Color,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit,
) {
    val g = Glass.colors
    val source = remember { MutableInteractionSource() }
    val disabledFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Surface(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .pressScale(source)
            .glassFill(
                shape,
                if (enabled) fill else disabledFill,
                g,
                sheen = if (enabled) sheen else Color.Transparent,
                sheenStop = ButtonSheenStop,
            ),
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = if (enabled) contentColor else disabledContent,
        border = if (enabled) border else null,
        interactionSource = source,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                Modifier
                    .defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = ButtonMinHeight)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Primary action: frosted teal glass. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val g = Glass.colors
    GlassButtonBase(
        onClick, modifier, enabled,
        fill = g.primary,
        contentColor = g.onPrimary,
        sheen = g.sheen.copy(alpha = g.sheen.alpha * 0.6f),
        border = if (g.enabled) g.rimStroke() else null,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Secondary action: soft tinted glass. */
@Composable
fun GlassTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val g = Glass.colors
    GlassButtonBase(
        onClick, modifier, enabled,
        fill = g.tonal,
        contentColor = g.onTonal,
        sheen = g.sheen,
        border = if (g.enabled) g.rimStroke() else null,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Low-emphasis action: clear glass with an outline. */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val g = Glass.colors
    GlassButtonBase(
        onClick, modifier, enabled,
        fill = g.clear,
        contentColor = MaterialTheme.colorScheme.primary,
        sheen = g.sheen,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = contentPadding,
        content = content,
    )
}

/** Round primary glass button for play / pause. Size it with [modifier]. */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val g = Glass.colors
    GlassButtonBase(
        onClick, modifier, enabled,
        fill = g.primary,
        contentColor = g.onPrimary,
        sheen = g.sheen.copy(alpha = g.sheen.alpha * 0.6f),
        border = if (g.enabled) g.rimStroke(1.5.dp) else null,
        contentPadding = PaddingValues(0.dp),
    ) { content() }
}

/**
 * Extended floating action button in primary glass. The label collapses (animated) when
 * [expanded] is false, e.g. while the list scrolls.
 */
@Composable
fun GlassExtendedFab(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val g = Glass.colors
    GlassButtonBase(
        onClick, modifier.defaultMinSize(minHeight = 56.dp), true,
        fill = g.primary,
        contentColor = g.onPrimary,
        sheen = g.sheen.copy(alpha = g.sheen.alpha * 0.6f),
        border = if (g.enabled) g.rimStroke() else null,
        contentPadding = PaddingValues(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        icon()
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(AppTheme.motion.fade()) + expandHorizontally(AppTheme.motion.layout()),
            exit = fadeOut(AppTheme.motion.fade()) + shrinkHorizontally(AppTheme.motion.layout()),
        ) {
            Row {
                Spacer(Modifier.width(12.dp))
                text()
            }
        }
    }
}

/** Registers an open overlay so the app behind it frosts. */
@Composable
fun TrackOverlay() {
    val overlays = LocalOverlayState.current
    DisposableEffect(overlays) {
        overlays.open++
        onDispose { overlays.open-- }
    }
}

/** Bottom sheet in frosted glass; the screen behind blurs while it is open (Android 12+). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    TrackOverlay()
    val g = Glass.colors
    val shape = MaterialTheme.shapes.extraLarge
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = if (g.enabled) modifier.border(g.rimStroke(), shape) else modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = g.sheet,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

/** Alert dialog in frosted glass; the screen behind blurs while it is open (Android 12+). */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    TrackOverlay()
    val g = Glass.colors
    val shape = AlertDialogDefaults.shape
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = if (g.enabled) modifier.border(g.rimStroke(), shape) else modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = shape,
        containerColor = g.sheet,
    )
}

/** Segmented buttons on glass: clear when unselected so the card shows through. */
@Composable
fun glassSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = Glass.colors.tinted,
    activeContentColor = Glass.colors.onTinted,
    inactiveContainerColor = Color.Transparent,
)
