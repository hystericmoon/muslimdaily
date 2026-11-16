package id.haho.muslimdaily.feature.duas.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.haho.design.theme.HahoTheme

/**
 * Keeps the phrase that plays in view. The text reports the active phrase's place ([report]); when the active phrase
 * changes (the audio moves on, or next or previous is pressed) the scroll brings it into the visible part of
 * [viewport], above the media bar. Nothing scrolls within one phrase, so a reader who scrolls by hand is left alone until the
 * next phrase change. With reduce motion the jump is instant.
 */
class FollowPhrase internal constructor(private val scroll: ScrollState) {
    private var viewport: Rect = Rect.Zero
    private var active: Rect? = null
    internal var bottomInsetPx: Float = 0f
    internal var marginPx: Float = 0f
    internal var animate: Boolean = true

    /** Put on the scrolling container, before its scroll modifier, so it measures the visible window. */
    val viewportModifier: Modifier = Modifier.onGloballyPositioned { viewport = it.boundsInRoot() }

    /** Called by the text with the active phrase's bounds in the window. */
    fun report(bounds: Rect) {
        active = bounds
    }

    internal suspend fun reveal() {
        val phrase = active ?: return
        if (viewport.height <= 0f) return
        val top = viewport.top + marginPx
        val bottom = viewport.bottom - bottomInsetPx - marginPx
        val delta = when {
            phrase.height > bottom - top -> phrase.top - top // taller than the window: start of the phrase
            phrase.top < top -> phrase.top - top
            phrase.bottom > bottom -> phrase.bottom - bottom
            else -> return
        }
        if (animate) scroll.animateScrollBy(delta) else scroll.scrollBy(delta)
    }
}

/**
 * Follows [activePhrase] in [scroll]. [bottomInset] is the height of what covers the end of the window (the glass bar).
 */
@Composable
fun rememberFollowPhrase(scroll: ScrollState, activePhrase: Int, bottomInset: Dp = 0.dp): FollowPhrase {
    val follow = remember(scroll) { FollowPhrase(scroll) }
    val density = LocalDensity.current
    follow.bottomInsetPx = with(density) { bottomInset.toPx() }
    follow.marginPx = with(density) { HahoTheme.spacing.md.toPx() }
    follow.animate = !HahoTheme.motion.reduced
    LaunchedEffect(follow, activePhrase) {
        if (activePhrase < 0) return@LaunchedEffect
        // Two frames: the new phrase has been laid out and has reported where it is.
        withFrameNanos { }
        withFrameNanos { }
        follow.reveal()
    }
    return follow
}
