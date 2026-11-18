package id.haho.muslimdaily.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.AdwamMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The motion of a mark turning done: the check draws itself and the control pulses once (1.0, 1.08,
 * 1.0). It plays only when [done] flips from false to true while the same [key] (the item) stays on
 * screen: arriving on an item that is already done, or undoing, shows the end state with no motion.
 * With the system's remove-animations setting on, nothing moves. The haptic tap is the caller's.
 */
@Stable
class MarkMotion internal constructor() {
    internal val draw = Animatable(1f)
    internal val scale = Animatable(1f)

    /** 0 (nothing drawn) to 1 (the whole check). */
    val drawProgress: Float get() = draw.value

    /** 1 at rest, [AdwamMotion.MARK_PULSE_SCALE] at the top of the pulse. */
    val pulseScale: Float get() = scale.value
}

@Composable
fun rememberMarkMotion(done: Boolean, key: Any): MarkMotion {
    val reduced = HahoTheme.motion.reduced
    val motion = remember { MarkMotion() }
    var wasDone by remember(key) { mutableStateOf(done) }
    LaunchedEffect(key, done, reduced) {
        if (done && !wasDone && !reduced) {
            motion.draw.snapTo(0f)
            coroutineScope {
                launch { motion.draw.animateTo(1f, AdwamMotion.draw(reduced = false)) }
                launch {
                    motion.scale.animateTo(AdwamMotion.MARK_PULSE_SCALE, AdwamMotion.pulseUp(reduced = false))
                    motion.scale.animateTo(1f, AdwamMotion.pulseDown(reduced = false))
                }
            }
        } else {
            motion.draw.snapTo(1f)
            motion.scale.snapTo(1f)
        }
        wasDone = done
    }
    return motion
}

/** Applies [MarkMotion]'s pulse to the control that holds the check (a transform, so its 48 dp target does not change). */
fun Modifier.markPulse(motion: MarkMotion): Modifier = graphicsLayer {
    val s = motion.pulseScale
    scaleX = s
    scaleY = s
}

/**
 * A check mark drawn as a stroke so it can draw itself: [progress] 0 to 1 along its path. At 1 it
 * is the full check, 24 dp like the icon it replaces, in the current content colour.
 */
@Composable
fun DrawnCheck(progress: () -> Float, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier.size(24.dp)) {
        val unit = size.width / 24f
        val full = Path().apply {
            moveTo(5.2f * unit, 12.4f * unit)
            lineTo(9.7f * unit, 16.9f * unit)
            lineTo(18.8f * unit, 7.6f * unit)
        }
        val shown = Path()
        val measure = PathMeasure().apply { setPath(full, forceClosed = false) }
        measure.getSegment(0f, measure.length * progress().coerceIn(0f, 1f), shown, startWithMoveTo = true)
        drawPath(
            shown,
            color = color,
            style = Stroke(width = 2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
