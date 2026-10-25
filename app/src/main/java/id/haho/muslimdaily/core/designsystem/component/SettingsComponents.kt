package id.haho.muslimdaily.core.designsystem.component

import id.haho.muslimdaily.core.designsystem.glass.glassSegmentedColors
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.isLargeFontScale

/** A titled group of settings rows on a rounded container. */
@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        if (title != null) SectionHeader(title, titleModifier)
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter),
        ) {
            Column(Modifier.padding(vertical = AppTheme.spacing.xs), content = content)
        }
    }
}

/**
 * A settings row with Material's list-item sizes, except that the leading and trailing parts stay
 * centred however many lines the summary wraps to (Material's ListItem moves them to the top at
 * three lines, which breaks the column of icons down a group).
 */
@Composable
private fun SettingsListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (supporting == null) OneLineHeight else TwoLineHeight)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            leading?.invoke()
        }
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            trailing?.invoke()
        }
    }
}

/** Material list-item heights for one and two lines. */
private val OneLineHeight = 56.dp
private val TwoLineHeight = 72.dp

@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
) {
    SettingsListItem(
        headline = title,
        supporting = summary,
        leading = icon?.let { { Icon(painterResource(it), null) } },
        trailing = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** A read-only row: no chevron, no click, read by TalkBack as plain text. */
@Composable
fun InfoRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
) {
    SettingsListItem(
        headline = title,
        supporting = summary,
        leading = icon?.let { { Icon(painterResource(it), null) } },
        modifier = modifier.semantics(mergeDescendants = true) {},
    )
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    SettingsListItem(
        headline = title,
        supporting = summary,
        leading = icon?.let { { Icon(painterResource(it), null) } },
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A radio list; the whole row is the target and the group is announced as one. */
@Composable
fun <T> RadioGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    description: (@Composable (T) -> String?)? = null,
) {
    Column(modifier.selectableGroup()) {
        options.forEach { option ->
            SettingsListItem(
                headline = label(option),
                supporting = description?.invoke(option),
                leading = { RadioButton(selected = option == selected, onClick = null) },
                modifier = Modifier.selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
            )
        }
    }
}

/**
 * A labelled −/+ stepper. TalkBack reads the label and value, and each button names its effect.
 */
@Composable
fun StepperRow(
    title: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    val labels: @Composable (Modifier) -> Unit = { m ->
        Column(m.semantics(mergeDescendants = true) {}) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    val controls: @Composable () -> Unit = {
        StepperControls(title, valueText, onDecrement, onIncrement, decrementDescription, incrementDescription, canDecrement, canIncrement)
    }
    val padded = modifier
        .fillMaxWidth()
        .heightIn(min = AppTheme.dimens.largeTouchTarget)
        .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.xs)
    // At large font scales the label gets the full width and the controls move below it.
    if (isLargeFontScale()) {
        Column(padded, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            labels(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { controls() }
        }
    } else {
        Row(padded, verticalAlignment = Alignment.CenterVertically) {
            labels(Modifier.weight(1f))
            controls()
        }
    }
}

@Composable
private fun StepperControls(
    title: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
) {
    run {
        FilledTonalIconButton(onClick = onDecrement, enabled = canDecrement, modifier = Modifier.size(AppTheme.dimens.minTouchTarget)) {
            Icon(painterResource(R.drawable.ic_remove), decrementDescription)
        }
        Text(
            valueText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = AppTheme.spacing.xxxl + AppTheme.spacing.lg)
                .padding(horizontal = AppTheme.spacing.xs)
                .semantics {
                    contentDescription = "$title: $valueText"
                    liveRegion = LiveRegionMode.Polite
                },
        )
        FilledTonalIconButton(onClick = onIncrement, enabled = canIncrement, modifier = Modifier.size(AppTheme.dimens.minTouchTarget)) {
            Icon(painterResource(R.drawable.ic_add), incrementDescription)
        }
    }
}

@Composable
fun <T> SegmentedRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clearAndSetSemantics { })
        summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (isLargeFontScale()) {
            // Segments would truncate their labels at large font scales; a radio list never does.
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    val text = label(option)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = AppTheme.dimens.minTouchTarget)
                            .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                            .semantics { contentDescription = "$title: $text" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = AppTheme.spacing.sm).clearAndSetSemantics {})
                    }
                }
            }
            return@Column
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                val text = label(option)
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    // The segment's description is "title: option"; the visible option is not read twice.
                    label = { Text(text, maxLines = 1, modifier = Modifier.clearAndSetSemantics {}) },
                    colors = glassSegmentedColors(),
                    modifier = Modifier.semantics { contentDescription = "$title: $text" },
                )
            }
        }
    }
}
