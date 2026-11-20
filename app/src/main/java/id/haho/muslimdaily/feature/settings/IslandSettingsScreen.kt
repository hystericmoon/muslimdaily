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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlay = Settings.canDrawOverlays(context)
        viewModel.reschedule()
    }
    AdwamScreen(title = stringResource(R.string.reminders_island_title), onBack = onBack) {
        settings?.let {
            IslandSettingsContent(
                settings = it,
                update = viewModel::update,
                overlayAllowed = overlay,
                onAskOverlay = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                },
            )
        }
    }
}

@Composable
fun IslandSettingsContent(
    settings: AppSettings,
    update: (SettingsEditor.() -> Unit) -> Unit,
    overlayAllowed: Boolean = true,
    onAskOverlay: () -> Unit = {},
) {
    val c = settings.island
    var previewMode by rememberSaveable { mutableStateOf(IslandMode.PILL) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = HahoTheme.spacing.xxl)) {
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
        item(key = "preview") {
            Column(Modifier.padding(HahoTheme.dimens.screenGutter), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                IslandPreview(c, previewMode)
                SegmentedRow(
                    title = stringResource(R.string.island_preview),
                    options = IslandMode.entries,
                    selected = previewMode,
                    onSelect = { previewMode = it },
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
        item(key = "size") {
            SettingsGroup(stringResource(R.string.island_group_size)) {
                SliderRow(stringResource(R.string.island_height), c.heightDp, IslandSettings.HEIGHT, "dp") { v -> update { islandHeight = v } }
                SliderRow(stringResource(R.string.island_padding), c.paddingDp, IslandSettings.PADDING, "dp") { v -> update { islandPadding = v } }
                SliderRow(stringResource(R.string.island_font), c.fontSp, IslandSettings.FONT, "sp") { v -> update { islandFont = v } }
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
        item(key = "card") {
            SettingsGroup(stringResource(R.string.island_group_card)) {
                SliderRow(stringResource(R.string.island_width), c.cardWidthDp, IslandSettings.CARD_WIDTH, "dp") { v -> update { islandCardWidth = v } }
                SliderRow(stringResource(R.string.island_font), c.cardFontSp, IslandSettings.CARD_FONT, "sp") { v -> update { islandCardFont = v } }
                SliderRow(stringResource(R.string.island_seconds), c.cardSeconds, IslandSettings.CARD_SECONDS, " s") { v -> update { islandCardSeconds = v } }
                InfoNote(stringResource(R.string.island_card_gesture_note), Modifier.padding(HahoTheme.dimens.screenGutter))
            }
        }
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
                        }
                    },
                    modifier = Modifier.padding(horizontal = HahoTheme.spacing.sm),
                ) { Text(stringResource(R.string.island_reset)) }
            }
        }
    }
}

/**
 * A strip like the top of the phone: the island at the chosen size and offsets, and a camera drawn
 * where the gap is, so the gap can be matched to the real camera by eye.
 */
@Composable
private fun IslandPreview(c: IslandSettings, mode: IslandMode) {
    val camera = Color(0xFF3A3F3C)
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (mode == IslandMode.PILL) 72.dp else 300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFD8D6CC)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.offset(x = c.offsetXDp.dp, y = c.offsetYDp.dp)) {
            IslandOverlay(
                texts = previewTexts(c),
                config = c,
                gapDp = IslandGeometry.withAdjust(IslandGeometry.GAP_PADDING_DP * 2 + PREVIEW_CAMERA_DP, c.gapAdjustDp),
                mode = mode,
                onToggle = {},
                onOpen = {},
            )
            if (mode == IslandMode.PILL) {
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
        }
    }
}

@Composable
private fun previewTexts(c: IslandSettings): IslandTexts {
    val name = stringResource(Prayer.MAGHRIB.labelRes())
    val clock = "18.04"
    val left = stringResource(R.string.island_minutes, 7)
    val names = FIVE_PRAYERS.map { stringResource(it.labelRes()) }
    val minutes = listOf(245, 701, 903, 1084, 1147)
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
        arcTimes = listOf("04.05", "11.41", "15.03", "18.04", "19.07"),
        arcTicks = minutes.map { (it - minutes.first()) / (minutes.last() - minutes.first()).toFloat() },
        arcNext = 3,
        arcProgress = (1077 - minutes.first()) / (minutes.last() - minutes.first()).toFloat(),
        tomorrow = null,
        alertProgress = 0.3f,
        alertStart = stringResource(R.string.island_alert_opened, "17.54"),
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
