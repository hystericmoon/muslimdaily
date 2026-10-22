package id.haho.muslimdaily.core.designsystem.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.core.designsystem.theme.AppTheme

/**
 * Fades and lifts an element in the first time its screen opens; [index] staggers siblings by
 * 40 ms. Plays once per screen (not again on recomposition or when returning to it). No motion
 * when the system "remove animations" setting is on.
 */
@Composable
fun Modifier.enterReveal(index: Int = 0): Modifier {
    val motion = AppTheme.motion
    if (motion.reduced || LocalInspectionMode.current) return this
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            progress.animateTo(1f, motion.enter(index))
            played = true
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
    }
}

/** A short "pop" (scale up, settle back) each time [trigger] becomes true after the first frame. */
@Composable
fun Modifier.popOn(trigger: Boolean, peak: Float = 1.18f): Modifier {
    val motion = AppTheme.motion
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first) {
            first = false
        } else if (trigger && !motion.reduced) {
            scale.animateTo(peak, tween(90))
            scale.animateTo(1f, motion.countTap())
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** Rotates a chevron to point up when [expanded]. */
@Composable
fun Modifier.chevronRotation(expanded: Boolean): Modifier {
    val angle by animateFloatAsState(if (expanded) 180f else 0f, AppTheme.motion.layout(), label = "chevron")
    return graphicsLayer { rotationZ = angle }
}
