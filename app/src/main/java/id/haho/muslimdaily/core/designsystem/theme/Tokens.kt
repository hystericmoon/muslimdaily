package id.haho.muslimdaily.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp), // chips
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp), // dua cards and sheets
)

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

@Immutable
data class Dimens(
    val screenGutter: Dp = 16.dp,
    val cardPadding: Dp = 20.dp,
    val minTouchTarget: Dp = 48.dp,
    /** Player controls and other primary actions sit above the 48dp minimum. */
    val largeTouchTarget: Dp = 64.dp,
    val playerMainButton: Dp = 72.dp,
    val icon: Dp = 24.dp,
    val smallIcon: Dp = 18.dp,
    val completeDisc: Dp = 56.dp,
    val highlightUnderline: Dp = 2.dp,
    val divider: Dp = 1.dp,
    val maxContentWidth: Dp = 640.dp,
    val timelineDot: Dp = 10.dp,
    val placeholderWordHeight: Dp = 28.dp,
)

/** Colour roles M3 does not cover. Gold appears only as fill or non-text indicator. */
@Immutable
data class ExtendedColors(
    val highlightFill: Color,
    val onHighlightFill: Color,
    val highlightIndicator: Color,
    val completeFill: Color,
    val onCompleteFill: Color,
)

internal val LightExtendedColors = ExtendedColors(
    highlightFill = LightTokens.highlightFill,
    onHighlightFill = LightTokens.onHighlightFill,
    highlightIndicator = LightTokens.highlightIndicator,
    completeFill = LightTokens.completeFill,
    onCompleteFill = LightTokens.onCompleteFill,
)

internal val DarkExtendedColors = ExtendedColors(
    highlightFill = DarkTokens.highlightFill,
    onHighlightFill = DarkTokens.onHighlightFill,
    highlightIndicator = DarkTokens.highlightIndicator,
    completeFill = DarkTokens.completeFill,
    onCompleteFill = DarkTokens.onCompleteFill,
)

/**
 * Motion is calm and short: springs without bounce for layout and state changes, a light bounce
 * only for the count tap. Micro-interactions (press, toggle, expand) stay under ~250 ms. When the
 * system "remove animations" setting is on, every spec snaps.
 */
@Immutable
data class Motion(val reduced: Boolean = false) {
    fun <T> countTap(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

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

    /** Elements entering the screen for the first time (staggered by [index]). */
    fun <T> enter(index: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(320, delayMillis = (index.coerceAtMost(8)) * 40, easing = EaseOutCubic)
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalDimens = staticCompositionLocalOf { Dimens() }
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
val LocalMotion = staticCompositionLocalOf { Motion() }
