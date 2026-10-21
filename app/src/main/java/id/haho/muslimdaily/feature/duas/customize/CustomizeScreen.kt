package id.haho.muslimdaily.feature.duas.customize

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SectionHeader
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.domain.myset.FormChoice
import id.haho.muslimdaily.domain.myset.ResolvedItem
import id.haho.muslimdaily.domain.myset.SetItem

data class CustomizeActions(
    val onToggle: (key: String, enabled: Boolean) -> Unit = { _, _ -> },
    val onReorder: (keys: List<String>) -> Unit = {},
    val onMove: (key: String, delta: Int) -> Unit = { _, _ -> },
    val onChoose: (group: String, choice: FormChoice) -> Unit = { _, _ -> },
    val onReset: () -> Unit = {},
)

@Composable
fun CustomizeRoute(onBack: () -> Unit, viewModel: CustomizeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    AppScreen(
        title = stringResource(R.string.customize_title, state?.event?.label.orEmpty()),
        onBack = onBack,
        actions = {
            TextButton(onClick = { confirmReset = true }, enabled = state?.isCustomized == true) {
                Text(stringResource(R.string.customize_reset), maxLines = 1)
            }
        },
    ) {
        state?.let {
            CustomizeContent(
                it,
                CustomizeActions(
                    onToggle = viewModel::setEnabled,
                    onReorder = viewModel::reorder,
                    onMove = viewModel::move,
                    onChoose = viewModel::choose,
                ),
            )
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.customize_reset_title)) },
            text = { Text(stringResource(R.string.customize_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToFullSet()
                    confirmReset = false
                }) { Text(stringResource(R.string.customize_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
fun CustomizeContent(state: CustomizeUiState, actions: CustomizeActions, initiallyShowHidden: Boolean = false) {
    // Local order while dragging; persisted when the drag ends.
    var order by remember(state.shown.map { it.item.key }) { mutableStateOf(state.shown) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    var showHidden by remember { mutableStateOf(initiallyShowHidden) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl),
    ) {
        item(key = "intro") {
            InfoNote(
                stringResource(R.string.customize_intro),
                Modifier.padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
            )
        }
        itemsIndexed(order, key = { _, it -> it.item.key }) { index, resolved ->
            val key = resolved.item.key
            val dragging = draggingKey == key
            CustomizeRow(
                index = index,
                count = order.size,
                resolved = resolved,
                choice = (resolved.item as? SetItem.Variants)?.let { state.choices[it.group] },
                actions = actions,
                dragging = dragging,
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .onSizeChanged { heights[key] = it.height },
                handleModifier = Modifier.pointerInput(key) {
                    detectDragGestures(
                        onDragStart = {
                            draggingKey = key
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val i = order.indexOfFirst { it.item.key == key }
                            if (dragOffset > 0 && i < order.lastIndex) {
                                val next = heights[order[i + 1].item.key] ?: return@detectDragGestures
                                if (dragOffset > next / 2f) {
                                    order = order.toMutableList().apply { add(i + 1, removeAt(i)) }
                                    dragOffset -= next
                                }
                            } else if (dragOffset < 0 && i > 0) {
                                val prev = heights[order[i - 1].item.key] ?: return@detectDragGestures
                                if (-dragOffset > prev / 2f) {
                                    order = order.toMutableList().apply { add(i - 1, removeAt(i)) }
                                    dragOffset += prev
                                }
                            }
                        },
                        onDragEnd = {
                            draggingKey = null
                            dragOffset = 0f
                            actions.onReorder(order.map { it.item.key })
                        },
                        onDragCancel = {
                            draggingKey = null
                            dragOffset = 0f
                        },
                    )
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (state.hidden.isNotEmpty()) {
            item(key = "hidden-header") {
                val label = pluralStringResource(R.plurals.customize_hidden, state.hidden.size, state.hidden.size)
                val stateLabel = stringResource(if (showHidden) R.string.state_expanded else R.string.state_collapsed)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppTheme.dimens.largeTouchTarget)
                        .selectable(selected = false, role = Role.Button, onClick = { showHidden = !showHidden })
                        .semantics { stateDescription = stateLabel }
                        .padding(horizontal = AppTheme.dimens.screenGutter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    Icon(
                        painterResource(if (showHidden) R.drawable.ic_keyboard_arrow_up else R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                    )
                }
            }
            if (showHidden) {
                itemsIndexed(state.hidden, key = { _, it -> "hidden:" + it.item.key }) { _, resolved ->
                    HiddenRow(resolved) { actions.onToggle(resolved.item.key, true) }
                }
            }
        }
    }
}

@Composable
private fun CustomizeRow(
    index: Int,
    count: Int,
    resolved: ResolvedItem,
    choice: FormChoice?,
    actions: CustomizeActions,
    dragging: Boolean,
    modifier: Modifier,
    handleModifier: Modifier,
) {
    val key = resolved.item.key
    val moveUp = stringResource(R.string.customize_move_up)
    val moveDown = stringResource(R.string.customize_move_down)
    Surface(
        color = if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        shadowElevation = if (dragging) AppTheme.spacing.sm else 0.dp,
        modifier = modifier,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTheme.dimens.largeTouchTarget)
                    .semantics {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveUp) { actions.onMove(key, -1); true })
                            if (index < count - 1) add(CustomAccessibilityAction(moveDown) { actions.onMove(key, 1); true })
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    handleModifier.size(AppTheme.dimens.minTouchTarget),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_drag_indicator),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier
                        .weight(1f)
                        .toggleable(value = true, role = Role.Switch, onValueChange = { actions.onToggle(key, false) })
                        .padding(vertical = AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ItemSummary(index, resolved, Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = null, modifier = Modifier.padding(horizontal = AppTheme.spacing.md))
                }
            }
            val item = resolved.item
            if (item is SetItem.Variants) {
                VariantChoices(item, choice) { actions.onChoose(item.group, it) }
            }
        }
    }
}

@Composable
private fun ItemSummary(index: Int, resolved: ResolvedItem, modifier: Modifier = Modifier) {
    val dua = resolved.form
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        Text(
            buildString {
                append(stringResource(R.string.customize_item_n, index + 1))
                if (resolved.item is SetItem.Variants) append(" · ").append(pluralStringResource(R.plurals.customize_forms, resolved.item.forms.size, resolved.item.forms.size))
                dua.count?.let { append(" · ").append(pluralStringResource(R.plurals.dua_count, it, it)) }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        ArabicText(
            dua.phrases.firstOrNull()?.text ?: dua.arabic,
            style = AppTheme.arabic.arabicSmall,
        )
        Text(
            dua.translationId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VariantChoices(item: SetItem.Variants, choice: FormChoice?, onChoose: (FormChoice) -> Unit) {
    val options: List<Pair<FormChoice, String>> =
        item.forms.mapIndexed { i, f -> FormChoice.Fixed(f.id) to stringResource(R.string.event_form_n, i + 1) } +
            (FormChoice.Rotate(0) to stringResource(R.string.customize_rotate))
    val selected: FormChoice = when (choice) {
        is FormChoice.Rotate -> FormChoice.Rotate(0)
        is FormChoice.Fixed -> choice
        null -> FormChoice.Fixed(item.forms.first().id)
    }
    Column(
        Modifier
            .padding(start = AppTheme.dimens.minTouchTarget, bottom = AppTheme.spacing.sm)
            .selectableGroup(),
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTheme.dimens.minTouchTarget)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = {
                        // Keep the rotation position when re-selecting Rotate.
                        onChoose(if (option is FormChoice.Rotate && choice is FormChoice.Rotate) choice else option)
                    }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = AppTheme.spacing.sm))
            }
        }
        if (selected is FormChoice.Rotate) {
            InfoNote(stringResource(R.string.customize_rotate_note), Modifier.padding(end = AppTheme.dimens.screenGutter))
        }
    }
}

@Composable
private fun HiddenRow(resolved: ResolvedItem, onRestore: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.dimens.largeTouchTarget)
            .padding(start = AppTheme.dimens.screenGutter, end = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            ArabicText(resolved.form.phrases.firstOrNull()?.text ?: resolved.form.arabic, style = AppTheme.arabic.arabicSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onRestore) { Text(stringResource(R.string.customize_restore), maxLines = 1) }
    }
}
