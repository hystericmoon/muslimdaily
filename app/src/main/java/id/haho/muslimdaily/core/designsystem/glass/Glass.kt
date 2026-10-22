package id.haho.muslimdaily.core.designsystem.glass

import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import kotlin.math.max

/**
 * Frosted "liquid glass" tokens. Surfaces are translucent fills over a soft aurora backdrop, with
 * a light rim (bright at the top edge, fading downward) and a faint specular sheen. Alphas were
 * chosen so text keeps ≥ 4.5:1 against the worst-case backdrop; see GlassContrastTest.
 *
 * When glass is off (Settings → Tampilan → Efek kaca) every role resolves to an opaque colour.
 */
@Immutable
data class GlassColors(
    val enabled: Boolean,
    val base: Color,
    /** Backdrop glows: (colour, alpha) painted as large radial gradients. */
    val glows: List<Pair<Color, Float>>,
    /** Cards and grouped lists. */
    val card: Color,
    /** Chrome: nav bar and top bar when scrolled. Only ever sits over the backdrop. */
    val chrome: Color,
    /** Bottom sheets and dialogs. Nearly opaque: on Android 11 and older nothing behind is blurred. */
    val sheet: Color,
    /** Tinted hero glass (next prayer, review queue). */
    val tinted: Color,
    val onTinted: Color,
    /** Primary actions: teal glass. */
    val primary: Color,
    val onPrimary: Color,
    /** Secondary (tonal) actions. */
    val tonal: Color,
    val onTonal: Color,
    /** Outlined / clear actions. */
    val clear: Color,
    val rimTop: Color,
    val rimBottom: Color,
    val sheen: Color,
) {
    val rim: Brush get() = Brush.verticalGradient(listOf(rimTop, rimBottom))
    fun rimStroke(width: Dp = 1.dp) = BorderStroke(width, rim)

    /** The darkest/brightest backdrop a glass surface can sit on, for contrast checks. */
    fun backdropExtremes(): List<Color> {
        val each = glows.map { (c, a) -> c.copy(alpha = a).compositeOver(base) }
        val stacked = glows.fold(base) { acc, (c, a) -> c.copy(alpha = a).compositeOver(acc) }
        return listOf(base) + each + stacked
    }
}

val LocalGlass = staticCompositionLocalOf<GlassColors> { error("GlassColors not provided") }

/** Counts open sheets/dialogs so the app behind them can frost (blur) while they are shown. */
class OverlayState {
    var open by mutableIntStateOf(0)
        internal set
}

val LocalOverlayState = staticCompositionLocalOf { OverlayState() }

object Glass {
    val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlass.current
}

/**
 * The aurora backdrop behind every screen: parchment (or soft dark) with three large, very soft
 * glows. Static by design; it never animates.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val g = Glass.colors
    Box(modifier.fillMaxSize().background(g.base)) {
        if (g.enabled) {
            Canvas(Modifier.fillMaxSize()) {
                val anchors = listOf(
                    Offset(size.width * 0.05f, size.height * 0.02f),
                    Offset(size.width * 1.0f, size.height * 0.62f),
                    Offset(size.width * 0.15f, size.height * 1.0f),
                )
                g.glows.forEachIndexed { i, (color, alpha) ->
                    val center = anchors[i % anchors.size]
                    val radius = max(size.width, size.height) * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
        }
        content()
    }
}

/** Frosts the content behind open overlays (Android 12+; older versions just dim via the scrim). */
@Composable
fun Modifier.frostWhenOverlayOpen(): Modifier {
    val overlays = LocalOverlayState.current
    val target = if (overlays.open > 0 && Glass.colors.enabled) 18.dp else 0.dp
    val radius by animateDpAsState(target, AppTheme.motion.fade(), label = "frost")
    return if (radius > 0.dp) this.blur(radius) else this
}

/**
 * Paints a glass fill behind content: the translucent [fill] plus a specular sheen (a soft white
 * wash at the top of [shape] that fades out by [sheenStop] of the height). Text drawn on top keeps
 * its contrast because the sheen only ever lightens what is behind it.
 */
fun Modifier.glassFill(
    shape: Shape,
    fill: Color,
    colors: GlassColors,
    sheen: Color = colors.sheen,
    sheenStop: Float = 0.5f,
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, fill)
    if (colors.enabled && sheen.alpha > 0f) {
        drawOutline(outline, Brush.verticalGradient(0f to sheen, sheenStop to sheen.copy(alpha = 0f)))
    }
}

/**
 * Gentle press feedback: scales to 97% with a spring while pressed. Snaps (no motion) when the
 * system "remove animations" setting is on.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, AppTheme.motion.press(), label = "press")
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Convenience for components that need their own interaction source for [pressScale]. */
@Composable
fun rememberPressSource(): MutableInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
