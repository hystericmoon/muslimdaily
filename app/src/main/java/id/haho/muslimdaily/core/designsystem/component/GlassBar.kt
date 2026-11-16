package id.haho.muslimdaily.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import id.haho.design.glass.Glass

/**
 * What the glass bar blurs: the host's content, recorded into a layer, and where the bar starts in the host.
 */
internal class BarBackdrop(val layer: GraphicsLayer) {
    var barTop by mutableFloatStateOf(0f)
}

private val LocalBarBackdrop = compositionLocalOf<BarBackdrop?> { null }

/**
 * A screen with one media bar at the bottom. The content fills the whole area and is told the bar's height
 * ([content]'s argument) to pad its end by, so text scrolls behind the bar and can still scroll clear of it.
 * [bar] draws a [GlassBarSurface]; null shows no bar (and no padding).
 */
@Composable
fun GlassBarHost(
    bar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable (barHeight: Dp) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { BarBackdrop(layer) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            content(if (bar == null) 0.dp else with(density) { barHeightPx.toDp() })
        }
        if (bar != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height }
                    .onGloballyPositioned { backdrop.barTop = it.positionInParent().y },
            ) {
                CompositionLocalProvider(LocalBarBackdrop provides backdrop) { bar() }
            }
        }
    }
}

/**
 * The media bar's surface: full width, translucent glass over a blur of what scrolls behind it (Android 12 and
 * newer; older versions get a denser tint), a light rim along the top and no handle. With Glass effect off it is a solid
 * surface. [expanded] (a dock whose settings are open) uses the near-opaque frosted sheet colour so sliders and lists stay
 * readable. [insets] pads the gesture bar and side cut-outs; leave it off where a tab bar below takes them.
 */
@Composable
fun GlassBarSurface(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    insets: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val g = Glass.colors
    val backdrop = LocalBarBackdrop.current
    val blur = g.enabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !LocalInspectionMode.current
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val fill: Color = when {
        !g.enabled -> base
        expanded -> g.sheet
        blur -> base.copy(alpha = BLUR_TINT)
        else -> base.copy(alpha = PLAIN_TINT)
    }
    val blurLayer = rememberGraphicsLayer()
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                if (blur && !expanded && backdrop != null) {
                    blurLayer.renderEffect = BlurEffect(BLUR_RADIUS, BLUR_RADIUS, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(size = size.toIntSize()) {
                        translate(0f, -backdrop.barTop) { drawLayer(backdrop.layer) }
                    }
                    drawLayer(blurLayer)
                }
                drawRect(fill)
                if (g.enabled) {
                    drawRect(Brush.verticalGradient(0f to g.sheen, 0.6f to Color.Transparent))
                    drawLine(g.rimTop, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                }
            }
            .then(
                if (insets) {
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

private const val BLUR_TINT = 0.55f
private const val PLAIN_TINT = 0.92f
private const val BLUR_RADIUS = 24f
