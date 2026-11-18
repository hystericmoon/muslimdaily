package id.haho.muslimdaily.island

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The island is always black, like the camera it wraps, so its colours do not follow the theme. */
private val IslandBlack = Color(0xFF000000)
private val IslandText = Color(0xFFF2F5F3)
private val IslandMuted = Color(0xFFB4C2BA)
private val IslandAccent = Color(0xFF5FD3A6)

/** Everything the overlay draws is already formatted, so this stays free of Android resources. */
data class IslandTexts(
    val prayerName: String,
    val clock: String,
    val left: String,
    val place: String,
    val openLabel: String,
    val dismissLabel: String,
    val description: String,
)

/**
 * The collapsed pill (name on the left, countdown on the right, a gap for the camera between)
 * and, after a tap, a card with the time, the place and a button into the app. Both are one
 * 48dp-tall-or-more target for touch and TalkBack.
 */
@Composable
fun IslandOverlay(
    texts: IslandTexts,
    gapDp: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    big: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    Box(modifier = Modifier.animateContentSize()) {
        if (big) {
            // Ten minutes before a prayer: a large card that announces itself to TalkBack once.
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(IslandBlack)
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
                        Text(texts.openLabel, color = IslandBlack, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (expanded) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(IslandBlack)
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
                    Text(texts.openLabel, color = IslandBlack, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // The touch target is 48dp tall; the black pill drawn in it is only as tall as the camera area.
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = texts.description },
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(IslandBlack)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(texts.prayerName, color = IslandText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Spacer(Modifier.width(if (gapDp > 0) gapDp.dp else 8.dp))
                    Text(texts.left, color = IslandAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}
