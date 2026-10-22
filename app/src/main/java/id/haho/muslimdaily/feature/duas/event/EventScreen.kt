package id.haho.muslimdaily.feature.duas.event

import id.haho.muslimdaily.core.designsystem.glass.enterReveal
import id.haho.muslimdaily.core.designsystem.glass.GlassExtendedFab
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.domain.myset.ResolvedItem
import id.haho.muslimdaily.domain.myset.SetItem
import id.haho.muslimdaily.feature.duas.components.DuaCard

@Composable
fun EventRoute(
    onBack: () -> Unit,
    onOpenDua: (String) -> Unit,
    onCustomize: (String) -> Unit,
    onStart: (String) -> Unit,
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state
    AppScreen(
        title = s?.event?.label.orEmpty(),
        onBack = onBack,
        actions = {
            IconButton(onClick = { onCustomize(viewModel.eventId) }) {
                Icon(painterResource(R.drawable.ic_tune), stringResource(R.string.event_customize))
            }
        },
        floatingActionButton = {
            if (s != null && s.items.isNotEmpty()) {
                GlassExtendedFab(
                    onClick = { onStart(viewModel.eventId) },
                    icon = { Icon(painterResource(R.drawable.ic_play_arrow), null) },
                    text = { Text(stringResource(R.string.event_start)) },
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
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppTheme.dimens.screenGutter,
            end = AppTheme.dimens.screenGutter,
            bottom = AppTheme.spacing.xxxl * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        item(key = "mode") {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                val options = listOf(false to R.string.event_full_set, true to R.string.event_my_set)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = state.useMySet == value,
                            onClick = { onUseMySet(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            label = { Text(stringResource(label), maxLines = 1) },
                        )
                    }
                }
                when {
                    state.useMySet && !state.customized -> InfoNote(stringResource(R.string.event_my_set_empty_hint))
                    state.useMySet && state.hiddenCount > 0 -> FlowRow(verticalArrangement = Arrangement.Center) {
                        InfoNote(
                            androidx.compose.ui.res.pluralStringResource(R.plurals.event_hidden, state.hiddenCount, state.hiddenCount),
                            Modifier.align(Alignment.CenterVertically),
                        )
                        TextButton(onClick = onCustomize) { Text(stringResource(R.string.event_customize)) }
                    }
                    !state.useMySet -> InfoNote(stringResource(R.string.event_full_set_hint))
                }
            }
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
                modifier = Modifier.animateItem().enterReveal(index),
            )
        }
    }
}

@Composable
private fun ItemHeader(index: Int, total: Int, resolved: ResolvedItem, onChooseForm: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
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
