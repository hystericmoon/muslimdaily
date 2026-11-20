package id.haho.muslimdaily.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import id.haho.muslimdaily.core.ui.rememberTimeFormatter
import id.haho.muslimdaily.island.offsetX
import id.haho.muslimdaily.island.offsetY
import java.time.ZoneOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.design.component.InfoNote
import id.haho.design.component.SegmentedRow
import id.haho.design.component.SettingsGroup
import id.haho.design.component.SwitchRow
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.IslandRight
import id.haho.muslimdaily.data.settings.IslandSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.island.IslandGeometry
import id.haho.muslimdaily.island.IslandMode
import id.haho.muslimdaily.island.IslandOverlay
import id.haho.muslimdaily.island.IslandTexts
import kotlin.math.roundToInt

/** Width of the camera drawn in the preview, in dp (a typical punch-hole). */
private const val PREVIEW_CAMERA_DP = 28

@Composable
fun IslandSettingsRoute(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var mode by rememberSaveable { mutableStateOf(IslandMode.PILL) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlay = Settings.canDrawOverlays(context)
        viewModel.reschedule()
    }
    AdwamScreen(
        title = stringResource(R.string.reminders_island_title),
        onBack = onBack,
        // Docked above the system bar: the mode being edited stays in reach and in view while a slider moves.
        bottomBar = { IslandModeDock(mode, onSelect = { mode = it }) },
    ) {
        settings?.let {
            IslandSettingsContent(
                settings = it,
                update = viewModel::update,
                mode = mode,
                overlayAllowed = overlay,
                onAskOverlay = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                },
            )
        }
    }
}

/** Pill, card or alert: which one the preview shows and which settings are listed. */
@Composable
fun IslandModeDock(mode: IslandMode, onSelect: (IslandMode) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm)) {
        SegmentedRow(
            title = stringResource(R.string.island_preview),
            options = IslandMode.entries,
            selected = mode,
            onSelect = onSelect,
            label = {
                stringResource(
                    when (it) {
                        IslandMode.PILL -> R.string.island_preview_pill
                        IslandMode.CARD -> R.string.island_preview_card
                        IslandMode.BIG -> R.string.island_preview_big
                    },
                )
            },
        )
    }
}

@Composable
fun IslandSettingsContent(
    settings: AppSettings,
    update: (SettingsEditor.() -> Unit) -> Unit,
    mode: IslandMode = IslandMode.PILL,
    overlayAllowed: Boolean = true,
    onAskOverlay: () -> Unit = {},
) {
    val c = settings.island
    Column(Modifier.fillMaxSize()) {
        // Pinned: the preview stays in view while the sliders below scroll.
        Box(Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm)) {
            IslandPreview(c, mode)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
            item(key = "show") {
                SettingsGroup(stringResource(R.string.island_group_show)) {
                    SwitchRow(
                        title = stringResource(R.string.reminders_island_title),
                        summary = stringResource(R.string.reminders_island_summary),
                        checked = settings.reminders.nextPrayerIsland,
                        onCheckedChange = { v ->
                            update { remIsland = v }
                            if (v && !overlayAllowed) onAskOverlay()
                        },
                        icon = R.drawable.ic_notifications,
                    )
                }
            }
            if (settings.reminders.nextPrayerIsland && !overlayAllowed) {
                item(key = "perm-overlay") {
                    PermissionCard(stringResource(R.string.reminders_island_permission), stringResource(R.string.reminders_island_allow), onAskOverlay)
                }
            }
            when (mode) {
                IslandMode.PILL -> pillSettings(c, update)
                IslandMode.CARD -> cardSettings(c, update)
                IslandMode.BIG -> bigSettings(c, update)
            }
            item(key = "look") {
                SettingsGroup(stringResource(R.string.island_group_look)) {
                    SliderRow(stringResource(R.string.island_opacity), c.opacityPercent, IslandSettings.OPACITY, "%") { v -> update { islandOpacity = v } }
                    SwitchRow(
                        title = stringResource(R.string.island_glow_title),
                        summary = stringResource(R.string.island_glow_summary),
                        checked = c.glow,
                        onCheckedChange = { v -> update { islandGlow = v } },
                        icon = R.drawable.ic_notifications,
                    )
                }
            }
            item(key = "other") {
                SettingsGroup(stringResource(R.string.island_group_other)) {
                    SwitchRow(
                        title = stringResource(R.string.island_landscape_title),
                        summary = stringResource(R.string.island_landscape_summary),
                        checked = c.hideInLandscape,
                        onCheckedChange = { v -> update { islandLandscape = v } },
                        icon = R.drawable.ic_schedule,
                    )
                    TextButton(
                        onClick = {
                            update {
                                val d = IslandSettings()
                                islandHeight = d.heightDp; islandPadding = d.paddingDp; islandFont = d.fontSp
                                islandGap = d.gapAdjustDp; islandX = d.offsetXDp; islandY = d.offsetYDp
                                islandOpacity = d.opacityPercent; islandRight = d.right
                                islandBig = d.bigCard; islandLead = d.bigCardLeadMinutes; islandLandscape = d.hideInLandscape
                                islandCardWidth = d.cardWidthDp; islandCardFont = d.cardFontSp; islandCardSeconds = d.cardSeconds
                                islandBigWidth = d.bigWidthDp; islandBigFont = d.bigFontSp; islandBigSeconds = d.bigSeconds
                                islandBigUntil = d.bigUntilPrayer; islandGlow = d.glow
                                setIslandCardPosition(null, null); setIslandBigPosition(null, null)
                            }
                        },
                        modifier = Modifier.padding(horizontal = HahoTheme.spacing.sm),
                    ) { Text(stringResource(R.string.island_reset)) }
                }
            }
        }
    }
}

private fun LazyListScope.pillSettings(c: IslandSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    item(key = "size") {
        SettingsGroup(stringResource(R.string.island_group_size)) {
            SliderRow(stringResource(R.string.island_height), c.heightDp, IslandSettings.HEIGHT, "dp") { v -> update { islandHeight = v } }
            SliderRow(stringResource(R.string.island_padding), c.paddingDp, IslandSettings.PADDING, "dp") { v -> update { islandPadding = v } }
            SliderRow(stringResource(R.string.island_font), c.fontSp, IslandSettings.FONT, "sp") { v -> update { islandFont = v } }
            SegmentedRow(
                title = stringResource(R.string.island_right),
                options = IslandRight.entries,
                selected = c.right,
                onSelect = { v -> update { islandRight = v } },
                label = {
                    stringResource(if (it == IslandRight.COUNTDOWN) R.string.island_right_countdown else R.string.island_right_time)
                },
            )
        }
    }
    item(key = "position") {
        SettingsGroup(stringResource(R.string.island_group_position)) {
            SliderRow(stringResource(R.string.island_gap), c.gapAdjustDp, IslandSettings.GAP, "dp", signed = true) { v -> update { islandGap = v } }
            SliderRow(stringResource(R.string.island_x), c.offsetXDp, IslandSettings.OFFSET_X, "dp", signed = true) { v -> update { islandX = v } }
            SliderRow(stringResource(R.string.island_y), c.offsetYDp, IslandSettings.OFFSET_Y, "dp", signed = true) { v -> update { islandY = v } }
            InfoNote(stringResource(R.string.island_gap_note), Modifier.padding(HahoTheme.dimens.screenGutter))
        }
    }
}

private fun LazyListScope.cardSettings(c: IslandSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    item(key = "card") {
        SettingsGroup(stringResource(R.string.island_group_card)) {
            SliderRow(stringResource(R.string.island_width), c.cardWidthDp, IslandSettings.CARD_WIDTH, "dp") { v -> update { islandCardWidth = v } }
            SliderRow(stringResource(R.string.island_font), c.cardFontSp, IslandSettings.CARD_FONT, "sp") { v -> update { islandCardFont = v } }
            SliderRow(stringResource(R.string.island_seconds), c.cardSeconds, IslandSettings.CARD_SECONDS, " s") { v -> update { islandCardSeconds = v } }
            InfoNote(stringResource(R.string.island_card_gesture_note), Modifier.padding(HahoTheme.dimens.screenGutter))
        }
    }
    item(key = "card-position") {
        SettingsGroup(stringResource(R.string.island_group_position)) {
            val x = c.offsetX(IslandMode.CARD)
            val y = c.offsetY(IslandMode.CARD)
            SliderRow(stringResource(R.string.island_x), x, IslandSettings.OFFSET_X, "dp", signed = true) { v -> update { setIslandCardPosition(v, y) } }
            SliderRow(stringResource(R.string.island_y), y, IslandSettings.OFFSET_Y, "dp", signed = true) { v -> update { setIslandCardPosition(x, v) } }
            FollowPill(own = c.cardOffsetXDp != null || c.cardOffsetYDp != null) { update { setIslandCardPosition(null, null) } }
        }
    }
}

private fun LazyListScope.bigSettings(c: IslandSettings, update: (SettingsEditor.() -> Unit) -> Unit) {
    item(key = "big") {
        SettingsGroup(stringResource(R.string.island_group_big)) {
            SwitchRow(
                title = stringResource(R.string.island_big_title),
                summary = stringResource(R.string.island_big_summary),
                checked = c.bigCard,
                onCheckedChange = { v -> update { islandBig = v } },
                icon = R.drawable.ic_notifications,
            )
            if (c.bigCard) {
                SliderRow(stringResource(R.string.island_lead), c.bigCardLeadMinutes, IslandSettings.LEAD, " min") { v -> update { islandLead = v } }
                SliderRow(stringResource(R.string.island_width), c.bigWidthDp, IslandSettings.BIG_WIDTH, "dp") { v -> update { islandBigWidth = v } }
                SliderRow(stringResource(R.string.island_font), c.bigFontSp, IslandSettings.BIG_FONT, "sp") { v -> update { islandBigFont = v } }
                SwitchRow(
                    title = stringResource(R.string.island_until_title),
                    summary = stringResource(R.string.island_until_summary),
                    checked = c.bigUntilPrayer,
                    onCheckedChange = { v -> update { islandBigUntil = v } },
                    icon = R.drawable.ic_schedule,
                )
                if (!c.bigUntilPrayer) {
                    SliderRow(stringResource(R.string.island_seconds), c.bigSeconds, IslandSettings.BIG_SECONDS, " s") { v -> update { islandBigSeconds = v } }
                }
            }
        }
    }
    if (c.bigCard) {
        item(key = "big-position") {
            SettingsGroup(stringResource(R.string.island_group_position)) {
                val x = c.offsetX(IslandMode.BIG)
                val y = c.offsetY(IslandMode.BIG)
                SliderRow(stringResource(R.string.island_x), x, IslandSettings.OFFSET_X, "dp", signed = true) { v -> update { setIslandBigPosition(v, y) } }
                SliderRow(stringResource(R.string.island_y), y, IslandSettings.OFFSET_Y, "dp", signed = true) { v -> update { setIslandBigPosition(x, v) } }
                FollowPill(own = c.bigOffsetXDp != null || c.bigOffsetYDp != null) { update { setIslandBigPosition(null, null) } }
            }
        }
    }
}

/** The card and alert start in the pill's place; once moved, this goes back to following it. */
@Composable
private fun FollowPill(own: Boolean, onReset: () -> Unit) {
    if (own) {
        TextButton(onClick = onReset, modifier = Modifier.padding(horizontal = HahoTheme.spacing.sm)) { Text(stringResource(R.string.island_follow_pill)) }
    } else {
        InfoNote(stringResource(R.string.island_follows_pill_note), Modifier.padding(HahoTheme.dimens.screenGutter))
    }
}

/** The status bar the preview draws: tall enough for a clock and icons, like the real one. */
private const val PREVIEW_STATUS_BAR_DP = 24

/**
 * A strip like the top of the phone: a wallpaper, a status bar with a clock and battery, and the
 * island at its chosen size, place and opacity. A camera is drawn where the pill's gap is, so the
 * gap can be matched to the real one by eye. The open card and alert cover the status bar here
 * the same way they do on the phone. Sized by the mode, so there is no empty space under it.
 */
@Composable
private fun IslandPreview(c: IslandSettings, mode: IslandMode) {
    val camera = Color(0xFF3A3F3C)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B5547), Color(0xFF1E2824), Color(0xFF34423A))))
            .padding(bottom = 12.dp)
            // The preview repeats the real island; TalkBack gets the real one.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            Modifier.fillMaxWidth().height(PREVIEW_STATUS_BAR_DP.dp).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val statusStyle = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE6ECE8))
            Text(previewClock(), style = statusStyle)
            Text("91", style = statusStyle)
        }
        val overlay = @Composable {
            IslandOverlay(
                texts = previewTexts(c),
                config = c,
                gapDp = IslandGeometry.withAdjust(IslandGeometry.GAP_PADDING_DP * 2 + PREVIEW_CAMERA_DP, c.gapAdjustDp),
                mode = mode,
                onToggle = {},
                onOpen = {},
                haptics = false,
                statusBarDp = PREVIEW_STATUS_BAR_DP,
            )
        }
        if (mode == IslandMode.PILL) {
            Box(Modifier.offset(x = c.offsetXDp.dp, y = c.offsetYDp.dp)) {
                overlay()
                // The camera, centred in the pill's gap.
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ((c.heightDp - PREVIEW_CAMERA_DP) / 2).coerceAtLeast(0).dp)
                        .size(PREVIEW_CAMERA_DP.dp)
                        .clip(CircleShape)
                        .background(camera),
                )
            }
        } else {
            overlay()
        }
    }
}

private val PREVIEW_DAY = java.time.LocalDate.of(2026, 11, 20)

/** A sample time in the phone's own 12 or 24 hour format, like the real island. */
@Composable
private fun previewTime(hour: Int, minute: Int): String {
    val formatter = rememberTimeFormatter()
    return formatter.format(PREVIEW_DAY.atTime(hour, minute).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
}

@Composable
private fun previewClock(): String = previewTime(12, 4)

@Composable
private fun previewTexts(c: IslandSettings): IslandTexts {
    val name = stringResource(Prayer.MAGHRIB.labelRes())
    val clock = previewTime(18, 4)
    val left = stringResource(R.string.island_minutes, 7)
    val names = FIVE_PRAYERS.map { stringResource(it.labelRes()) }
    val minutes = listOf(245, 701, 903, 1084, 1147)
    val times = minutes.map { previewTime(it / 60, it % 60) }
    return IslandTexts(
        prayerName = name,
        clock = clock,
        left = left,
        pillRight = if (c.right == IslandRight.TIME) clock else left,
        place = stringResource(R.string.island_sample_place),
        openLabel = stringResource(R.string.island_open),
        dismissLabel = stringResource(R.string.island_dismiss),
        description = "$name $clock $left",
        cardDescription = "$name $clock $left",
        arcNames = names,
        arcTimes = times,
        arcTicks = minutes.map { (it - minutes.first()) / (minutes.last() - minutes.first()).toFloat() },
        arcNext = 3,
        arcProgress = (1077 - minutes.first()) / (minutes.last() - minutes.first()).toFloat(),
        tomorrow = null,
        alertProgress = 0.3f,
        alertStart = stringResource(R.string.island_alert_opened, previewTime(17, 54)),
    )
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    range: IntRange,
    unit: String,
    signed: Boolean = false,
    onChange: (Int) -> Unit,
) {
    val valueText = (if (signed && value > 0) "+" else "") + value + unit
    Column(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = valueText
            },
        )
    }
}
