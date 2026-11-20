package id.haho.muslimdaily.island

import android.graphics.Matrix
import android.os.Build
import android.graphics.RectF
import android.graphics.SweepGradient
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.dismiss
import kotlinx.coroutines.launch
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.haho.muslimdaily.data.settings.IslandSettings

/** The island is black, like the camera it wraps, so its colours do not follow the theme. */
private val IslandInk = Color(0xFF000000)
private val IslandText = Color(0xFFF2F5F3)
private val IslandMuted = Color(0xFFB4C2BA)
private val IslandAccent = Color(0xFF5FD3A6)
private val IslandTrack = Color(0xFF2B322E)

/** Everything the overlay draws is already formatted, so this stays free of Android resources. */
data class IslandTexts(
    val prayerName: String,
    val clock: String,
    val left: String,
    /** The right side of the collapsed pill: the countdown or the time, as the user chose. */
    val pillRight: String,
    val place: String,
    val openLabel: String,
    val dismissLabel: String,
    val description: String,
    /** What the card and alert say to TalkBack: tapping opens Adwam, swiping closes. */
    val cardDescription: String,
    /** The arc card: the five prayers' names and times, each one's place on the arc (0 to 1), the next one's index, and where now is. */
    val arcNames: List<String>,
    val arcTimes: List<String>,
    val arcTicks: List<Float>,
    val arcNext: Int,
    val arcProgress: Float,
    /** "besok" when the next prayer is tomorrow's Subuh, otherwise null. */
    val tomorrow: String?,
    /** The alert's line: how far through the time since it opened, and the time it opened. */
    val alertProgress: Float,
    val alertStart: String,
)

enum class IslandMode { PILL, CARD, BIG }

/**
 * The collapsed pill (name on the left, countdown or time on the right, a gap for the camera
 * between), a card after a tap, and the big card before a prayer. Every size comes from
 * [config]; the touch target never drops below 48 dp.
 *
 * Switching mode snaps the size (one window resize) and fades and scales the content with layer
 * transforms only. Animating the size would resize the overlay window on every frame, which is
 * what made the first version crawl.
 */
@Composable
fun IslandOverlay(
    texts: IslandTexts,
    config: IslandSettings,
    gapDp: Int,
    mode: IslandMode,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit = {},
    haptics: Boolean = true,
    /** Height of the system status bar; the open card and alert cover it with a solid band. */
    statusBarDp: Int = 0,
) {
    val ink = IslandInk.copy(alpha = config.opacityPercent / 100f)
    // Haptics go through the overlay's own View (no VIBRATE permission) and follow the Settings switch.
    // No tick when an alert appears: only touch, the close threshold, and open or close.
    val view = LocalView.current
    val tick = remember(view, haptics) { { type: Int -> if (haptics) view.performHapticFeedback(type) } }
    val openTick = { tick(HapticFeedbackConstants.CONTEXT_CLICK); Unit }
    val closeTick = { tick(doneHaptic()); Unit }
    val toggleTick = { openTick(); onToggle() }
    val openAppTick = { openTick(); onOpen() }
    val dismissTick = { closeTick(); onDismiss() }
    CompositionLocalProvider(LocalIslandTick provides tick) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(130)) + scaleIn(tween(160), initialScale = 0.88f, transformOrigin = TransformOrigin(0.5f, 0f)),
                initialContentExit = fadeOut(tween(70)),
                sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
            )
        },
        contentAlignment = Alignment.TopCenter,
        label = "island",
    ) { target ->
        when (target) {
            IslandMode.BIG -> OpenFrame(config, IslandMode.BIG, statusBarDp) { AlertCard(texts, config, gapDp, ink, openAppTick, dismissTick) }
            IslandMode.CARD -> OpenFrame(config, IslandMode.CARD, statusBarDp) { ArcCard(texts, config, gapDp, ink, openAppTick, dismissTick) }
            IslandMode.PILL -> Pill(texts, config, gapDp, ink, toggleTick)
        }
    }
    }
}

/** The pill's own place, or the card's or alert's (which follow the pill's until set). */
fun IslandSettings.offsetX(mode: IslandMode): Int = when (mode) {
    IslandMode.PILL -> offsetXDp
    IslandMode.CARD -> cardOffsetXDp ?: offsetXDp
    IslandMode.BIG -> bigOffsetXDp ?: offsetXDp
}

fun IslandSettings.offsetY(mode: IslandMode): Int = when (mode) {
    IslandMode.PILL -> offsetYDp
    IslandMode.CARD -> cardOffsetYDp ?: offsetYDp
    IslandMode.BIG -> bigOffsetYDp ?: offsetYDp
}

/**
 * The open card and alert live in a window as wide as the screen. Its top band, as tall as the
 * status bar, is solid black whatever the opacity, so the clock and icons never show through or
 * collide with the prayer name. The card sits on it at its own place; moved down past the bar, the
 * band is left out and the bar shows as it is.
 */
@Composable
private fun OpenFrame(config: IslandSettings, mode: IslandMode, statusBarDp: Int, content: @Composable () -> Unit) {
    val x = config.offsetX(mode)
    val y = config.offsetY(mode)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        if (statusBarDp > 0 && y <= statusBarDp) {
            Box(Modifier.fillMaxWidth().height(statusBarDp.dp).background(IslandInk))
        }
        Box(Modifier.offset(x = x.dp, y = y.dp)) { content() }
    }
}

private val LocalIslandTick = staticCompositionLocalOf<(Int) -> Unit> { {} }

/** The haptic for a finished close (Confirm from Android 11, a context click before). */
private fun doneHaptic(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK

/** One light tick when a finger lands. Watches the touch on the initial pass, so it never consumes it. */
@Composable
private fun Modifier.touchDownTick(): Modifier {
    val tick = LocalIslandTick.current
    return pointerInput(tick) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            tick(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}

@Composable
private fun Pill(texts: IslandTexts, config: IslandSettings, gapDp: Int, ink: Color, onToggle: () -> Unit) {
    // The touch target is at least 48 dp tall; the pill drawn in it has the chosen height.
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .touchDownTick()
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = texts.description },
        contentAlignment = Alignment.TopCenter,
    ) {
        // The two sides get equal width (the wider one sets it), so the gap sits in the middle of the
        // pill, and the middle of the pill is the middle of the screen, where the camera is.
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(config.heightDp.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(ink)
                .padding(horizontal = config.paddingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(texts.prayerName, color = IslandText, fontSize = config.fontSp.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Spacer(Modifier.width(if (gapDp > 0) gapDp.dp else 8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(texts.pillRight, color = IslandAccent, fontSize = config.fontSp.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/**
 * The card and big card are one control: tap anywhere to open Adwam, swipe any way to close.
 * TalkBack gets the same two as actions (activate, and a "close" custom action), so no gesture is
 * needed. The drag moves and fades the card with layer transforms only.
 */
@Composable
private fun Modifier.swipeToDismiss(onDismiss: () -> Unit): Modifier {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val current by rememberUpdatedState(onDismiss)
    val tick = LocalIslandTick.current
    var pastThreshold by remember { mutableStateOf(false) }
    return this
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            alpha = 1f - (offset.value.getDistance() / (threshold * 4)).coerceIn(0f, 0.6f)
        }
        .touchDownTick()
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    pastThreshold = false
                    if (offset.value.getDistance() > threshold) current() else scope.launch { offset.animateTo(Offset.Zero) }
                },
                onDragCancel = { pastThreshold = false; scope.launch { offset.animateTo(Offset.Zero) } },
            ) { change, drag ->
                change.consume()
                scope.launch { offset.snapTo(offset.value + drag) }
                // One tick each time the drag crosses the 48 dp close line, so the finger knows it will close.
                val past = (offset.value + drag).getDistance() > threshold
                if (past != pastThreshold) {
                    pastThreshold = past
                    if (past) tick(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        }
}

private fun Modifier.openOrDismiss(texts: IslandTexts, onOpen: () -> Unit, onDismiss: () -> Unit): Modifier = this
    .clickable(onClickLabel = texts.openLabel, role = Role.Button, onClick = onOpen)
    .semantics {
        contentDescription = texts.cardDescription
        customActions = listOf(CustomAccessibilityAction(texts.dismissLabel) { onDismiss(); true })
        dismiss { onDismiss(); true }
    }

private const val CARD_PAD_H = 24

/** True when the system's animations are off (Reduce motion); read once when a card appears. */
@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * Whether [left] and [right] each fit in their half of a card of [widthDp] with the camera gap
 * between them. When they do not (large text, a long countdown, English), the row stacks.
 */
@Composable
private fun rememberFlankFits(
    widthDp: Int,
    gapDp: Int,
    left: AnnotatedString,
    leftStyle: TextStyle,
    right: String,
    rightStyle: TextStyle,
    rightPadDp: Int = 0,
): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val inner = with(density) { (widthDp - 2 * CARD_PAD_H).dp.toPx() }
    val gap = with(density) { maxOf(gapDp, 8).dp.toPx() }
    val side = (inner - gap) / 2
    val l = measurer.measure(left, base.merge(leftStyle), softWrap = false, maxLines = 1).size.width
    val r = measurer.measure(AnnotatedString(right), base.merge(rightStyle), softWrap = false, maxLines = 1).size.width +
        with(density) { (2 * rightPadDp).dp.toPx() }
    return l <= side && r <= side
}

/** Name on the left, countdown on the right, the camera in the gap between; stacked when [stacked]. */
@Composable
private fun FlankRow(stacked: Boolean, gapDp: Int, left: @Composable () -> Unit, right: @Composable () -> Unit) {
    if (stacked) {
        Column(
            Modifier.fillMaxWidth().padding(top = if (gapDp > 0) 40.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            left()
            right()
        }
    } else {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { left() }
            Spacer(Modifier.width(maxOf(gapDp, 8).dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { right() }
        }
    }
}

private val GlowClear = 0x005FD3A6
private val GlowBright = 0xF25FD3A6.toInt()

/**
 * A light that runs round the card's edge every 4 s while the card is open. Drawn in the draw
 * phase only: one gradient whose rotation changes per frame, no layout and no recomposition. With
 * the system's animations off it is a still edge. Skill `motion`: the owner chose this on 20 Nov
 * as the one exception to "no looping animation", because the island is a short-lived overlay on
 * top of other apps, not a screen where people read; it stops with the card.
 */
@Composable
private fun Modifier.travellingLight(active: Boolean, corner: Dp): Modifier {
    if (!active) return this
    val reduced = rememberReducedMotion()
    val angle: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(200f) }
    } else {
        rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
            label = "glowAngle",
        )
    }
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val shader = SweepGradient(w / 2, h / 2, intArrayOf(GlowClear, GlowClear, GlowBright, GlowClear), floatArrayOf(0f, 0.55f, 0.85f, 1f))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke
        }
        val matrix = Matrix()
        val rect = RectF(stroke / 2, stroke / 2, w - stroke / 2, h - stroke / 2)
        val radius = corner.toPx() - stroke / 2
        onDrawWithContent {
            drawContent()
            matrix.setRotate(angle.value, w / 2, h / 2)
            shader.setLocalMatrix(matrix)
            paint.shader = shader
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, paint) }
        }
    }
}

/** The prayer arc card: today's five prayers on an arc from Subuh to Isya, a marker for now. */
@Composable
private fun ArcCard(texts: IslandTexts, config: IslandSettings, gapDp: Int, ink: Color, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val k = config.cardFontSp / 26f
    val head = TextStyle(fontSize = config.cardFontSp.sp, fontWeight = FontWeight.ExtraBold)
    val tag = texts.tomorrow
    val left = buildAnnotatedString {
        append(texts.prayerName)
        if (tag != null) {
            withStyle(SpanStyle(fontSize = (14 * k).sp, fontWeight = FontWeight.SemiBold, color = IslandMuted)) {
                append(" ")
                append(tag)
            }
        }
    }
    val stacked = !rememberFlankFits(config.cardWidthDp, gapDp, left, head, texts.left, head)
    Column(
        modifier = Modifier
            .width(config.cardWidthDp.dp)
            .swipeToDismiss(onDismiss)
            .clip(RoundedCornerShape(28.dp))
            .background(ink)
            .travellingLight(config.glow, 28.dp)
            .openOrDismiss(texts, onOpen, onDismiss)
            .padding(start = CARD_PAD_H.dp, end = CARD_PAD_H.dp, bottom = 12.dp),
    ) {
        FlankRow(
            stacked = stacked,
            gapDp = gapDp,
            left = { Text(left, color = IslandText, style = head, maxLines = 1) },
            right = { Text(texts.left, color = IslandAccent, style = head, maxLines = 1) },
        )
        ArcArt(texts, k)
        PrayerRow(texts, k, config.cardWidthDp)
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { SwipeHandle() }
    }
}

/** The arc itself. Decoration: the five times below carry the same information as text. */
@Composable
private fun ArcArt(texts: IslandTexts, k: Float) {
    Box(Modifier.fillMaxWidth().heightIn(min = 92.dp)) {
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val w = size.width
            val d = density
            val y0 = 64 * d
            val path = Path().apply {
                moveTo(0f, y0)
                quadraticTo(w / 2, -22 * d, w, y0)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val length = measure.length
            val line = Stroke(width = 3 * d, cap = StrokeCap.Round)
            drawPath(path, IslandTrack, style = line)
            val done = Path()
            measure.getSegment(0f, length * texts.arcProgress, done, true)
            drawPath(done, IslandAccent, style = line)
            drawCircle(IslandMuted, 3.5f * d, measure.getPosition(0f))
            texts.arcTicks.forEachIndexed { i, fraction ->
                if (i == 0) return@forEachIndexed
                val at = measure.getPosition(length * fraction)
                if (i == texts.arcNext && texts.tomorrow == null) {
                    drawCircle(IslandAccent, 5.5f * d + 1f * d, at)
                    drawCircle(IslandInk, 5.5f * d - 1f * d, at)
                } else {
                    drawCircle(IslandMuted, 3.5f * d, at)
                }
            }
            val now = measure.getPosition(length * texts.arcProgress)
            drawCircle(IslandInk, 9.5f * d, now)
            drawCircle(IslandAccent, 6.5f * d, now)
        }
        Text(
            texts.clock,
            Modifier.align(Alignment.TopCenter).padding(top = 34.dp),
            color = IslandText,
            fontSize = (28 * k).sp,
            fontWeight = FontWeight.Light,
        )
    }
}

/** The five prayers in a row, the next one in the accent and underlined; a list when they do not fit. */
@Composable
private fun PrayerRow(texts: IslandTexts, k: Float, widthDp: Int) {
    val timeSize = (13 * k).coerceAtLeast(11f)
    val nameSize = (12 * k).coerceAtLeast(11f)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = LocalTextStyle.current
    val column = with(density) { ((widthDp - 2 * CARD_PAD_H).dp / 5).toPx() - 4.dp.toPx() }
    val widest = texts.arcNames.indices.maxOf { i ->
        maxOf(
            measurer.measure(texts.arcNames[i], base.merge(TextStyle(fontSize = nameSize.sp)), softWrap = false, maxLines = 1).size.width,
            measurer.measure(texts.arcTimes[i], base.merge(TextStyle(fontSize = timeSize.sp, fontWeight = FontWeight.ExtraBold)), softWrap = false, maxLines = 1).size.width,
        )
    }
    if (widest <= column) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            texts.arcNames.indices.forEach { i ->
                val next = i == texts.arcNext
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        texts.arcTimes[i],
                        color = if (next) IslandAccent else IslandText,
                        fontSize = timeSize.sp,
                        fontWeight = if (next) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(texts.arcNames[i], color = IslandMuted, fontSize = nameSize.sp, maxLines = 1, softWrap = false)
                    if (next && i == 0 && texts.tomorrow != null) {
                        Text(texts.tomorrow, color = IslandAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    if (next) {
                        Box(
                            Modifier
                                .padding(top = 3.dp)
                                .fillMaxWidth(0.8f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(IslandAccent),
                        )
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            texts.arcNames.indices.forEach { i ->
                val next = i == texts.arcNext
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (next && i == 0 && texts.tomorrow != null) texts.arcNames[i] + " " + texts.tomorrow else texts.arcNames[i],
                        color = if (next) IslandAccent else IslandMuted,
                        fontSize = nameSize.sp,
                        fontWeight = if (next) FontWeight.ExtraBold else FontWeight.Normal,
                    )
                    Text(
                        texts.arcTimes[i],
                        color = if (next) IslandAccent else IslandText,
                        fontSize = timeSize.sp,
                        fontWeight = if (next) FontWeight.ExtraBold else FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * The prayer alert, shown by itself before a prayer: name and a filled countdown beside the
 * camera, time and place under them, and a line over the time since it opened. The chip is filled
 * so the alert stays the loudest thing on screen when animations are off.
 */
@Composable
private fun AlertCard(texts: IslandTexts, config: IslandSettings, gapDp: Int, ink: Color, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val k = config.bigFontSp / 30f
    val head = TextStyle(fontSize = config.bigFontSp.sp, fontWeight = FontWeight.ExtraBold)
    val stacked = !rememberFlankFits(config.bigWidthDp, gapDp, AnnotatedString(texts.prayerName), head, texts.left, head, rightPadDp = 14)
    val small = (13 * k).coerceAtLeast(12f)
    // Announces itself to TalkBack once when it appears.
    Column(
        modifier = Modifier
            .width(config.bigWidthDp.dp)
            .swipeToDismiss(onDismiss)
            .clip(RoundedCornerShape(32.dp))
            .background(ink)
            .travellingLight(config.glow, 32.dp)
            .openOrDismiss(texts, onOpen, onDismiss)
            .padding(start = CARD_PAD_H.dp, end = CARD_PAD_H.dp, bottom = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        FlankRow(
            stacked = stacked,
            gapDp = gapDp,
            left = { Text(texts.prayerName, color = IslandText, style = head, maxLines = 1) },
            right = {
                Text(
                    texts.left,
                    color = IslandInk,
                    style = head,
                    maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(IslandAccent).padding(horizontal = 14.dp, vertical = 2.dp),
                )
            },
        )
        if (stacked) {
            Text(
                texts.clock + " · " + texts.place,
                color = IslandMuted,
                fontSize = small.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(texts.clock, color = IslandMuted, fontSize = small.sp)
                Text(
                    texts.place,
                    color = IslandMuted,
                    fontSize = small.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp),
                )
            }
            AlertLine(texts.alertProgress)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(texts.alertStart, color = IslandMuted, fontSize = 11.sp)
                Text(texts.clock, color = IslandMuted, fontSize = 11.sp)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { SwipeHandle() }
    }
}

/** Solid where the alert has been open, dotted for the time left, a marker for now. Decoration. */
@Composable
private fun AlertLine(progress: Float) {
    Canvas(Modifier.fillMaxWidth().padding(top = 14.dp).height(12.dp)) {
        val d = density
        val pad = 6 * d
        val y = size.height / 2
        val x = pad + (size.width - 2 * pad) * progress
        drawLine(IslandAccent, Offset(pad, y), Offset(x, y), strokeWidth = 3 * d, cap = StrokeCap.Round)
        drawLine(
            IslandMuted.copy(alpha = 0.7f),
            Offset(x, y),
            Offset(size.width - pad, y),
            strokeWidth = 2 * d,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f * d, 5 * d)),
        )
        drawCircle(IslandInk, 9 * d, Offset(x, y))
        drawCircle(IslandAccent, 6 * d, Offset(x, y))
    }
}

/** The grab bar at the foot of a card: the cue that it can be swiped away. Decoration only. */
@Composable
private fun SwipeHandle() {
    Box(
        Modifier
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(IslandMuted.copy(alpha = 0.45f)),
    )
}
