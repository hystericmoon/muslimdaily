package id.haho.muslimdaily.feature.duas.event

import id.haho.design.glass.glassSegmentedColors
import id.haho.design.component.isLargeFontScale
import androidx.compose.ui.text.style.TextAlign
import id.haho.design.glass.enterReveal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import id.haho.design.glass.GlassTonalButton
import id.haho.design.glass.DockedBar
import id.haho.design.glass.GlassButton
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.feature.routines.RoutineSheet
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import id.haho.design.glass.GlassOutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.EmptyState
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.domain.myset.ResolvedItem
import id.haho.muslimdaily.domain.myset.SetItem
import id.haho.muslimdaily.feature.duas.components.DuaCard

@Composable
fun EventRoute(
    /** Null in the list-detail pane, where there is nothing to go back to. */
    onBack: (() -> Unit)?,
    onOpenDua: (String) -> Unit,
    onStart: (String) -> Unit,
    /** The routine editor, for a new routine that starts with this dua. */
    onNewRoutine: (String) -> Unit = {},
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    sheetFor?.let { id ->
        RoutineSheet(
            duaId = id,
            routines = s?.routines.orEmpty(),
            onSetIncluded = { routineId, included -> viewModel.setInRoutine(routineId, id, included) },
            onNew = { onNewRoutine(id) },
            onDismiss = { sheetFor = null },
        )
    }
    // With no routine the first bookmark makes "Doa saya"; with one it toggles there and says
    // where (Ubah opens the picker); with several it asks which.
    val onBookmark: (String) -> Unit = bookmark@{ id ->
        val routines = s?.routines ?: return@bookmark
        val message = when (routines.size) {
            0 -> {
                val name = resources.getString(R.string.routine_default_name)
                viewModel.startRoutine(name, id)
                resources.getString(R.string.bookmark_added, name)
            }
            1 -> {
                val r = routines.single()
                val included = id !in r.duaIds
                viewModel.setInRoutine(r.id, id, included)
                resources.getString(if (included) R.string.bookmark_added else R.string.bookmark_removed, r.name)
            }
            else -> {
                sheetFor = id
                null
            }
        }
        if (message != null) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(message, actionLabel = resources.getString(R.string.bookmark_change), duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) sheetFor = id
            }
        }
    }
    AdwamScreen(
        title = s?.event?.label.orEmpty(),
        onBack = onBack,
        snackbarHostState = snackbar,
        // Docked on the menu: read the occasion's duas one after another.
        bottomBar = {
            if (s != null && s.items.isNotEmpty()) {
                EventActionBar(
                    count = s.items.size,
                    onStart = { onStart(viewModel.eventId) },
                )
            }
        },
    ) {
        if (s != null) {
            EventContent(
                state = s,
                onOpenDua = onOpenDua,
                onChooseForm = viewModel::chooseForm,
                onAddToHifz = viewModel::addToHifz,
                onRemoveFromHifz = viewModel::removeFromHifz,
                onBookmark = onBookmark,
            )
        }
    }
}

@Composable
fun EventContent(
    state: EventUiState,
    onOpenDua: (String) -> Unit,
    onChooseForm: (group: String, duaId: String) -> Unit,
    onAddToHifz: (String) -> Unit = {},
    onRemoveFromHifz: (String) -> Unit = {},
    onBookmark: (String) -> Unit = {},
) {
    val saved = remember(state.routines) { state.routines.flatMap { it.duaIds }.toSet() }
    // Removing drops the dua's review schedule, so it is confirmed as on the Hafalan page.
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }
    confirmRemove?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.memorize_remove_title)) },
            text = { Text(stringResource(R.string.memorize_remove_body)) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = null; onRemoveFromHifz(id) }) { Text(stringResource(R.string.memorize_remove)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HahoTheme.dimens.screenGutter,
            end = HahoTheme.dimens.screenGutter,
            bottom = HahoTheme.spacing.xxxl * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        if (state.items.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = R.drawable.ic_inbox,
                    title = stringResource(R.string.event_empty_title),
                    body = stringResource(R.string.event_empty_body),
                )
            }
        }
        itemsIndexed(state.items, key = { _, it -> it.item.key }) { index, resolved ->
            DuaCard(
                dua = resolved.form,
                prefs = state.prefs,
                onClick = { onOpenDua(resolved.form.id) },
                header = {
                    val id = resolved.form.id
                    ItemHeader(index, state.items.size, resolved, onChooseForm, saved = id in saved, onBookmark = { onBookmark(id) })
                },
                footer = {
                    val id = resolved.form.id
                    HifzButton(
                        inHifz = id in state.hifz,
                        onClick = { if (id in state.hifz) confirmRemove = id else onAddToHifz(id) },
                    )
                },
                modifier = Modifier.animateItem().enterReveal(index),
            )
        }
    }
}

@Composable
private fun ItemHeader(
    index: Int,
    total: Int,
    resolved: ResolvedItem,
    onChooseForm: (String, String) -> Unit,
    saved: Boolean = false,
    onBookmark: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.event_item_position, index + 1, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // A toggle that says whether the dua is in a routine; the snackbar or sheet says which.
            val savedLabel = stringResource(if (saved) R.string.bookmark_state_saved else R.string.bookmark_state_not_saved)
            IconToggleButton(
                checked = saved,
                onCheckedChange = { onBookmark() },
                modifier = Modifier.semantics { stateDescription = savedLabel },
            ) {
                Icon(
                    painterResource(if (saved) R.drawable.ic_bookmark_added_fill else R.drawable.ic_bookmark_add),
                    contentDescription = stringResource(R.string.bookmark_label),
                    tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val item = resolved.item
        if (item is SetItem.Variants) {
            Text(
                stringResource(R.string.event_variants_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                item.forms.forEachIndexed { i, form ->
                    FilterChip(
                        selected = form.id == resolved.form.id,
                        onClick = { onChooseForm(item.group, form.id) },
                        label = { Text(stringResource(R.string.event_form_n, i + 1)) },
                        leadingIcon = if (form.id == resolved.form.id) {
                            { Icon(painterResource(R.drawable.ic_check), null) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** Adds the dua to "My hifz", or once there, says so (tapping again offers to remove it). */
@Composable
private fun HifzButton(inHifz: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            painterResource(if (inHifz) R.drawable.ic_check else R.drawable.ic_record_voice_over),
            null,
            Modifier.size(HahoTheme.dimens.smallIcon),
        )
        Spacer(Modifier.width(HahoTheme.spacing.sm))
        Text(stringResource(if (inHifz) R.string.event_in_hifz else R.string.dua_memorize), maxLines = 1)
    }
    if (inHifz) {
        GlassTonalButton(onClick = onClick, modifier = modifier) { content() }
    } else {
        GlassOutlinedButton(onClick = onClick, modifier = modifier) { content() }
    }
}

@Composable
fun EventActionBar(count: Int, onStart: () -> Unit) {
    DockedBar {
        GlassButton(onClick = onStart, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Spacer(Modifier.width(HahoTheme.spacing.sm))
            // The count drops at large font sizes so the label fits the row.
            Text(
                if (isLargeFontScale()) stringResource(R.string.event_start_short) else stringResource(R.string.event_start_all, count),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
