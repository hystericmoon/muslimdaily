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
    onCustomize: (String) -> Unit,
    onStart: (String) -> Unit,
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state
    AdwamScreen(
        title = s?.event?.label.orEmpty(),
        onBack = onBack,
        // Docked on the menu, one row: read now, or adjust which duas and in what order.
        bottomBar = {
            if (s != null && s.items.isNotEmpty()) {
                EventActionBar(
                    count = s.items.size,
                    onStart = { onStart(viewModel.eventId) },
                    onCustomize = { onCustomize(viewModel.eventId) },
                )
            }
        },
    ) {
        if (s != null) {
            EventContent(
                state = s,
                onUseMySet = viewModel::setUseMySet,
                onOpenDua = onOpenDua,
                onChooseForm = viewModel::chooseForm,
                onCustomize = { onCustomize(viewModel.eventId) },
                onAddToHifz = viewModel::addToHifz,
                onRemoveFromHifz = viewModel::removeFromHifz,
            )
        }
    }
}

@Composable
fun EventContent(
    state: EventUiState,
    onUseMySet: (Boolean) -> Unit,
    onOpenDua: (String) -> Unit,
    onChooseForm: (group: String, duaId: String) -> Unit,
    onCustomize: () -> Unit,
    onAddToHifz: (String) -> Unit = {},
    onRemoveFromHifz: (String) -> Unit = {},
) {
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
        // Only when the reader has their own set: say so, and how to change it.
        when {
            state.customized && state.useMySet -> item(key = "my-set") {
                InfoNote(
                    if (state.hiddenCount > 0) {
                        androidx.compose.ui.res.pluralStringResource(R.plurals.event_my_set_hidden, state.hiddenCount, state.hiddenCount)
                    } else {
                        stringResource(R.string.event_my_set_active)
                    },
                )
            }
            state.customized -> item(key = "my-set-off") {
                FlowRow(verticalArrangement = Arrangement.Center) {
                    InfoNote(stringResource(R.string.event_my_set_off), Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = { onUseMySet(true) }) { Text(stringResource(R.string.event_my_set_use)) }
                }
            }
            else -> Unit
        }
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
                header = { ItemHeader(index, state.items.size, resolved, onChooseForm) },
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
private fun ItemHeader(index: Int, total: Int, resolved: ResolvedItem, onChooseForm: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Text(
            stringResource(R.string.event_item_position, index + 1, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
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
fun EventActionBar(count: Int, onStart: () -> Unit, onCustomize: () -> Unit) {
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
        GlassTonalButton(onClick = onCustomize) {
            Icon(painterResource(R.drawable.ic_tune), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Spacer(Modifier.width(HahoTheme.spacing.sm))
            Text(stringResource(R.string.event_adjust), maxLines = 1)
        }
    }
}
