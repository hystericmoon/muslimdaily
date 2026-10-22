package id.haho.muslimdaily.feature.mosque

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.glass.GlassOutlinedButton
import id.haho.muslimdaily.core.designsystem.glass.GlassTonalButton
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.durationLabel
import id.haho.muslimdaily.core.ui.isLargeFontScale
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.core.ui.rememberTimeFormatter
import id.haho.muslimdaily.data.mosque.Mosque
import java.text.NumberFormat
import java.time.Duration
import kotlin.math.ceil

data class MosqueActions(
    val onAllowLocation: () -> Unit = {},
    val onUseSaved: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onDirections: (Mosque) -> Unit = {},
    val onSearchMaps: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    /** Location was denied for good; the allow buttons open app settings instead. */
    val permissionBlocked: Boolean = false,
)

@Composable
fun NearbyMosqueRoute(onBack: () -> Unit, viewModel: NearbyMosqueViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val unnamed = stringResource(R.string.mosque_unnamed)
    val query = stringResource(R.string.mosque_map_query)
    val activity = LocalActivity.current
    // After "don't ask again" the system dialog no longer shows, so the button opens app settings.
    var blocked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val precise = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        blocked = !precise && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.onPermissionResult()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (viewModel.hasPrecisePermission()) blocked = false
        viewModel.onResume()
    }
    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No maps app; the buttons are hidden in that case, so this is only a race.
        }
    }
    val canSearch = MapsIntents.canHandle(context, MapsIntents.search(0.0, 0.0, query))
    AppScreen(title = stringResource(R.string.mosque_title), onBack = onBack) {
        NearbyMosqueContent(
            state = state,
            header = header,
            canOpenMaps = canSearch,
            actions = MosqueActions(
                onAllowLocation = {
                    if (blocked) {
                        launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    } else {
                        permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                permissionBlocked = blocked,
                onUseSaved = viewModel::useSaved,
                onRetry = viewModel::locate,
                onDirections = { m -> launch(MapsIntents.directions(context, m, m.name.ifBlank { unnamed })) },
                onSearchMaps = { lat, lon -> launch(MapsIntents.search(lat, lon, query)) },
            ),
        )
    }
}

@Composable
fun NearbyMosqueContent(
    state: MosqueUiState,
    header: MosqueHeader?,
    canOpenMaps: Boolean,
    actions: MosqueActions,
) {
    when (state) {
        MosqueUiState.Locating -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                CircularProgressIndicator()
                Text(stringResource(R.string.mosque_locating), style = MaterialTheme.typography.bodyMedium)
            }
        }
        is MosqueUiState.NeedsPermission -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = R.drawable.ic_near_me,
                title = stringResource(R.string.mosque_permission_title),
                body = stringResource(R.string.mosque_permission_body),
            ) {
                GlassButton(onClick = actions.onAllowLocation, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (actions.permissionBlocked) R.string.mosque_open_settings else R.string.mosque_allow_location))
                }
                if (actions.permissionBlocked) {
                    Text(
                        stringResource(R.string.mosque_permission_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                state.savedName?.let { name ->
                    GlassOutlinedButton(onClick = actions.onUseSaved, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mosque_use_saved, name), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        MosqueUiState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = R.drawable.ic_location_off,
                title = stringResource(R.string.mosque_unavailable_title),
                body = stringResource(R.string.mosque_unavailable_body),
            ) {
                GlassButton(onClick = actions.onRetry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_retry)) }
            }
        }
        is MosqueUiState.Ready -> MosqueList(state, header, canOpenMaps, actions)
    }
}

@Composable
private fun MosqueList(state: MosqueUiState.Ready, header: MosqueHeader?, canOpenMaps: Boolean, actions: MosqueActions) {
    val gutter = AppTheme.dimens.screenGutter
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = AppTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        header?.let { h -> item { NextPrayerCard(h) } }
        item { OriginNote(state.origin, actions) }
        if (state.mosques.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_mosque,
                    title = stringResource(R.string.mosque_none_title),
                    body = stringResource(R.string.mosque_none_body),
                )
            }
        } else {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column {
                        state.mosques.forEachIndexed { i, m ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            MosqueRow(m, actions)
                        }
                    }
                }
            }
        }
        if (canOpenMaps) {
            item {
                GlassOutlinedButton(
                    onClick = { actions.onSearchMaps(state.origin.latitude, state.origin.longitude) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_travel_explore), null)
                    Spacer(Modifier.width(AppTheme.spacing.sm))
                    Text(stringResource(R.string.mosque_search_maps))
                }
            }
        }
        item {
            Text(
                stringResource(R.string.mosque_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextPrayerCard(h: MosqueHeader) {
    val time = rememberTimeFormatter()
    val name = stringResource(h.next.prayer.labelRes())
    val left = Duration.between(h.now, h.next.time)
    GlassCard(Modifier.fillMaxWidth(), color = Glass.colors.tinted, contentColor = Glass.colors.onTinted) {
        Column(Modifier.padding(AppTheme.spacing.lg).semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.today_next), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.notif_next_prayer_title, name, time.format(h.next.time, h.zone)),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(R.string.countdown_in, durationLabel(left)), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OriginNote(origin: Origin, actions: MosqueActions) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        when (origin) {
            is Origin.Precise -> InfoNote(
                origin.accuracyMeters?.let { stringResource(R.string.mosque_origin_precise, it) }
                    ?: stringResource(R.string.mosque_origin_precise_unknown),
                icon = R.drawable.ic_my_location,
            )
            is Origin.Approximate -> {
                InfoNote(stringResource(R.string.mosque_origin_approximate))
                GlassTonalButton(onClick = actions.onAllowLocation) {
                    Text(stringResource(if (actions.permissionBlocked) R.string.mosque_open_settings else R.string.mosque_allow_precise))
                }
            }
            is Origin.Saved -> {
                InfoNote(stringResource(R.string.mosque_origin_saved, origin.name))
                GlassTonalButton(onClick = actions.onRetry) { Text(stringResource(R.string.mosque_use_current)) }
            }
        }
    }
}

@Composable
private fun MosqueRow(m: Mosque, actions: MosqueActions) {
    val name = m.name.ifBlank { stringResource(R.string.mosque_unnamed) }
    val walk = ceil(m.distanceMeters / WALK_METERS_PER_MINUTE).toInt().coerceAtLeast(1)
    val distance = formatDistance(m.distanceMeters)
    val routeLabel = stringResource(R.string.mosque_route_to, name)
    val details: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier.semantics(mergeDescendants = true) {}) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(
                stringResource(R.string.mosque_distance, distance, pluralStringResource(R.plurals.mosque_walk_minutes, walk, walk)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val route: @Composable () -> Unit = {
        GlassTonalButton(onClick = { actions.onDirections(m) }, modifier = Modifier.semantics { contentDescription = routeLabel }) {
            Icon(painterResource(R.drawable.ic_directions_walk), null, Modifier.size(18.dp))
            Spacer(Modifier.width(AppTheme.spacing.xs))
            Text(stringResource(R.string.mosque_route))
        }
    }
    val padding = Modifier.fillMaxWidth().padding(start = AppTheme.spacing.lg, end = AppTheme.spacing.md, top = AppTheme.spacing.md, bottom = AppTheme.spacing.md)
    if (isLargeFontScale()) {
        // Large text: the button would squeeze the name, so it goes underneath.
        Column(padding, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            details(Modifier)
            route()
        }
    } else {
        Row(padding, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Icon(painterResource(R.drawable.ic_mosque), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppTheme.dimens.icon))
            details(Modifier.weight(1f))
            route()
        }
    }
}

/** About 4.8 km/h. Only used to give a rough walking time. */
private const val WALK_METERS_PER_MINUTE = 80.0

@Composable
private fun formatDistance(meters: Int): String =
    if (meters < 1000) {
        stringResource(R.string.mosque_meters, meters)
    } else {
        val km = NumberFormat.getNumberInstance(id.haho.muslimdaily.core.ui.AppLocale).apply { maximumFractionDigits = 1 }.format(meters / 1000.0)
        stringResource(R.string.mosque_km, km)
    }
