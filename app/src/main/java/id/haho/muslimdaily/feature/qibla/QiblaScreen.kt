package id.haho.muslimdaily.feature.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.data.sensor.CompassRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.Qibla
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface QiblaUiState {
    data object Loading : QiblaUiState
    data object NoLocation : QiblaUiState

    /** [heading] is null on phones without a compass: the bearing is still shown. */
    data class Ready(val place: String, val bearing: Double, val heading: Double?, val reliable: Boolean) : QiblaUiState
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    settings: SettingsRepository,
    compass: CompassRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<QiblaUiState> = settings.settings
        .map { it.location }
        .distinctUntilChanged()
        .flatMapLatest { loc ->
            when {
                loc == null -> flowOf(QiblaUiState.NoLocation)
                !compass.available() -> flowOf(QiblaUiState.Ready(loc.name, Qibla.bearing(loc.latitude, loc.longitude), null, true))
                else -> compass.headings(loc.latitude, loc.longitude).map {
                    QiblaUiState.Ready(loc.name, Qibla.bearing(loc.latitude, loc.longitude), it.degrees, it.reliable)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState.Loading)
}

/** Within this many degrees the phone counts as facing the qibla. */
private const val ALIGNED_DEGREES = 3.0
private const val SPOKEN_STEP = 15

@Composable
fun QiblaRoute(onBack: () -> Unit, onOpenLocation: () -> Unit, viewModel: QiblaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScreen(title = stringResource(R.string.qibla_title), onBack = onBack) {
        QiblaContent(state, onOpenLocation)
    }
}

@Composable
fun QiblaContent(state: QiblaUiState, onOpenLocation: () -> Unit) {
    when (state) {
        QiblaUiState.Loading -> Unit
        QiblaUiState.NoLocation -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = R.drawable.ic_location_off,
                title = stringResource(R.string.today_no_location_title),
                body = stringResource(R.string.qibla_no_location),
            ) {
                GlassButton(onClick = onOpenLocation, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.today_pick_city)) }
            }
        }
        is QiblaUiState.Ready -> Ready(state)
    }
}

@Composable
private fun Ready(state: QiblaUiState.Ready) {
    val bearing = state.bearing.roundToInt() % 360
    val turn = state.heading?.let { Qibla.turn(it, state.bearing) }
    // Hysteresis: aligned within ALIGNED_DEGREES, lost only past twice that, so sensor jitter at
    // the edge doesn't buzz and announce over and over.
    // Plain holder, not state: it only remembers the last answer between sensor readings.
    val latch = remember { BooleanArray(1) }
    val aligned = when {
        turn == null -> false
        latch[0] -> abs(turn) <= ALIGNED_DEGREES * 2
        else -> abs(turn) <= ALIGNED_DEGREES
    }.also { latch[0] = it }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(aligned) { if (aligned) haptics.performHapticFeedback(HapticFeedbackType.Confirm) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        val guidance = when {
            turn == null -> stringResource(R.string.qibla_no_sensor)
            aligned -> stringResource(R.string.qibla_aligned)
            turn > 0 -> stringResource(R.string.qibla_turn_right, abs(turn).roundToInt())
            else -> stringResource(R.string.qibla_turn_left, abs(turn).roundToInt())
        }
        val step = turn?.let { (abs(it) / SPOKEN_STEP).roundToInt().coerceAtLeast(1) * SPOKEN_STEP }
        val spoken = when {
            turn == null || aligned -> guidance
            turn > 0 -> stringResource(R.string.qibla_turn_right, step!!)
            else -> stringResource(R.string.qibla_turn_left, step!!)
        }
        Dial(
            heading = state.heading ?: 0.0,
            bearing = state.bearing,
            aligned = aligned,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = guidance },
        )
        GlassCard(
            Modifier.fillMaxWidth(),
            color = if (aligned) Glass.colors.tinted else Glass.colors.card,
            contentColor = if (aligned) Glass.colors.onTinted else MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.padding(AppTheme.spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    guidance,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    // Announced in 15° steps, not on every sensor reading.
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = spoken
                    },
                )
                Text(
                    stringResource(R.string.qibla_bearing, bearing, state.place),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (!state.reliable) InfoNote(stringResource(R.string.qibla_calibrate))
        InfoNote(stringResource(R.string.qibla_note))
    }
}

/**
 * Compass rose turning with the phone (north stays north), a Ka'bah marker at the qibla
 * bearing, and a fixed pointer at the top for where the phone points.
 */
@Composable
private fun Dial(heading: Double, bearing: Double, aligned: Boolean, modifier: Modifier = Modifier) {
    // Unwrap the angle so crossing north (359° → 1°) turns 2°, not a full spin back.
    val unwrapped = remember { mutableFloatStateOf(-heading.toFloat()) }
    LaunchedEffect(heading) {
        val delta = ((-heading.toFloat() - unwrapped.floatValue) % 360f + 540f) % 360f - 180f
        unwrapped.floatValue += delta
    }
    val rotation by animateFloatAsState(unwrapped.floatValue, AppTheme.motion.press(), label = "dial")
    val colors = MaterialTheme.colorScheme
    val dialFill = Glass.colors.card
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurfaceVariant)
    val north = MaterialTheme.typography.titleMedium.copy(color = colors.error)
    val marks = listOf("U" to 0f, "T" to 90f, "S" to 180f, "B" to 270f)
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(dialFill, r)
        drawCircle(colors.outlineVariant, r, style = Stroke(1.dp.toPx()))
        rotate(rotation, c) {
            for (deg in 0 until 360 step 10) {
                val major = deg % 30 == 0
                val len = if (major) 14.dp.toPx() else 7.dp.toPx()
                rotate(deg.toFloat(), c) {
                    drawLine(
                        colors.outline,
                        Offset(c.x, c.y - r + 6.dp.toPx()),
                        Offset(c.x, c.y - r + 6.dp.toPx() + len),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
            }
            // Ka'bah marker: a filled square on a line from the centre.
            rotate(bearing.toFloat(), c) {
                val tip = c.y - r + 56.dp.toPx()
                drawLine(colors.primary, c, Offset(c.x, tip + 14.dp.toPx()), strokeWidth = 3.dp.toPx())
                val s = 18.dp.toPx()
                drawRect(KaabaBlack, topLeft = Offset(c.x - s / 2, tip - s / 2), size = androidx.compose.ui.geometry.Size(s, s))
                drawRect(colors.outline, topLeft = Offset(c.x - s / 2, tip - s / 2), size = androidx.compose.ui.geometry.Size(s, s), style = Stroke(1.dp.toPx()))
                drawRect(AppGold, topLeft = Offset(c.x - s / 2, tip - s / 2 + s * 0.25f), size = androidx.compose.ui.geometry.Size(s, s * 0.12f))
            }
        }
        // Cardinal letters stay upright; only their positions turn with the dial.
        marks.forEach { (label, deg) ->
            val layout = measurer.measure(label, if (deg == 0f) north else labelStyle)
            val a = Math.toRadians((deg + rotation).toDouble())
            val d = r - 34.dp.toPx()
            val x = c.x + d * kotlin.math.sin(a).toFloat()
            val y = c.y - d * kotlin.math.cos(a).toFloat()
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
        }
        // Fixed pointer: where the top of the phone faces.
        val pointer = Path().apply {
            moveTo(c.x, c.y - r - 2.dp.toPx())
            lineTo(c.x - 10.dp.toPx(), c.y - r + 16.dp.toPx())
            lineTo(c.x + 10.dp.toPx(), c.y - r + 16.dp.toPx())
            close()
        }
        drawPath(pointer, if (aligned) colors.primary else colors.onSurface)
        drawCircle(colors.primary, 6.dp.toPx(), c)
    }
}

private val AppGold = androidx.compose.ui.graphics.Color(0xFFC9A227)
private val KaabaBlack = androidx.compose.ui.graphics.Color(0xFF1C1C18)
