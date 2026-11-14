package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Box
import id.haho.design.glass.enterReveal
import id.haho.design.glass.Glass
import id.haho.design.glass.GlassAlertDialog
import id.haho.design.glass.GlassExtendedFab
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.core.ui.formatDayMonth
import androidx.compose.foundation.lazy.LazyColumn
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
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.EmptyState
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.domain.memorize.Leitner
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Where a dua stands, in the order the list shows them: what needs the reader first. */
enum class HifzStatus { DUE, LEARNING, NOT_STARTED, MEMORIZED }

data class HifzEntry(val item: ReviewItem, val dua: Dua, val eventLabel: String, val due: Boolean) {
    val status: HifzStatus
        get() = when {
            due -> HifzStatus.DUE
            item.notStarted -> HifzStatus.NOT_STARTED
            item.memorized -> HifzStatus.MEMORIZED
            else -> HifzStatus.LEARNING
        }
}

data class MemorizeHomeState(val today: LocalDate, val entries: List<HifzEntry>) {
    val dueEntries: List<HifzEntry> get() = sorted.filter { it.due }

    /** Due first, then learning, not started and memorized; within each, the nearest review first. */
    val sorted: List<HifzEntry>
        get() = entries.sortedWith(compareBy<HifzEntry>({ it.status.ordinal }, { it.item.state.due }, { it.eventLabel }))

    /** What "Continue" opens: the first due review, else the first dua not started yet. */
    val next: HifzEntry? get() = dueEntries.firstOrNull() ?: sorted.firstOrNull { it.status == HifzStatus.NOT_STARTED }
}

@HiltViewModel
class MemorizeHomeViewModel @Inject constructor(
    private val reviews: ReviewRepository,
    private val content: ContentRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)

    val state: StateFlow<MemorizeHomeState?> = combine(reviews.items(), content.allDuas().map { list -> list.filter { it.isRecitable } }, content.categories()) { items, duas, cats ->
        val byId = duas.associateBy { it.id }
        val labels = cats.flatMap { it.events }.associate { it.id to it.label }
        val twins = content.twinsOnce()
        MemorizeHomeState(
            today = today,
            entries = items.mapNotNull { item ->
                val dua = byId[item.duaId] ?: return@mapNotNull null
                // One entry stands for its twins too, e.g. "Dzikir pagi, Dzikir petang" (a comma, so
                // TalkBack pauses between the two).
                val label = (twins[dua.id] ?: listOf(dua.id)).mapNotNull { byId[it]?.event }.distinct()
                    .mapNotNull { labels[it] }.joinToString(", ")
                HifzEntry(item, dua, label, !item.notStarted && Leitner.isDue(item.state, today))
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
    AdwamScreen(
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
        contentPadding = PaddingValues(start = HahoTheme.dimens.screenGutter, end = HahoTheme.dimens.screenGutter, bottom = HahoTheme.spacing.xxxl * 2),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        item(key = "queue") { Box(Modifier.enterReveal(0)) { ContinueCard(state, onOpen) } }
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
                    modifier = Modifier.padding(top = HahoTheme.spacing.md).semantics { heading() },
                )
            }
            item(key = "rows") {
                GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.sorted.forEachIndexed { i, entry ->
                            key(entry.item.duaId) {
                                if (i > 0) HorizontalDivider(Modifier.padding(start = HahoTheme.spacing.lg + StatusDot + HahoTheme.spacing.md))
                                HifzRow(entry, onRemove = { onRemove(entry.dua.id) }) { onOpen(entry.dua.id, entry.due) }
                            }
                        }
                    }
                }
            }
        }
        item(key = "note") { InfoNote(stringResource(R.string.memorize_no_streaks)) }
    }
}

@Composable
private fun ContinueCard(state: MemorizeHomeState, onOpen: (String, Boolean) -> Unit) {
    val next = state.next
    GlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        color = Glass.colors.tinted,
        contentColor = Glass.colors.onTinted,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(HahoTheme.dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            if (next == null) {
                Text(stringResource(R.string.memorize_review_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.memorize_review_none), style = MaterialTheme.typography.titleLarge)
            } else {
                Text(stringResource(R.string.memorize_continue), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(next.eventLabel, style = MaterialTheme.typography.titleLarge)
                    val due = state.dueEntries.size
                    Text(
                        if (next.due) pluralStringResource(R.plurals.memorize_review_due, due, due) else stringResource(R.string.memorize_not_started),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                GlassButton(onClick = { onOpen(next.dua.id, next.due) }) {
                    Text(stringResource(if (next.due) R.string.memorize_review_start else R.string.memorize_start_learning))
                }
            }
        }
    }
}

private val StatusDot = 32.dp

/**
 * One dua: its state as an icon and in words, so nothing rests on colour alone. Removing sits
 * behind the overflow menu and a confirmation, away from the row's main tap.
 */
@Composable
private fun HifzRow(entry: HifzEntry, onRemove: () -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val status = statusText(entry)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.memorize_open), onClick = onClick)
            .padding(start = HahoTheme.spacing.lg, end = HahoTheme.spacing.xs, top = HahoTheme.spacing.sm, bottom = HahoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (container, content, icon) = when (entry.status) {
            HifzStatus.DUE -> Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, R.drawable.ic_event_repeat)
            HifzStatus.LEARNING -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, R.drawable.ic_schedule)
            HifzStatus.NOT_STARTED -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_record_voice_over)
            HifzStatus.MEMORIZED -> Triple(AdwamTheme.colors.completeFill, AdwamTheme.colors.onCompleteFill, R.drawable.ic_check)
        }
        Surface(shape = CircleShape, color = container, contentColor = content, modifier = Modifier.size(StatusDot)) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, Modifier.size(HahoTheme.dimens.smallIcon)) }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = HahoTheme.spacing.md)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(entry.eventLabel, style = MaterialTheme.typography.titleSmall)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    painterResource(R.drawable.ic_more_vert),
                    stringResource(R.string.memorize_more_named, entry.eventLabel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.memorize_remove_menu)) },
                    onClick = { menu = false; confirmRemove = true },
                )
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

@Composable
private fun statusText(entry: HifzEntry): String = when (entry.status) {
    HifzStatus.DUE -> stringResource(R.string.memorize_due_badge)
    HifzStatus.NOT_STARTED -> stringResource(R.string.memorize_not_started)
    HifzStatus.LEARNING -> stringResource(R.string.memorize_state_learning, formatDayMonth(entry.item.state.due))
    HifzStatus.MEMORIZED -> stringResource(R.string.memorize_state_memorized, formatDayMonth(entry.item.state.due))
}
