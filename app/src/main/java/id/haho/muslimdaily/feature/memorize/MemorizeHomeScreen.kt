package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Box
import id.haho.muslimdaily.core.designsystem.glass.enterReveal
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassAlertDialog
import id.haho.muslimdaily.core.designsystem.glass.GlassExtendedFab
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.muslimdaily.core.designsystem.component.Badge
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.domain.memorize.Leitner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class HifzEntry(val item: ReviewItem, val dua: Dua, val eventLabel: String, val due: Boolean)

data class MemorizeHomeState(val today: LocalDate, val entries: List<HifzEntry>) {
    val dueEntries: List<HifzEntry> get() = entries.filter { it.due }
}

@HiltViewModel
class MemorizeHomeViewModel @Inject constructor(
    private val reviews: ReviewRepository,
    content: ContentRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)

    val state: StateFlow<MemorizeHomeState?> = combine(reviews.items(), content.allDuas(), content.categories()) { items, duas, cats ->
        val byId = duas.associateBy { it.id }
        val labels = cats.flatMap { it.events }.associate { it.id to it.label }
        MemorizeHomeState(
            today = today,
            entries = items.mapNotNull { item ->
                val dua = byId[item.duaId] ?: return@mapNotNull null
                HifzEntry(item, dua, labels[dua.event].orEmpty(), !item.notStarted && Leitner.isDue(item.state, today))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Takes a dua out of "My hifz", with its review schedule. The dua itself stays in the library. */
    fun remove(duaId: String) {
        viewModelScope.launch { reviews.remove(duaId) }
    }
}

@Composable
fun MemorizeHomeRoute(
    onOpen: (duaId: String, fromReview: Boolean) -> Unit,
    onPick: () -> Unit,
    viewModel: MemorizeHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScreen(
        title = stringResource(R.string.nav_memorize),
        floatingActionButton = {
            GlassExtendedFab(
                onClick = onPick,
                icon = { Icon(painterResource(R.drawable.ic_add), null) },
                text = { Text(stringResource(R.string.memorize_add)) },
            )
        },
    ) {
        state?.let { MemorizeHomeContent(it, onRemove = viewModel::remove, onOpen = onOpen) }
    }
}

@Composable
fun MemorizeHomeContent(state: MemorizeHomeState, onRemove: (String) -> Unit = {}, onOpen: (String, Boolean) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, bottom = AppTheme.spacing.xxxl * 2),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        item(key = "queue") { Box(Modifier.enterReveal(0)) { ReviewQueueCard(state, onOpen) } }
        if (state.entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = R.drawable.ic_record_voice_over,
                    title = stringResource(R.string.memorize_empty_title),
                    body = stringResource(R.string.memorize_empty_body),
                )
            }
        } else {
            item(key = "header") {
                Text(
                    stringResource(R.string.memorize_my_hifz),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = AppTheme.spacing.md).semantics { heading() },
                )
            }
            items(state.entries, key = { it.item.duaId }) { entry ->
                Box(Modifier.animateItem()) { HifzRow(entry, onRemove = { onRemove(entry.dua.id) }) { onOpen(entry.dua.id, entry.due) } }
            }
        }
        item(key = "note") { InfoNote(stringResource(R.string.memorize_no_streaks)) }
    }
}

@Composable
private fun ReviewQueueCard(state: MemorizeHomeState, onOpen: (String, Boolean) -> Unit) {
    val due = state.dueEntries
    GlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        color = Glass.colors.tinted,
        contentColor = Glass.colors.onTinted,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(AppTheme.dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Text(stringResource(R.string.memorize_review_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
            if (due.isEmpty()) {
                Text(stringResource(R.string.memorize_review_none), style = MaterialTheme.typography.titleLarge)
            } else {
                Text(pluralStringResource(R.plurals.memorize_review_due, due.size, due.size), style = MaterialTheme.typography.titleLarge)
                GlassButton(onClick = { onOpen(due.first().dua.id, true) }) { Text(stringResource(R.string.memorize_review_start)) }
            }
        }
    }
}

@Composable
private fun HifzRow(entry: HifzEntry, onRemove: () -> Unit, onClick: () -> Unit) {
    var confirmRemove by remember { mutableStateOf(false) }
    GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.memorize_open), onClick = onClick)
                .padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            ArabicText(entry.dua.phrases.firstOrNull()?.text ?: entry.dua.arabic, style = AppTheme.arabic.arabicSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.eventLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { confirmRemove = true }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(R.string.memorize_remove_named, entry.eventLabel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                when {
                    entry.due -> Badge(
                        stringResource(R.string.memorize_due_badge),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        icon = R.drawable.ic_event_repeat,
                    )
                    entry.item.notStarted -> Badge(
                        stringResource(R.string.memorize_not_started),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        icon = R.drawable.ic_record_voice_over,
                    )
                    entry.item.memorized -> Badge(
                        stringResource(R.string.memorize_memorized),
                        container = AppTheme.colors.completeFill,
                        content = AppTheme.colors.onCompleteFill,
                        icon = R.drawable.ic_check,
                    )
                    else -> Unit
                }
                if (!entry.item.notStarted) {
                    Badge(
                        stringResource(R.string.memorize_box_n, entry.item.state.box),
                        container = MaterialTheme.colorScheme.surfaceContainerHighest,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!entry.due && !entry.item.notStarted) {
                    Text(
                        stringResource(R.string.memorize_next_on, formatLongDate(entry.item.state.due)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
    if (confirmRemove) {
        GlassAlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.memorize_remove_title)) },
            text = { Text(stringResource(R.string.memorize_remove_body)) },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) { Text(stringResource(R.string.memorize_remove)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
