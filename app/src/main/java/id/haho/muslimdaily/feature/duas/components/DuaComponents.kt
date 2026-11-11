package id.haho.muslimdaily.feature.duas.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import id.haho.muslimdaily.core.ui.AppLocale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import id.haho.design.glass.pressScale
import id.haho.design.glass.GlassBottomSheet
import id.haho.design.glass.GlassIconButton
import id.haho.design.glass.DockedBar
import id.haho.design.glass.GlassCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.muslimdaily.core.designsystem.component.withBookNotes
import id.haho.design.component.Badge
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.data.settings.PhraseFill
import id.haho.muslimdaily.data.settings.PhraseLayout
import id.haho.muslimdaily.data.settings.TranslationMode
import java.util.Locale

/** Reading preferences that every dua view honours. */
data class ReadingPrefs(
    val translation: TranslationMode = TranslationMode.INDONESIAN,
    val transliteration: Boolean = false,
    val layout: PhraseLayout = PhraseLayout.LINES,
    val fill: PhraseFill = PhraseFill.NONE,
    val hideFillWhilePlaying: Boolean = true,
) {
    /** The fill to draw now: none while the audio plays, when the reader chose that. */
    fun fillFor(playing: Boolean): PhraseFill = if (playing && hideFillWhilePlaying) PhraseFill.NONE else fill
}

/** "Al-Bukhari 7/162", "Hisnul Muslim no. 100": book and number as the source prints them. */
fun sourceLabel(ref: SourceRef): String = listOf(ref.book, ref.no).filter(String::isNotBlank).joinToString(" ")

/** The chip shows the grading when the source states one, otherwise the first source. */
@Composable
fun sourceChipLabel(dua: Dua): String =
    if (dua.grading != null) gradeLabel(dua) else dua.sources.firstOrNull()?.let(::sourceLabel).orEmpty()

@Composable
fun gradeLabel(dua: Dua): String {
    val g = dua.grading ?: return ""
    val grade = g.grade.replaceFirstChar { it.titlecase(Locale.ROOT) }
    return stringResource(R.string.dua_grade_by, grade, g.grader)
}

/** Translations and transliteration below the Arabic, per the reader's settings. */
@Composable
fun DuaTranslations(dua: Dua, prefs: ReadingPrefs, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val reading = HahoTheme.reading
    // The translator's glosses in brackets read as asides: smaller, secondary colour.
    val noteColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        if (prefs.transliteration && dua.transliteration.isNotBlank()) {
            Text(
                dua.transliteration,
                style = reading.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The few entries with no Indonesian at any source (translation_id_from "none") show the English.
        val showEn = dua.translationEn.isNotBlank() && (prefs.translation != TranslationMode.INDONESIAN || dua.translationId.isBlank())
        // The few entries with no English at the source show the Indonesian rather than the Arabic alone.
        val showId = dua.translationId.isNotBlank() && (prefs.translation != TranslationMode.ENGLISH || !showEn)
        if (showId) {
            // Tagged as Indonesian so TalkBack keeps the Indonesian voice in the English UI.
            Text(
                withBookNotes(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(localeList = androidx.compose.ui.text.intl.LocaleList("id")))
                        append(dua.translationId)
                        pop()
                    },
                    noteColor,
                ),
                style = reading.bodyLarge,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showEn) {
            // Tagged as English so TalkBack switches voice inside the Indonesian UI.
            Text(
                withBookNotes(
                    androidx.compose.ui.text.buildAnnotatedString {
                        pushStyle(androidx.compose.ui.text.SpanStyle(localeList = androidx.compose.ui.text.intl.LocaleList("en")))
                        append(dua.translationEn)
                        pop()
                    },
                    noteColor,
                ),
                style = reading.bodyLarge,
                color = if (showId) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Source and grading as a text-labelled chip; opens the full reference sheet. */
@Composable
fun SourceChip(dua: Dua, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val label = sourceChipLabel(dua)
    val description = stringResource(R.string.dua_source_chip_description, label)
    AssistChip(
        onClick = { open = true },
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(
                painterResource(if (dua.grading != null) R.drawable.ic_verified else R.drawable.ic_menu_book),
                null,
                Modifier.size(HahoTheme.dimens.smallIcon),
            )
        },
        modifier = modifier.semantics { contentDescription = description },
    )
    if (open) SourceSheet(dua) { open = false }
}

@Composable
fun SourceSheet(dua: Dua, onDismiss: () -> Unit) {
    GlassBottomSheet(onDismissRequest = onDismiss) {
        SourceDetails(dua, Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).padding(bottom = HahoTheme.spacing.xxl))
    }
}

/** Sources, takhrij and grading of a dua: the body of [SourceSheet]. */
@Composable
fun SourceDetails(dua: Dua, modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
    ) {
        Text(stringResource(R.string.dua_sources_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        SheetSection(stringResource(R.string.dua_sources)) {
            dua.sources.forEach { Text(sourceLabel(it), style = MaterialTheme.typography.bodyLarge) }
        }
        if (dua.reference.isNotBlank()) {
            SheetSection(stringResource(R.string.dua_reference)) {
                Text(dua.reference, style = HahoTheme.reading.bodyMedium)
            }
        }
        if (dua.grading != null) {
            SheetSection(stringResource(R.string.dua_grading)) {
                Text(gradeLabel(dua), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (dua.altGradings.isNotEmpty()) {
            SheetSection(stringResource(R.string.dua_alt_gradings)) {
                dua.altGradings.forEach {
                    Text(stringResource(R.string.dua_grade_by, it.grade.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }, it.grader))
                }
            }
        }
        Text(
            stringResource(R.string.dua_grading_policy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Badge(
        text = pluralStringResource(R.plurals.dua_count, count, count),
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        icon = R.drawable.ic_repeat,
        modifier = modifier,
    )
}

/** A dua in a list. Tapping opens the detail. */
@Composable
fun DuaCard(
    dua: Dua,
    prefs: ReadingPrefs,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val press = remember { MutableInteractionSource() }
    GlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth().pressScale(press, pressed = 0.985f),
    ) {
        Column(
            Modifier
                .clickable(
                    interactionSource = press,
                    indication = ripple(),
                    onClickLabel = stringResource(R.string.dua_open),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(HahoTheme.dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
        ) {
            header?.invoke()
            // A preview: long texts (e.g. Ayat al-Kursi) are clamped; the detail shows them in full.
            ArabicText(dua.arabic, style = AdwamTheme.arabic.body(dua.isQuran), maxLines = CARD_ARABIC_LINES)
            DuaTranslations(dua, prefs, maxLines = CARD_TRANSLATION_LINES)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
            ) {
                dua.count?.let { CountBadge(it, Modifier.align(Alignment.CenterVertically)) }
                SourceChip(dua)
            }
        }
    }
}

/**
 * Adds the dua to the Memorize tab (My hifz) and says whether it is there. A toggle like the
 * routine bookmark: it never starts a session, the Memorize tab does.
 */
@Composable
fun HifzToggle(inHifz: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val state = stringResource(if (inHifz) R.string.memorize_state_in else R.string.memorize_state_not_in)
    IconToggleButton(
        checked = inHifz,
        onCheckedChange = { onToggle() },
        modifier = modifier.semantics { stateDescription = state },
    ) {
        Icon(
            painterResource(if (inHifz) R.drawable.ic_record_voice_over_fill else R.drawable.ic_record_voice_over),
            contentDescription = stringResource(R.string.memorize_add_label),
            tint = if (inHifz) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Repeat, previous phrase, play/pause, next phrase and speed, in one compact row. Every control is at least 48dp. */
@Composable
fun PlayerBar(
    isPlaying: Boolean,
    speed: Float,
    repeat: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSpeed: (Float) -> Unit,
    onRepeat: (Boolean) -> Unit,
    onPreviousPhrase: () -> Unit,
    onNextPhrase: () -> Unit,
    canPrevious: Boolean,
    canNext: Boolean,
    modifier: Modifier = Modifier,
    /** Phrases in the dua; the segmented track above the controls shows from two. */
    phraseCount: Int = 0,
    activePhrase: Int = -1,
    onSeekPhrase: (Int) -> Unit = {},
) {
    var speedMenu by remember { mutableStateOf(false) }
    // Docked on the bottom navigation, full width, like a media mini-player.
    DockedBar(modifier) {
      Column(Modifier.fillMaxWidth()) {
        if (phraseCount >= 2) {
            PhraseTrack(phraseCount, activePhrase, enabled, onSeekPhrase)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val repeatState = stringResource(if (repeat) R.string.state_enabled else R.string.state_disabled)
            IconToggleButton(
                checked = repeat,
                onCheckedChange = onRepeat,
                enabled = enabled,
                modifier = Modifier
                    .size(PLAYER_TARGET)
                    .semantics { stateDescription = repeatState },
            ) {
                Icon(
                    painterResource(R.drawable.ic_repeat),
                    contentDescription = stringResource(R.string.player_repeat),
                    tint = if (repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPreviousPhrase, enabled = enabled && canPrevious, modifier = Modifier.size(PLAYER_TARGET)) {
                Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.player_previous_phrase))
            }
            GlassIconButton(
                onClick = onToggle,
                enabled = enabled,
                modifier = Modifier.size(AdwamDimens.playerCompactButton),
            ) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                    modifier = Modifier.size(HahoTheme.dimens.icon * 1.25f),
                )
            }
            IconButton(onClick = onNextPhrase, enabled = enabled && canNext, modifier = Modifier.size(PLAYER_TARGET)) {
                Icon(painterResource(R.drawable.ic_skip_next), stringResource(R.string.player_next_phrase))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val speedDescription = stringResource(R.string.player_speed, speedLabel(speed))
                TextButton(
                    onClick = { speedMenu = true },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = speedDescription }.size(width = PLAYER_TARGET * 1.25f, height = PLAYER_TARGET),
                ) {
                    Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge)
                }
                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                    SPEEDS.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(speedLabel(s)) },
                            onClick = {
                                onSpeed(s)
                                speedMenu = false
                            },
                            leadingIcon = if (s == speed) { { Icon(painterResource(R.drawable.ic_check), null) } } else null,
                        )
                    }
                }
            }
        }
      }
    }
}

/**
 * One tick per phrase: heard ones tinted, the current one lit. Tap a tick to jump there, or drag
 * across to choose one; the audio restarts from that phrase when the finger lifts. One slider
 * for TalkBack ("Phrase 4 of 24"), which steps with the volume keys or the swipe actions.
 */
@Composable
private fun PhraseTrack(count: Int, active: Int, enabled: Boolean, onSeek: (Int) -> Unit) {
    var width by remember { mutableStateOf(1) }
    var dragging by remember { mutableStateOf<Int?>(null) }
    val shown = dragging ?: active
    fun indexAt(x: Float) = ((x / width) * count).toInt().coerceIn(0, count - 1)
    val position = stringResource(R.string.dua_phrase_position, shown.coerceAtLeast(0) + 1, count)
    val heard = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val lit = MaterialTheme.colorScheme.primary
    val rest = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xxs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(PLAYER_TRACK_TARGET)
                .onSizeChanged { width = it.width.coerceAtLeast(1) }
                .pointerInput(count, enabled) {
                    if (enabled) detectTapGestures { onSeek(indexAt(it.x)) }
                }
                .pointerInput(count, enabled) {
                    if (enabled) detectHorizontalDragGestures(
                        onDragStart = { dragging = indexAt(it.x) },
                        onDragEnd = { dragging?.let(onSeek); dragging = null },
                        onDragCancel = { dragging = null },
                    ) { change, _ -> dragging = indexAt(change.position.x) }
                }
                .clearAndSetSemantics {
                    contentDescription = position
                    progressBarRangeInfo = ProgressBarRangeInfo(shown.coerceAtLeast(0).toFloat(), 0f..(count - 1).toFloat(), count - 2)
                    setProgress { target -> onSeek(target.toInt().coerceIn(0, count - 1)); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(count) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (i == shown) 8.dp else 6.dp)
                            .background(
                                when {
                                    i == shown -> lit
                                    i < shown -> heard
                                    else -> rest
                                },
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
        Text(
            position,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val PLAYER_TRACK_TARGET = 32.dp
private val PLAYER_TARGET = 48.dp
private val SPEEDS = listOf(0.5f, 0.75f, 0.85f, 1f, 1.25f)

fun speedLabel(speed: Float): String = String.format(AppLocale, "%.2f×", speed).replace(Regex("[.,]00×"), "×").replace(Regex("([.,]\\d)0×"), "$1×")

private const val CARD_ARABIC_LINES = 4
private const val CARD_TRANSLATION_LINES = 3
