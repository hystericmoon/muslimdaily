package id.haho.muslimdaily.feature.duas.library

import id.haho.design.glass.enterReveal
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import id.haho.muslimdaily.feature.duas.event.EventViewModel
import id.haho.muslimdaily.feature.duas.event.EventRoute
import id.haho.design.glass.Glass
import androidx.hilt.lifecycle.viewmodel.HiltViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.core.os.bundleOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.design.component.SectionHeader
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.feature.routines.RoutinesCard
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.data.routine.Routine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DuaLibraryViewModel @Inject constructor(content: ContentRepository, routines: RoutineRepository) : ViewModel() {
    val categories: StateFlow<List<DuaCategory>?> = content.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val routines: StateFlow<List<Routine>> = routines.routines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class RoutineActions(
    val onStart: (Routine) -> Unit = {},
    val onEdit: (Routine) -> Unit = {},
    val onNew: () -> Unit = {},
)

/** From this width the library and the chosen occasion sit side by side (list-detail). */
private val ListDetailWidth = 840.dp

@Composable
fun DuaLibraryRoute(
    onOpenEvent: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenDua: (String) -> Unit = {},
    onCustomize: (String) -> Unit = {},
    onStart: (String) -> Unit = {},
    /** Routine editor: (routine id or null for new, occasion to pre-fill from or null). */
    onRoutine: (String?, String?) -> Unit = { _, _ -> },
    viewModel: DuaLibraryViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= ListDetailWidth
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val shown = selected ?: categories?.firstOrNull()?.events?.firstOrNull()?.id
        val library = @Composable { modifier: Modifier ->
            AdwamScreen(
                title = stringResource(R.string.duas_title),
                modifier = modifier,
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(painterResource(R.drawable.ic_search), stringResource(R.string.action_search))
                    }
                },
            ) {
                categories?.let {
                    DuaLibraryContent(
                        it,
                        selectedId = if (twoPane) shown else null,
                        routines = routines,
                        routineActions = RoutineActions(
                            onStart = { r -> onStart(r.runKey) },
                            onEdit = { r -> onRoutine(r.id, null) },
                            onNew = { onRoutine(null, null) },
                        ),
                        onOpenEvent = if (twoPane) { id -> selected = id } else onOpenEvent,
                    )
                }
            }
        }
        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                library(Modifier.weight(0.42f))
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(0.58f)) {
                    // One view model per occasion, keyed like the EventRoute destination would be.
                    if (shown != null) key(shown) { EventPane(shown, onOpenDua, onCustomize, onStart) }
                }
            }
        } else {
            library(Modifier)
        }
    }
}

@Composable
private fun EventPane(
    eventId: String,
    onOpenDua: (String) -> Unit,
    onCustomize: (String) -> Unit,
    onStart: (String) -> Unit,
) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val defaults = owner as HasDefaultViewModelProviderFactory
    val extras = MutableCreationExtras(defaults.defaultViewModelCreationExtras)
        .apply { set(DEFAULT_ARGS_KEY, bundleOf("eventId" to eventId)) }
    val vm: EventViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "event-pane:$eventId",
        factory = HiltViewModelFactory(LocalContext.current, defaults.defaultViewModelProviderFactory),
        extras = extras,
    )
    EventRoute(onBack = null, onOpenDua = onOpenDua, onCustomize = onCustomize, onStart = onStart, viewModel = vm)
}

@Composable
fun DuaLibraryContent(
    categories: List<DuaCategory>,
    selectedId: String? = null,
    routines: List<Routine>? = null,
    routineActions: RoutineActions = RoutineActions(),
    onOpenEvent: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
        // The user's own routines come first: they are what they open the tab for.
        if (routines != null) {
            item(key = "routines") {
                SectionHeader(stringResource(R.string.routine_section))
                RoutinesCard(
                    routines = routines,
                    onStart = routineActions.onStart,
                    onEdit = routineActions.onEdit,
                    onNew = routineActions.onNew,
                    modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter),
                )
            }
        }
        categories.forEachIndexed { index, category ->
            item(key = category.id) {
                SectionHeader(category.label)
                GlassCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter).enterReveal(index),
                ) {
                    Column {
                        val timeline = category.id == DAILY
                        category.events.forEachIndexed { i, event ->
                            if (i > 0 && !timeline) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            EventRow(
                                event = event,
                                timeline = timeline,
                                isFirst = i == 0,
                                isLast = i == category.events.lastIndex,
                                selected = event.id == selectedId,
                                onClick = { onOpenEvent(event.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * An event row. In the daily category the rows sit on a vertical rail, reading as the day from
 * waking to sleeping.
 */
@Composable
private fun EventRow(event: DuaEvent, timeline: Boolean, isFirst: Boolean, isLast: Boolean, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = HahoTheme.dimens.largeTouchTarget)
            .then(if (selected) Modifier.background(Glass.colors.tinted) else Modifier)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(start = HahoTheme.dimens.screenGutter, end = HahoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (timeline) {
            val line = MaterialTheme.colorScheme.outlineVariant
            Column(
                Modifier.fillMaxHeight().width(AdwamDimens.timelineDot),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f).width(HahoTheme.dimens.divider * 2).background(if (isFirst) line.copy(alpha = 0f) else line))
                Box(Modifier.size(AdwamDimens.timelineDot).background(MaterialTheme.colorScheme.primary, CircleShape))
                Box(Modifier.weight(1f).width(HahoTheme.dimens.divider * 2).background(if (isLast) line.copy(alpha = 0f) else line))
            }
        }
        Column(
            Modifier.weight(1f).padding(start = if (timeline) HahoTheme.spacing.lg else 0.dp, top = HahoTheme.spacing.md, bottom = HahoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xxs),
        ) {
            Text(event.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                pluralStringResource(R.plurals.duas_in_event, event.duaCount, event.duaCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(HahoTheme.spacing.md),
        )
    }
}

private const val DAILY = "daily"
