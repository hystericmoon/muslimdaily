package id.haho.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import id.haho.design.theme.HahoTheme
import kotlin.math.abs

/**
 * A slider whose zero sits in the middle (exposure, warmth, a straighten angle): the track fills
 * in the accent from the centre to the thumb, with a tick at zero. 48dp tall.
 *
 * Accessibility: TalkBack reads [name] and [valueDescription] (say "plus 0.30", not "0.30"); the
 * volume keys call [onStep] with +1 / -1 while it has focus; [onReset] becomes a custom action
 * named [resetLabel], so there is always a non-drag way back to zero.
 *
 * @param valueRange symmetric around zero, e.g. -1f..1f.
 * @param steps as for Material's Slider; pick an odd count so zero is a stop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterZeroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    name: String,
    valueDescription: String,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val track = scheme.outline
    val tick = scheme.onSurfaceVariant
    val thumb = scheme.onSurface
    val span = valueRange.endInclusive - valueRange.start
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        thumb = { Box(Modifier.size(ThumbSize).background(thumb, CircleShape)) },
        track = { _ ->
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val y = size.height / 2
                val h = 4.dp.toPx()
                val r = CornerRadius(h / 2)
                drawRoundRect(track, Offset(0f, y - h / 2), Size(size.width, h), r)
                val centre = (0f - valueRange.start) / span * size.width
                val pos = (value - valueRange.start) / span * size.width
                drawRoundRect(accent, Offset(minOf(centre, pos), y - h / 2), Size(abs(pos - centre), h), r)
                val tw = 2.dp.toPx()
                val th = 12.dp.toPx()
                drawRect(tick, Offset(centre - tw / 2, y - th / 2), Size(tw, th))
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HahoTheme.dimens.minTouchTarget)
            .onPreviewKeyEvent { e ->
                if (!enabled || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.VolumeUp -> { onStep(1); true }
                    Key.VolumeDown -> { onStep(-1); true }
                    else -> false
                }
            }
            .semantics {
                contentDescription = name
                stateDescription = valueDescription
                if (onReset != null && resetLabel != null) {
                    customActions = listOf(CustomAccessibilityAction(resetLabel) { onReset(); true })
                }
            },
    )
}

private val ThumbSize = 20.dp
