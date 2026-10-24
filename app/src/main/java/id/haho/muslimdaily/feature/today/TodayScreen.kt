package id.haho.muslimdaily.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import id.haho.muslimdaily.core.designsystem.glass.enterReveal
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassOutlinedButton
import id.haho.muslimdaily.core.designsystem.glass.GlassTonalButton
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import java.time.Duration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.feature.routines.RoutinesCard
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import id.haho.muslimdaily.core.designsystem.component.bidiIsolate
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SectionHeader
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.PreviewSurface
import id.haho.muslimdaily.core.ui.ThemePreviews
import id.haho.muslimdaily.core.ui.durationLabel
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.muslimdaily.core.ui.hijriLabel
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.core.ui.rememberTimeFormatter
import id.haho.muslimdaily.data.location.LocationRepository
import kotlinx.coroutines.launch

data class TodayActions(
    val onToggle: (key: String, checked: Boolean) -> Unit = { _, _ -> },
    val onStartAdhkar: (eventId: String) -> Unit = {},
    val onOpenEvent: (eventId: String) -> Unit = {},
    val onOpenLocation: () -> Unit = {},
    val onOpenPrayerSettings: () -> Unit = {},
    val onUseGps: () -> Unit = {},
    val onSetTravelMode: (Boolean) -> Unit = {},
    val onEnableTravelFromSuggestion: () -> Unit = {},
    val onDismissTravel: () -> Unit = {},
    val onOpenMosques: () -> Unit = {},
    val onOpenQibla: () -> Unit = {},
    val onOpenFasting: () -> Unit = {},
    val onAnswerHijri: (HijriCheck, Boolean?) -> Unit = { _, _ -> },
    val onStartRoutine: (Routine) -> Unit = {},
)

@Composable
fun TodayRoute(
    onStartAdhkar: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenLocation: () -> Unit,
    onOpenPrayerSettings: () -> Unit,
    onOpenMosques: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenFasting: () -> Unit,
    onStartRun: (String) -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current

    fun handle(result: LocationRepository.FixResult) {
        val message = when (result) {
            is LocationRepository.FixResult.Success -> null
            LocationRepository.FixResult.NoPermission -> resources.getString(R.string.location_permission_denied)
            LocationRepository.FixResult.Unavailable -> resources.getString(R.string.location_unavailable)
        }
        if (message != null) scope.launch { snackbar.showSnackbar(message) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.updateLocationFromGps(::handle) else handle(LocationRepository.FixResult.NoPermission)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.checkTravel() }

    TodayScreen(
        state = state,
        snackbarHostState = snackbar,
        actions = TodayActions(
            onToggle = viewModel::toggle,
            onStartAdhkar = onStartAdhkar,
            onOpenEvent = onOpenEvent,
            onOpenLocation = onOpenLocation,
            onOpenPrayerSettings = onOpenPrayerSettings,
            onUseGps = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            onSetTravelMode = viewModel::setTravelMode,
            onEnableTravelFromSuggestion = viewModel::enableTravelMode,
            onDismissTravel = viewModel::dismissTravel,
            onOpenMosques = onOpenMosques,
            onOpenQibla = onOpenQibla,
            onOpenFasting = onOpenFasting,
            onAnswerHijri = viewModel::answerHijri,
            onStartRoutine = { onStartRun(it.runKey) },
        ),
        routines = routines,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    actions: TodayActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initiallyExpanded: Set<String> = emptySet(),
    routines: List<Routine> = emptyList(),
) {
    Box(Modifier.fillMaxSize()) {
        when (state) {
            TodayUiState.Loading -> Unit
            TodayUiState.NoLocation -> NoLocation(actions)
            is TodayUiState.Ready -> TodayContent(state.data, actions, initiallyExpanded, routines)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun NoLocation(actions: TodayActions) {
    Column(Modifier.fillMaxSize().statusBarsPadding(), verticalArrangement = Arrangement.Center) {
        EmptyState(
            icon = R.drawable.ic_location_on,
            title = stringResource(R.string.today_no_location_title),
            body = stringResource(R.string.today_no_location_body),
        ) {
            GlassButton(onClick = actions.onUseGps, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_my_location), null)
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.today_use_gps))
            }
            GlassOutlinedButton(onClick = actions.onOpenLocation, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.today_pick_city))
            }
        }
    }
}

/** Two columns from this width (Material's expanded class): today at a glance beside the schedule. */
private val TwoPaneWidth = 840.dp

@Composable
private fun TodayContent(data: TodayData, actions: TodayActions, initiallyExpanded: Set<String>, routines: List<Routine>) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val onExpandToggle: (String) -> Unit = { key -> expanded = if (key in expanded) expanded - key else expanded + key }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val bottom = PaddingValues(bottom = AppTheme.spacing.xxl)
        if (maxWidth >= TwoPaneWidth) {
            Row(Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxSize(), contentPadding = bottom) {
                    glanceItems(data, actions, routines)
                    otherItems(data, actions)
                }
                LazyColumn(Modifier.weight(1f).fillMaxSize().statusBarsPadding(), contentPadding = bottom) {
                    item(key = "pane-top") { Spacer(Modifier.height(AppTheme.spacing.lg)) }
                    scheduleItems(data, actions, expanded, onExpandToggle)
                }
            }
        } else {
            LazyColumn(Modifier.widthIn(max = AppTheme.dimens.maxContentWidth).fillMaxSize(), contentPadding = bottom) {
                glanceItems(data, actions, routines)
                scheduleItems(data, actions, expanded, onExpandToggle)
                otherItems(data, actions)
            }
        }
    }
}

/** Header, next prayer and the cards that matter right now. */
private fun LazyListScope.glanceItems(data: TodayData, actions: TodayActions, routines: List<Routine>) {
    item(key = "header") { Header(data, actions, Modifier.statusBarsPadding()) }
    item(key = "next") { NextPrayerCard(data, actions, Modifier.padding(horizontal = AppTheme.dimens.screenGutter).enterReveal(0)) }
    if (data.prohibited.isNotEmpty()) {
        item(key = "prohibited") {
            ProhibitedCard(data, Modifier.padding(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, top = AppTheme.spacing.md).enterReveal(1))
        }
    }
    data.hijriCheck?.let { c ->
        item(key = "hijri") {
            HijriCheckCard(c, actions.onAnswerHijri, Modifier.padding(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, top = AppTheme.spacing.md).enterReveal(1))
        }
    }
    data.ramadan?.let { r ->
        item(key = "ramadan") {
            RamadanCardView(r, data, { actions.onOpenEvent("iftar") }, Modifier.padding(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, top = AppTheme.spacing.md).enterReveal(1))
        }
    }
    data.travelSuggestionKm?.let { km ->
        item(key = "travel") { TravelSuggestion(km, data.locationName, actions, Modifier.padding(AppTheme.dimens.screenGutter)) }
    }
    data.adhkar?.let { card ->
        item(key = "adhkar") {
            AdhkarCardView(card, data, actions, Modifier.padding(start = AppTheme.dimens.screenGutter, end = AppTheme.dimens.screenGutter, top = AppTheme.spacing.md).enterReveal(1))
        }
    }
    if (routines.isNotEmpty()) {
        item(key = "routines") {
            Column {
                SectionHeader(stringResource(R.string.today_routines))
                RoutinesCard(
                    routines = routines,
                    onStart = actions.onStartRoutine,
                    onEdit = {},
                    onNew = {},
                    compact = true,
                    modifier = Modifier.padding(horizontal = AppTheme.dimens.screenGutter).enterReveal(1),
                )
            }
        }
    }
}

private fun LazyListScope.scheduleItems(
    data: TodayData,
    actions: TodayActions,
    expanded: Set<String>,
    onExpandToggle: (String) -> Unit,
) {
    item(key = "schedule-header") { SectionHeader(stringResource(R.string.today_schedule)) }
    item(key = "schedule") {
        PrayerListCard(
            data = data,
            expanded = expanded,
            onExpandToggle = onExpandToggle,
            onToggle = actions.onToggle,
            modifier = Modifier.padding(horizontal = AppTheme.dimens.screenGutter).enterReveal(2),
        )
    }
    item(key = "private-note") {
        InfoNote(
            stringResource(R.string.today_private_note),
            Modifier.padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
            icon = R.drawable.ic_lock,
        )
    }
}

private fun LazyListScope.otherItems(data: TodayData, actions: TodayActions) {
    item(key = "footer") { MethodFooter(data, actions.onOpenPrayerSettings) }
}

@Composable
private fun Header(data: TodayData, actions: TodayActions, modifier: Modifier = Modifier) {
    val locationDescription = stringResource(R.string.today_location_chip, data.locationName)
    Column(
        modifier = modifier.fillMaxWidth().padding(
            start = AppTheme.dimens.screenGutter,
            end = AppTheme.dimens.screenGutter,
            top = AppTheme.spacing.lg,
            bottom = AppTheme.spacing.md,
        ),
    ) {
        Text(
            formatLongDate(data.date),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            hijriLabel(data.hijri),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        ) {
            AssistChip(
                onClick = actions.onOpenLocation,
                label = { Text(data.locationName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_location_on), null, Modifier.size(AppTheme.dimens.smallIcon)) },
                modifier = Modifier.semantics { contentDescription = locationDescription },
            )
            FilterChip(
                selected = data.travelMode,
                onClick = { actions.onSetTravelMode(!data.travelMode) },
                label = { Text(stringResource(R.string.today_travel_chip)) },
                leadingIcon = {
                    Icon(
                        painterResource(if (data.travelMode) R.drawable.ic_check else R.drawable.ic_luggage),
                        null,
                        Modifier.size(AppTheme.dimens.smallIcon),
                    )
                },
            )
            AssistChip(
                onClick = actions.onOpenFasting,
                label = { Text(stringResource(R.string.fasting_open)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar_month), null, Modifier.size(AppTheme.dimens.smallIcon)) },
            )
        }
    }
}

@Composable
private fun NextPrayerCard(data: TodayData, actions: TodayActions, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    val next = data.next
    val name = stringResource(next.prayer.labelRes())
    val clock = time.format(next.time, data.zone)
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Glass.colors.tinted,
        contentColor = Glass.colors.onTinted,
    ) {
        Column(Modifier.padding(AppTheme.dimens.cardPadding)) {
            // The prayer on the left, its time on the right; with large text the time wraps below.
            FlowRow(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Column(Modifier.padding(end = AppTheme.spacing.md)) {
                    Text(stringResource(R.string.today_next), style = MaterialTheme.typography.labelLarge)
                    Text(name, style = MaterialTheme.typography.headlineMedium)
                    if (next.isTomorrow) {
                        Text(stringResource(R.string.today_tomorrow), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(clock, style = MaterialTheme.typography.displayMedium)
                    val countdown = stringResource(R.string.countdown_in, durationLabel(next.remaining))
                    val motion = AppTheme.motion
                    AnimatedContent(
                        targetState = countdown,
                        transitionSpec = {
                            (slideInVertically(motion.layout()) { it / 2 } + fadeIn(motion.fade()))
                                .togetherWith(slideOutVertically(motion.layout()) { -it / 2 } + fadeOut(motion.fade()))
                        },
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        label = "countdown",
                    ) { text ->
                        Text(text, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // Where to face and where to pray it, under the prayer they serve; each as wide as its
            // label, wrapping to a second line when large text needs it.
            FlowRow(
                Modifier.fillMaxWidth().padding(top = AppTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                NextPrayerAction(R.drawable.ic_explore, stringResource(R.string.qibla_open), actions.onOpenQibla)
                NextPrayerAction(R.drawable.ic_mosque, stringResource(R.string.mosque_open), actions.onOpenMosques)
            }
        }
    }
}

@Composable
private fun NextPrayerAction(icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        Icon(painterResource(icon), null, Modifier.size(AppTheme.dimens.smallIcon))
        Spacer(Modifier.width(AppTheme.spacing.sm))
        Text(label)
    }
}

@Composable
private fun TravelSuggestion(km: Int, locationName: String, actions: TodayActions, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Glass.colors.tinted,
        contentColor = Glass.colors.onTinted,
    ) {
        Column(Modifier.padding(AppTheme.spacing.lg)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Icon(painterResource(R.drawable.ic_travel_explore), null)
                Text(stringResource(R.string.today_travel_suggestion, km, bidiIsolate(locationName)), style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm),
            ) {
                TextButton(onClick = actions.onDismissTravel) { Text(stringResource(R.string.today_dismiss)) }
                TextButton(onClick = actions.onEnableTravelFromSuggestion) {
                    Text(stringResource(R.string.today_enable_travel))
                }
                GlassTonalButton(onClick = actions.onUseGps) { Text(stringResource(R.string.today_update_location)) }
            }
        }
    }
}

@Composable
private fun AdhkarCardView(card: AdhkarCard, data: TodayData, actions: TodayActions, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    val isMorning = card.eventId == TodayStateBuilder.MORNING
    val title = stringResource(if (isMorning) R.string.adhkar_morning else R.string.adhkar_evening)
    val subtitle = when {
        card.progress?.completed == true -> stringResource(R.string.adhkar_done_today)
        else -> stringResource(R.string.adhkar_active, time.format(card.end, data.zone))
    }
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(AppTheme.spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(AppTheme.dimens.minTouchTarget),
                ) {
                    Icon(
                        painterResource(if (isMorning) R.drawable.ic_wb_sunny else R.drawable.ic_wb_twilight),
                        contentDescription = null,
                        modifier = Modifier.padding(AppTheme.spacing.md),
                    )
                }
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    card.progress?.takeIf { !it.completed && it.done > 0 }?.let {
                        Text(
                            stringResource(R.string.adhkar_progress, it.done, it.total),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            InfoNote(stringResource(R.string.note_adhkar_window), Modifier.padding(top = AppTheme.spacing.sm))
            FlowRow(
                Modifier.fillMaxWidth().padding(top = AppTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                TextButton(onClick = { actions.onOpenEvent(card.eventId) }) {
                    Text(stringResource(R.string.adhkar_open_list))
                }
                val resume = card.progress?.let { !it.completed && it.done > 0 } == true
                val label = stringResource(if (resume) R.string.action_resume else R.string.action_start)
                GlassButton(onClick = { actions.onStartAdhkar(card.eventId) }) { Text(label) }
            }
        }
    }
}

@Composable
private fun MethodFooter(data: TodayData, onOpenPrayerSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.lg),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.today_method_footer, stringResource(data.method.labelRes()), stringResource(data.asr.labelRes())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenPrayerSettings) {
            Text(stringResource(R.string.today_change_method))
        }
    }
}

@ThemePreviews
@Composable
private fun TodayPreview() = PreviewSurface {
    TodayScreen(TodayPreviewData.ready(), TodayActions(), initiallyExpanded = setOf("DHUHR"))
}

@ThemePreviews
@Composable
private fun TodayNoLocationPreview() = PreviewSurface {
    TodayScreen(TodayUiState.NoLocation, TodayActions())
}

private val CrescentGold = androidx.compose.ui.graphics.Color(0xFFC9A227)

/** Ramadan: day N, when suhur ends and when to break the fast, with a countdown to the next. */
@Composable
private fun RamadanCardView(r: RamadanCard, data: TodayData, onIftarDua: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberTimeFormatter()
    val suhur = r.suhurNow(data.now)
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Icon(painterResource(R.drawable.ic_dark_mode), null, tint = CrescentGold, modifier = Modifier.size(AppTheme.dimens.icon))
                Text(
                    stringResource(R.string.ramadan_day, r.day),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                RamadanTime(stringResource(R.string.ramadan_suhur), time.format(r.suhurEnds, data.zone), active = suhur, Modifier.weight(1f))
                RamadanTime(stringResource(R.string.ramadan_iftar), time.format(r.iftar, data.zone), active = !suhur, Modifier.weight(1f))
            }
            val left = Duration.between(data.now, if (suhur) r.suhurEnds else r.iftar)
            Text(
                stringResource(if (suhur) R.string.ramadan_suhur_left else R.string.ramadan_iftar_left, durationLabel(left)),
                style = MaterialTheme.typography.titleMedium,
            )
            GlassTonalButton(onClick = onIftarDua) {
                Icon(painterResource(R.drawable.ic_restaurant), null, Modifier.size(AppTheme.dimens.smallIcon))
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.ramadan_iftar_dua))
            }
        }
    }
}

@Composable
private fun RamadanTime(label: String, clock: String, active: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (active) Glass.colors.tinted else androidx.compose.ui.graphics.Color.Transparent)
            .padding(AppTheme.spacing.md)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) Glass.colors.onTinted else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(clock, style = MaterialTheme.typography.headlineMedium, color = if (active) Glass.colors.onTinted else MaterialTheme.colorScheme.onSurface)
    }
}


/** Month start by sidang isbat: one question, two plain answers, and a way to leave it. */
@Composable
private fun HijriCheckCard(check: HijriCheck, onAnswer: (HijriCheck, Boolean?) -> Unit, modifier: Modifier = Modifier) {
    val months = stringArrayResource(R.array.hijri_months)
    val ending = months[check.month - 1]
    val next = months[check.nextMonth - 1]
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Icon(painterResource(R.drawable.ic_dark_mode), null, tint = CrescentGold, modifier = Modifier.size(AppTheme.dimens.icon))
                Text(
                    stringResource(R.string.hijri_check_title, next),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                stringResource(R.string.hijri_check_body, ending, next),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GlassTonalButton(onClick = { onAnswer(check, true) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (check.onEve) R.string.hijri_check_new else R.string.hijri_check_new_today, next))
            }
            GlassTonalButton(onClick = { onAnswer(check, false) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hijri_check_complete, ending))
            }
            TextButton(onClick = { onAnswer(check, null) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.hijri_check_skip))
            }
        }
    }
}
