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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
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
import id.haho.muslimdaily.domain.prayer.TimeWindow
import id.haho.muslimdaily.domain.prayer.WindowType
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
)

@Composable
fun TodayRoute(
    onStartAdhkar: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenLocation: () -> Unit,
    onOpenPrayerSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
        ),
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    actions: TodayActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initiallyExpanded: Set<String> = emptySet(),
) {
    Box(Modifier.fillMaxSize()) {
        when (state) {
            TodayUiState.Loading -> Unit
            TodayUiState.NoLocation -> NoLocation(actions)
            is TodayUiState.Ready -> TodayContent(state.data, actions, initiallyExpanded)
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

@Composable
private fun TodayContent(data: TodayData, actions: TodayActions, initiallyExpanded: Set<String>) {
    val gutter = AppTheme.dimens.screenGutter
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl),
    ) {
        item(key = "header") { Header(data, actions, Modifier.statusBarsPadding()) }
        item(key = "next") { NextPrayerCard(data, Modifier.padding(horizontal = gutter).enterReveal(0)) }
        data.travelSuggestionKm?.let { km ->
            item(key = "travel") { TravelSuggestion(km, data.locationName, actions, Modifier.padding(gutter)) }
        }
        item(key = "adhkar") {
            AdhkarCardView(data, actions, Modifier.padding(start = gutter, end = gutter, top = AppTheme.spacing.md).enterReveal(1))
        }
        item(key = "schedule-header") { SectionHeader(stringResource(R.string.today_schedule)) }
        item(key = "schedule") {
            PrayerListCard(
                data = data,
                expanded = expanded,
                onExpandToggle = { key -> expanded = if (key in expanded) expanded - key else expanded + key },
                onToggle = actions.onToggle,
                modifier = Modifier.padding(horizontal = gutter).enterReveal(2),
            )
        }
        item(key = "private-note") {
            InfoNote(
                stringResource(R.string.today_private_note),
                Modifier.padding(horizontal = gutter, vertical = AppTheme.spacing.sm),
                icon = R.drawable.ic_lock,
            )
        }
        item(key = "other-header") { SectionHeader(stringResource(R.string.today_other_sunnah)) }
        item(key = "other") { OtherSunnahCard(data, actions.onToggle, Modifier.padding(horizontal = gutter).enterReveal(3)) }
        item(key = "prohibited-header") { SectionHeader(stringResource(R.string.today_prohibited_title)) }
        item(key = "prohibited") { ProhibitedCard(data, Modifier.padding(horizontal = gutter).enterReveal(4)) }
        item(key = "footer") { MethodFooter(data, actions.onOpenPrayerSettings) }
    }
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
        }
    }
}

@Composable
private fun NextPrayerCard(data: TodayData, modifier: Modifier = Modifier) {
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
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.today_next), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    Text(name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f, fill = false))
                    if (next.isTomorrow) {
                        Text(
                            stringResource(R.string.today_tomorrow),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = AppTheme.spacing.xs),
                        )
                    }
                }
                Text(clock, style = MaterialTheme.typography.displayMedium)
                val countdown = stringResource(R.string.countdown_in, durationLabel(next.remaining))
                val motion = AppTheme.motion
                AnimatedContent(
                    targetState = countdown,
                    transitionSpec = {
                        (slideInVertically(motion.layout()) { it / 2 } + fadeIn(motion.fade()))
                            .togetherWith(slideOutVertically(motion.layout()) { -it / 2 } + fadeOut(motion.fade()))
                    },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    label = "countdown",
                ) { text ->
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }
                next.iqamah?.let {
                    Text(
                        stringResource(R.string.today_iqamah, time.format(it, data.zone)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (data.activeWindows.isNotEmpty()) {
                HorizontalDivider(
                    Modifier.padding(vertical = AppTheme.spacing.md),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.24f),
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    data.activeWindows.forEach { WindowLine(it, data) }
                }
            }
        }
    }
}

@Composable
private fun WindowLine(window: TimeWindow, data: TodayData) {
    val time = rememberTimeFormatter()
    val until = stringResource(R.string.window_until, time.format(window.end, data.zone))
    val label = stringResource(window.type.labelRes())
    if (window.type.isProhibited) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(AppTheme.spacing.md).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Icon(painterResource(R.drawable.ic_block), null, Modifier.size(AppTheme.dimens.icon))
                Column {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(until, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    } else {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Icon(painterResource(windowIcon(window)), null, Modifier.size(AppTheme.dimens.icon))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(until, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun windowIcon(window: TimeWindow): Int = when (window.type) {
    WindowType.MORNING_ADHKAR -> R.drawable.ic_wb_sunny
    WindowType.EVENING_ADHKAR -> R.drawable.ic_wb_twilight
    WindowType.DUHA -> R.drawable.ic_sunny
    WindowType.WITR -> R.drawable.ic_nightlight
    else -> R.drawable.ic_block
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
private fun AdhkarCardView(data: TodayData, actions: TodayActions, modifier: Modifier = Modifier) {
    val card = data.adhkar
    val time = rememberTimeFormatter()
    val isMorning = card.eventId == TodayStateBuilder.MORNING
    val title = stringResource(if (isMorning) R.string.adhkar_morning else R.string.adhkar_evening)
    val subtitle = when {
        card.progress?.completed == true -> stringResource(R.string.adhkar_done_today)
        card.active -> stringResource(R.string.adhkar_active, time.format(card.end, data.zone))
        else -> stringResource(R.string.adhkar_upcoming, time.format(card.start, data.zone))
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
            if (card.active) {
                InfoNote(stringResource(R.string.note_adhkar_window), Modifier.padding(top = AppTheme.spacing.sm))
            }
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
                if (card.active) {
                    GlassButton(onClick = { actions.onStartAdhkar(card.eventId) }) { Text(label) }
                } else {
                    GlassTonalButton(onClick = { actions.onStartAdhkar(card.eventId) }) { Text(label) }
                }
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
