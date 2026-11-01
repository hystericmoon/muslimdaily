package id.haho.muslimdaily.feature.routines

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.component.InfoNote
import id.haho.design.component.SectionHeader
import id.haho.design.glass.Glass
import id.haho.design.glass.GlassAlertDialog
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassCard
import id.haho.design.glass.GlassTonalButton
import id.haho.design.glass.chevronRotation
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.design.component.HahoTextField

class RoutineEditActions(
    val onRename: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onMove: (String, Int) -> Unit = { _, _ -> },
    val onReorder: (List<String>) -> Unit = {},
    val onPick: (Boolean) -> Unit = {},
    val onToggle: (Dua) -> Unit = {},
    val onToggleEvent: (String) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun RoutineEditRoute(onBack: () -> Unit, viewModel: RoutineEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    // In the picker, back returns to the routine rather than leaving it.
    BackHandler(enabled = s.picking) { viewModel.setPicking(false) }
    RoutineEditScreen(
        state = s,
        onBack = if (s.picking) ({ viewModel.setPicking(false) }) else onBack,
        actions = RoutineEditActions(
            onRename = viewModel::rename,
            onRemove = viewModel::remove,
            onMove = viewModel::move,
            onReorder = viewModel::reorder,
            onPick = viewModel::setPicking,
            onToggle = viewModel::toggle,
            onToggleEvent = viewModel::toggleEvent,
            onSearch = viewModel::search,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        ),
    )
}

@Composable
fun RoutineEditScreen(state: RoutineEditState, onBack: () -> Unit, actions: RoutineEditActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    AdwamScreen(
        title = stringResource(
            when {
                state.picking -> R.string.routine_pick_title
                state.isNew -> R.string.routine_new_title
                else -> R.string.routine_edit_title
            },
        ),
        onBack = onBack,
        actions = {
            if (state.picking) {
                TextButton(onClick = { actions.onPick(false) }) { Text(stringResource(R.string.routine_pick_done)) }
            } else {
                TextButton(onClick = actions.onSave, enabled = state.canSave) { Text(stringResource(R.string.routine_save)) }
            }
        },
    ) {
        if (state.picking) PickerContent(state, actions) else EditorContent(state, actions, onDelete = { confirmDelete = true })
    }
    if (confirmDelete) {
        GlassAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.routine_delete_title, state.name)) },
            text = { Text(stringResource(R.string.routine_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; actions.onDelete() }) { Text(stringResource(R.string.routine_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun EditorContent(state: RoutineEditState, actions: RoutineEditActions, onDelete: () -> Unit) {
    val gutter = HahoTheme.dimens.screenGutter
    var order by remember(state.items.map { it.id }) { mutableStateOf(state.items) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val labels = state.eventLabels

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item(key = "name") {
            Column(Modifier.padding(horizontal = gutter, vertical = HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                HahoTextField(
                    value = state.name,
                    onValueChange = actions.onRename,
                    label = { Text(stringResource(R.string.routine_name)) },
                    placeholder = { Text(stringResource(R.string.routine_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                InfoNote(stringResource(R.string.routine_intro))
            }
        }
        item(key = "list-header") {
            SectionHeader(pluralStringResource(R.plurals.routine_count, order.size, order.size))
        }
        itemsIndexed(order, key = { _, it -> it.id }) { index, dua ->
            val dragging = draggingId == dua.id
            RoutineRow(
                index = index,
                count = order.size,
                dua = dua,
                eventLabel = labels[dua.event].orEmpty(),
                dragging = dragging,
                actions = actions,
                modifier = Modifier
                    .animateItem(placementSpec = if (dragging) null else HahoTheme.motion.layout())
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .onSizeChanged { heights[dua.id] = it.height },
                handleModifier = Modifier.pointerInput(dua.id) {
                    detectDragGestures(
                        onDragStart = { draggingId = dua.id; dragOffset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val i = order.indexOfFirst { it.id == dua.id }
                            if (dragOffset > 0 && i < order.lastIndex) {
                                val next = heights[order[i + 1].id] ?: return@detectDragGestures
                                if (dragOffset > next / 2f) {
                                    order = order.toMutableList().apply { add(i + 1, removeAt(i)) }
                                    dragOffset -= next
                                }
                            } else if (dragOffset < 0 && i > 0) {
                                val prev = heights[order[i - 1].id] ?: return@detectDragGestures
                                if (-dragOffset > prev / 2f) {
                                    order = order.toMutableList().apply { add(i - 1, removeAt(i)) }
                                    dragOffset += prev
                                }
                            }
                        },
                        onDragEnd = { draggingId = null; dragOffset = 0f; actions.onReorder(order.map { it.id }) },
                        onDragCancel = { draggingId = null; dragOffset = 0f },
                    )
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.animateItem())
        }
        item(key = "add") {
            Column(Modifier.padding(horizontal = gutter, vertical = HahoTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                GlassTonalButton(onClick = { actions.onPick(true) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(HahoTheme.dimens.smallIcon))
                    Spacer(Modifier.width(HahoTheme.spacing.sm))
                    Text(stringResource(R.string.routine_add))
                }
                GlassButton(onClick = actions.onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.routine_save))
                }
                if (!state.canSave) {
                    InfoNote(stringResource(if (state.items.isEmpty()) R.string.routine_need_duas else R.string.routine_need_name))
                }
                if (!state.isNew) {
                    TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(
    index: Int,
    count: Int,
    dua: Dua,
    eventLabel: String,
    dragging: Boolean,
    actions: RoutineEditActions,
    modifier: Modifier,
    handleModifier: Modifier,
) {
    val moveUp = stringResource(R.string.customize_move_up)
    val moveDown = stringResource(R.string.customize_move_down)
    val lift by animateFloatAsState(if (dragging) 1f else 0f, HahoTheme.motion.press(), label = "lift")
    Surface(
        color = if (dragging) Glass.colors.sheet else Color.Transparent,
        shadowElevation = HahoTheme.spacing.sm * lift,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                .semantics {
                    customActions = buildList {
                        if (index > 0) add(CustomAccessibilityAction(moveUp) { actions.onMove(dua.id, -1); true })
                        if (index < count - 1) add(CustomAccessibilityAction(moveDown) { actions.onMove(dua.id, 1); true })
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(handleModifier.size(HahoTheme.dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_drag_indicator), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DuaSummary(index + 1, dua, eventLabel, Modifier.weight(1f).padding(vertical = HahoTheme.spacing.sm))
            IconButton(onClick = { actions.onRemove(dua.id) }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.routine_remove))
            }
        }
    }
}

/** Number, occasion and the start of the translation: enough to tell duas apart in a list. */
@Composable
private fun DuaSummary(number: Int?, dua: Dua, eventLabel: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xxs)) {
        Text(
            listOfNotNull(number?.let { "$it." }, eventLabel.ifBlank { null }).joinToString(" "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            dua.translationId.ifBlank { dua.arabic },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PickerContent(state: RoutineEditState, actions: RoutineEditActions) {
    val gutter = HahoTheme.dimens.screenGutter
    val selected = state.selectedIds
    val labels = state.eventLabels
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        item(key = "search") {
            Column(Modifier.padding(horizontal = gutter, vertical = HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                HahoTextField(
                    value = state.query,
                    onValueChange = actions.onSearch,
                    label = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    pluralStringResource(R.plurals.routine_selected, selected.size, selected.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        val results = state.results
        if (results != null) {
            if (results.isEmpty()) {
                item(key = "none") { InfoNote(stringResource(R.string.search_empty_title), Modifier.padding(gutter)) }
            }
            items(results, key = { "r:" + it.id }) { dua ->
                PickRow(dua, labels[dua.event].orEmpty(), state.prefs, dua.id in selected) { actions.onToggle(dua) }
            }
        } else {
            state.categories.forEach { category ->
                item(key = "c:" + category.id) { SectionHeader(category.label) }
                item(key = "cc:" + category.id) {
                    GlassCard(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                        Column {
                            category.events.forEachIndexed { i, event ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val open = event.id in state.expanded
                                val picked = state.eventDuas[event.id].orEmpty().count { it.id in selected }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                                        .toggleable(value = open, role = Role.Button, onValueChange = { actions.onToggleEvent(event.id) })
                                        .padding(horizontal = gutter, vertical = HahoTheme.spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(event.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { heading() })
                                        Text(
                                            if (picked > 0) pluralStringResource(R.plurals.routine_selected, picked, picked) else pluralStringResource(R.plurals.duas_in_event, event.duaCount, event.duaCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (picked > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(painterResource(R.drawable.ic_keyboard_arrow_down), null, Modifier.chevronRotation(open))
                                }
                                if (open) {
                                    state.eventDuas[event.id].orEmpty().forEach { dua ->
                                        PickRow(dua, "", state.prefs, dua.id in selected) { actions.onToggle(dua) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickRow(dua: Dua, eventLabel: String, prefs: ReadingPrefs, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = HahoTheme.dimens.largeTouchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(HahoTheme.spacing.md))
        PickSummary(dua, eventLabel, prefs, Modifier.weight(1f))
    }
}

/** Occasion, the start of the Arabic and the translation (and transliteration, if on): enough to choose by. */
@Composable
private fun PickSummary(dua: Dua, eventLabel: String, prefs: ReadingPrefs, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        if (eventLabel.isNotBlank()) {
            Text(eventLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        ArabicText(dua.arabic, style = AdwamTheme.arabic.body(dua.isQuran), maxLines = PICK_ARABIC_LINES)
        DuaTranslations(dua, prefs, maxLines = PICK_TRANSLATION_LINES)
    }
}

private const val PICK_ARABIC_LINES = 2
private const val PICK_TRANSLATION_LINES = 2
