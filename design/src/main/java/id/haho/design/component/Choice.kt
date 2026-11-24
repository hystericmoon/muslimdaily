package id.haho.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.haho.design.R
import id.haho.design.theme.HahoTheme

/**
 * A single-choice chip, or a card when [content] is set, in the [HahoFilterChip] look (outlined,
 * 8dp; selected = secondary container plus a check mark, so it never relies on colour alone).
 * Unlike [HahoFilterChip] it is a radio button for TalkBack, keeps a 48dp minimum height, can take
 * a long-press and can hold several lines. Put a row of them in a `selectableGroup()`.
 *
 * @param label one-line text; ignored when [content] is set. Wraps rather than truncating.
 * @param alignStart left-aligned card layout (taller padding), for choices with a description.
 * @param labelStyle e.g. with tabular figures for sizes and counts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HahoOptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    alignStart: Boolean = false,
    labelStyle: TextStyle = MaterialTheme.typography.labelLarge,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.small
    val scheme = MaterialTheme.colorScheme
    val spacing = HahoTheme.spacing
    val ink = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm, if (alignStart) Alignment.Start else Alignment.CenterHorizontally),
        verticalAlignment = if (content != null) Alignment.Top else Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = HahoTheme.dimens.minTouchTarget)
            .clip(shape)
            .then(if (selected) Modifier else Modifier.border(1.dp, scheme.outline, shape))
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .combinedClickable(
                enabled = enabled,
                role = Role.RadioButton,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(
                horizontal = if (alignStart) 14.dp else spacing.md,
                vertical = if (alignStart) spacing.md else 6.dp,
            ),
    ) {
        if (selected) {
            Icon(
                painterResource(R.drawable.haho_ic_check), contentDescription = null, tint = ink,
                modifier = Modifier
                    .then(if (content != null) Modifier.padding(top = 2.dp) else Modifier)
                    .size(HahoTheme.dimens.smallIcon),
            )
        }
        if (content != null) {
            CompositionLocalProvider(LocalContentColor provides ink) { Column(content = content) }
        } else if (label != null) {
            Text(label, style = labelStyle, color = ink, textAlign = TextAlign.Center)
        }
    }
}

/**
 * The track of a compact segmented control: an outlined pill holding [HahoSegment]s. Unlike
 * Material's segmented buttons it has no glass fill, so it also works on plain dark screens such
 * as photo editors. Give each segment `Modifier.weight(1f)` for equal widths.
 */
@Composable
fun HahoSegmentTrack(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(HahoTheme.spacing.xs)
            .selectableGroup(),
        content = content,
    )
}

/**
 * One segment of a [HahoSegmentTrack]: 48dp tall; selected = secondary container plus a check
 * mark. TalkBack reads it as a tab with its selected state. The label wraps.
 */
@Composable
fun HahoSegment(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val spacing = HahoTheme.spacing
    val ink = if (selected) scheme.onSecondaryContainer else scheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = HahoTheme.dimens.minTouchTarget)
            .clip(CircleShape)
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(spacing.sm),
    ) {
        if (selected) {
            Icon(painterResource(R.drawable.haho_ic_check), contentDescription = null, tint = ink, modifier = Modifier.size(HahoTheme.dimens.smallIcon))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink, textAlign = TextAlign.Center)
    }
}

@HahoPreviews
@Composable
private fun ChoicePreview() = HahoPreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            HahoOptionChip(selected = true, onClick = {}, label = "1 MB")
            HahoOptionChip(selected = false, onClick = {}, label = "2 MB")
        }
        HahoSegmentTrack {
            HahoSegment("Before", selected = false, onClick = {}, modifier = Modifier.weight(1f))
            HahoSegment("After", selected = true, onClick = {}, modifier = Modifier.weight(1f))
        }
    }
}
