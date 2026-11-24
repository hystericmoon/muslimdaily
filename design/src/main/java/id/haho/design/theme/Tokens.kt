package id.haho.design.theme

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii. Large and soft: cards and sheets use extraLarge (28dp); buttons are pills. */
val HahoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp), // chips, badges
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp), // cards, sheets
)

/** A 4dp-based spacing scale (with a 2dp hairline step). */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

/** Fixed sizes shared by every screen. App-specific sizes belong in the app. */
@Immutable
data class Dimens(
    val screenGutter: Dp = 16.dp,
    val cardPadding: Dp = 20.dp,
    val minTouchTarget: Dp = 48.dp,
    /** Primary controls (play, main actions) sit above the 48dp minimum. */
    val largeTouchTarget: Dp = 64.dp,
    val icon: Dp = 24.dp,
    val smallIcon: Dp = 18.dp,
    val divider: Dp = 1.dp,
    /** Content is centred and capped at this width on tablets and in landscape. */
    val maxContentWidth: Dp = 640.dp,
)

/**
 * Motion is calm and short: springs without bounce for layout and state changes, a light bounce
 * only for a deliberate tap (a counter, a toggle that celebrates). Micro-interactions (press,
 * toggle, expand) stay under ~250 ms. When the system "remove animations" setting is on, every
 * spec snaps.
 */
@Immutable
data class Motion(val reduced: Boolean = false) {
    /** A light bounce for a deliberate tap (e.g. a counter step). */
    fun <T> countTap(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    /** Moving to the next card / page in a sequence. */
    fun <T> cardAdvance(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Press feedback on buttons and cards. */
    fun <T> press(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow * 2)

    /** Colour and opacity changes. */
    fun <T> fade(): FiniteAnimationSpec<T> = if (reduced) snap() else tween(220, easing = FastOutSlowInEasing)

    /** Size changes and item placement in lists. */
    fun <T> layout(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Elements entering the screen for the first time (staggered by [index], capped at 8). */
    fun <T> enter(index: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(320, delayMillis = enterDelayMillis(index), easing = EaseOutCubic)

    companion object {
        /** Stagger between siblings entering together. */
        const val STAGGER_MILLIS = 40

        /** Delay for the [index]th sibling; later siblings share the 8th slot. */
        fun enterDelayMillis(index: Int): Int = index.coerceIn(0, 8) * STAGGER_MILLIS
    }
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalDimens = staticCompositionLocalOf { Dimens() }
val LocalMotion = staticCompositionLocalOf { Motion() }
