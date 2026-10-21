package id.haho.muslimdaily.core.designsystem.component

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.isLargeFontScale

/** A titled group of settings rows on a rounded container. */
@Composable
fun SettingsGroup(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        if (title != null) SectionHeader(title)
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter),
        ) {
            Column(Modifier.padding(vertical = AppTheme.spacing.xs), content = content)
        }
    }
}

private val TransparentListColors @Composable get() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(painterResource(it), null) } },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
        colors = TransparentListColors,
        modifier = modifier.selectable(selected = false, role = Role.Button, onClick = onClick),
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
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(painterResource(it), null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = TransparentListColors,
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
            ListItem(
                headlineContent = { Text(label(option)) },
                supportingContent = description?.invoke(option)?.let { { Text(it) } },
                leadingContent = { RadioButton(selected = option == selected, onClick = null) },
                colors = TransparentListColors,
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
                        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = AppTheme.spacing.sm))
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
                    label = { Text(text, maxLines = 1) },
                    modifier = Modifier.semantics { contentDescription = "$title: $text" },
                )
            }
        }
    }
}
