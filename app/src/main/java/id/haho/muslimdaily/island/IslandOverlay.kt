package id.haho.muslimdaily.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
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
    /** What the card and big card say to TalkBack: tapping opens Adwam, swiping closes. */
    val cardDescription: String,
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
) {
    val ink = IslandInk.copy(alpha = config.opacityPercent / 100f)
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
            IslandMode.BIG -> BigCard(texts, config, ink, onOpen, onDismiss)
            IslandMode.CARD -> Card(texts, config, ink, onOpen, onDismiss)
            IslandMode.PILL -> Pill(texts, config, gapDp, ink, onToggle)
        }
    }
}

@Composable
private fun Pill(texts: IslandTexts, config: IslandSettings, gapDp: Int, ink: Color, onToggle: () -> Unit) {
    // The touch target is at least 48 dp tall; the pill drawn in it has the chosen height.
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
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
    return this
        .graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            alpha = 1f - (offset.value.getDistance() / (threshold * 4)).coerceIn(0f, 0.6f)
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    if (offset.value.getDistance() > threshold) current() else scope.launch { offset.animateTo(Offset.Zero) }
                },
                onDragCancel = { scope.launch { offset.animateTo(Offset.Zero) } },
            ) { change, drag ->
                change.consume()
                scope.launch { offset.snapTo(offset.value + drag) }
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

@Composable
private fun Card(texts: IslandTexts, config: IslandSettings, ink: Color, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val k = config.cardFontSp / 20f
    Column(
        modifier = Modifier
            .width(config.cardWidthDp.dp)
            .swipeToDismiss(onDismiss)
            .clip(RoundedCornerShape(28.dp))
            .background(ink)
            .openOrDismiss(texts, onOpen, onDismiss)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(texts.prayerName, color = IslandText, fontSize = config.cardFontSp.sp, fontWeight = FontWeight.SemiBold)
            Text(texts.clock, color = IslandText, fontSize = config.cardFontSp.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(texts.left, color = IslandAccent, fontSize = (15 * k).sp)
        Text(texts.place, color = IslandMuted, fontSize = (13 * k).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BigCard(texts: IslandTexts, config: IslandSettings, ink: Color, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val k = config.bigFontSp / 40f
    // Announces itself to TalkBack once when it appears.
    Column(
        modifier = Modifier
            .width(config.bigWidthDp.dp)
            .swipeToDismiss(onDismiss)
            .clip(RoundedCornerShape(32.dp))
            .background(ink)
            .openOrDismiss(texts, onOpen, onDismiss)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(texts.left, color = IslandAccent, fontSize = (20 * k).sp, fontWeight = FontWeight.SemiBold)
        Text(texts.prayerName, color = IslandText, fontSize = config.bigFontSp.sp, fontWeight = FontWeight.Bold)
        Text(texts.clock, color = IslandText, fontSize = (24 * k).sp)
        Text(texts.place, color = IslandMuted, fontSize = (14 * k).coerceAtLeast(12f).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
