package id.haho.muslimdaily.feature.duas.library

import id.haho.design.glass.enterReveal
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.design.glass.chevronRotation
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.feature.routines.ROUTINE_PREVIEW_ROWS
import id.haho.muslimdaily.feature.routines.RoutineCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DuaLibraryViewModel @Inject constructor(
    content: ContentRepository,
    routines: RoutineRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val categories: StateFlow<List<DuaCategory>?> = content.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val routines: StateFlow<List<Routine>?> = routines.routines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The first duas of each routine, shown on its card. */
    val previews: StateFlow<Map<String, Dua>> = routines.routines()
        .mapLatest { list -> content.duas(list.flatMap { it.duaIds.take(ROUTINE_PREVIEW_ROWS) }.distinct()).associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** The tab used last; blank until the reader picks one. */
    val savedTab: StateFlow<String?> = settings.settings.map { it.duaLibraryTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveTab(tab: LibraryTab) {
        viewModelScope.launch { settings.update { duaLibraryTab = tab.key } }
    }
}

/** The Duas page's two tabs: the reader's routines, and the whole library. */
enum class LibraryTab(val key: String) {
    MINE("mine"),
    ALL("all"),
    ;

    companion object {
        /** The tab used last, or with none chosen yet, the routines once there are any. */
        fun initial(saved: String?, hasRoutines: Boolean): LibraryTab =
            entries.firstOrNull { it.key == saved } ?: if (hasRoutines) MINE else ALL
    }
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
    val previews by viewModel.previews.collectAsStateWithLifecycle()
    val savedTab by viewModel.savedTab.collectAsStateWithLifecycle()
    var chosenTab by rememberSaveable { mutableStateOf<LibraryTab?>(null) }
    var expanded by rememberSaveable { mutableStateOf(emptyList<String>()) }
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
                val c = categories
                val r = routines
                // Wait for the routines and the saved tab, so the page doesn't open on one tab and jump.
                if (c != null && r != null && savedTab != null) {
                    DuaLibraryContent(
                        c,
                        selectedId = if (twoPane) shown else null,
                        routines = r,
                        routineActions = RoutineActions(
                            onStart = { routine -> onStart(routine.runKey) },
                            onEdit = { routine -> onRoutine(routine.id, null) },
                            onNew = { onRoutine(null, null) },
                        ),
                        previews = previews,
                        tab = chosenTab ?: LibraryTab.initial(savedTab, r.isNotEmpty()),
                        onTab = { tab ->
                            chosenTab = tab
                            viewModel.saveTab(tab)
                        },
                        expanded = expanded.toSet(),
                        onToggleCategory = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
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

/**
 * The Duas page. With [routines] (null in previews of the library alone) it has two tabs: the
 * reader's routines, and the library, where each category opens in place to list its occasions.
 */
@Composable
fun DuaLibraryContent(
    categories: List<DuaCategory>,
    selectedId: String? = null,
    routines: List<Routine>? = null,
    routineActions: RoutineActions = RoutineActions(),
    previews: Map<String, Dua> = emptyMap(),
    tab: LibraryTab = LibraryTab.initial(null, !routines.isNullOrEmpty()),
    onTab: (LibraryTab) -> Unit = {},
    expanded: Set<String> = emptySet(),
    onToggleCategory: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (routines != null) {
            PrimaryTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                LibraryTab.entries.forEach { t ->
                    Tab(
                        selected = t == tab,
                        onClick = { onTab(t) },
                        text = { Text(stringResource(if (t == LibraryTab.MINE) R.string.library_tab_mine else R.string.library_tab_all), maxLines = 1) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (routines != null && tab == LibraryTab.MINE) {
            MyRoutines(routines, previews, routineActions, onBrowse = { onTab(LibraryTab.ALL) })
        } else {
            Categories(categories, selectedId, expanded, onToggleCategory, onOpenEvent)
        }
    }
}

@Composable
private fun MyRoutines(routines: List<Routine>, previews: Map<String, Dua>, actions: RoutineActions, onBrowse: () -> Unit) {
    val gutter = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = HahoTheme.spacing.md, bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        if (routines.isEmpty()) {
            item(key = "empty") {
                Column(gutter, verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                    RoutinesCard(routines = routines, onStart = actions.onStart, onEdit = actions.onEdit, onNew = actions.onNew)
                    TextButton(onClick = onBrowse) { Text(stringResource(R.string.library_browse_all)) }
                }
            }
        } else {
            itemsIndexed(routines, key = { _, r -> r.id }) { index, r ->
                RoutineCard(
                    routine = r,
                    previews = previews,
                    onStart = { actions.onStart(r) },
                    onEdit = { actions.onEdit(r) },
                    modifier = gutter.enterReveal(index),
                )
            }
            item(key = "new") {
                TextButton(onClick = actions.onNew, modifier = Modifier.padding(horizontal = HahoTheme.spacing.sm)) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(HahoTheme.dimens.smallIcon))
                    Spacer(Modifier.width(HahoTheme.spacing.sm))
                    Text(stringResource(R.string.routine_new))
                }
            }
        }
    }
}

@Composable
private fun Categories(
    categories: List<DuaCategory>,
    selectedId: String?,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = HahoTheme.spacing.md, bottom = HahoTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        categories.forEachIndexed { index, category ->
            item(key = category.id) {
                CategoryCard(
                    category = category,
                    expanded = category.id in expanded,
                    selectedId = selectedId,
                    onToggle = { onToggle(category.id) },
                    onOpenEvent = onOpenEvent,
                    modifier = Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).enterReveal(index),
                )
            }
        }
    }
}

/** A category that opens in place, like a prayer row on Today, to list its occasions. */
@Composable
private fun CategoryCard(
    category: DuaCategory,
    expanded: Boolean,
    selectedId: String?,
    onToggle: () -> Unit,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(if (expanded) R.string.library_collapse_category else R.string.library_expand_category)
    val stateLabel = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val headerBackground by animateColorAsState(
        if (expanded) Glass.colors.tinted else Color.Transparent,
        HahoTheme.motion.fade(),
        label = "categoryHeader",
    )
    GlassCard(shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(headerBackground)
                    .heightIn(min = HahoTheme.dimens.largeTouchTarget)
                    .clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = stateLabel }
                    .padding(start = HahoTheme.dimens.screenGutter, end = HahoTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f).padding(vertical = HahoTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xxs),
                ) {
                    Text(category.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(
                        pluralStringResource(R.plurals.library_event_count, category.events.size, category.events.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.size(HahoTheme.dimens.minTouchTarget), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.chevronRotation(expanded),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(HahoTheme.motion.layout()) + fadeIn(HahoTheme.motion.fade()),
                exit = shrinkVertically(HahoTheme.motion.layout()) + fadeOut(HahoTheme.motion.fade()),
            ) {
                Column {
                    val timeline = category.id == DAILY
                    category.events.forEachIndexed { i, event ->
                        if (i == 0 || !timeline) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
