package id.haho.muslimdaily.island

import androidx.compose.animation.AnimatedContent
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
            IslandMode.BIG -> BigCard(texts, ink, onOpen, onDismiss)
            IslandMode.CARD -> Card(texts, ink, onToggle, onOpen)
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

@Composable
private fun Card(texts: IslandTexts, ink: Color, onToggle: () -> Unit, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(ink)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = texts.description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(texts.prayerName, color = IslandText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(texts.clock, color = IslandText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(texts.left, color = IslandAccent, fontSize = 15.sp)
        Text(texts.place, color = IslandMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(IslandAccent)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(texts.openLabel, color = IslandInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BigCard(texts: IslandTexts, ink: Color, onOpen: () -> Unit, onDismiss: () -> Unit) {
    // Announces itself to TalkBack once when it appears.
    Column(
        modifier = Modifier
            .width(340.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(ink)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = texts.description
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(texts.left, color = IslandAccent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(texts.prayerName, color = IslandText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(texts.clock, color = IslandText, fontSize = 24.sp)
        Text(texts.place, color = IslandMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(texts.dismissLabel, color = IslandText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(IslandAccent)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(texts.openLabel, color = IslandInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
