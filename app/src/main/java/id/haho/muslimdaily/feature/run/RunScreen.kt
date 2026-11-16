package id.haho.muslimdaily.feature.run

import id.haho.design.glass.GlassOutlinedButton
import id.haho.design.glass.GlassButton
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.design.glass.GlassCard
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.ArabicText
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import id.haho.muslimdaily.feature.duas.components.PhraseLines
import id.haho.muslimdaily.feature.duas.components.PlayerControls
import id.haho.muslimdaily.feature.duas.components.rememberFollowPhrase
import id.haho.muslimdaily.core.designsystem.component.GlassBarHost
import id.haho.muslimdaily.core.designsystem.component.GlassBarSurface
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.feature.settings.ReadingSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class RunActions(
    val onTap: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onPage: (Int) -> Unit = {},
    val onRestart: () -> Unit = {},
    val onAutoAdvance: (Boolean) -> Unit = {},
    val onFromMemory: (Boolean) -> Unit = {},
    val onReveal: (Int) -> Unit = {},
    val onRecall: (Int, Boolean) -> Unit = { _, _ -> },
    val onAddToHifz: (Int) -> Unit = {},
    val onClose: () -> Unit = {},
    val onPlay: () -> Unit = {},
    val onPlayFrom: (phrase: Int) -> Unit = {},
    val onSkipPhrase: (delta: Int) -> Unit = {},
    val onSpeed: (Float) -> Unit = {},
    val onRepeat: (Boolean) -> Unit = {},
    val onUpdateReading: (SettingsEditor.() -> Unit) -> Unit = {},
    val onDismissMarkHint: () -> Unit = {},
)

@Composable
fun RunRoute(onClose: () -> Unit, viewModel: RunViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val hapticsOn = state?.haptics ?: true

    LaunchedEffect(viewModel, hapticsOn) {
        viewModel.effects.collect { effect ->
            if (!hapticsOn) return@collect
            haptics.performHapticFeedback(
                when (effect) {
                    RunEffect.Tick -> HapticFeedbackType.SegmentTick
                    RunEffect.ItemComplete, RunEffect.SessionComplete -> HapticFeedbackType.Confirm
                },
            )
        }
    }
    // Keep the screen awake while reading.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    state?.let {
        RunScreen(
            it,
            RunActions(
                onTap = viewModel::tap,
                onUndo = viewModel::undo,
                onPage = viewModel::goTo,
                onRestart = viewModel::restart,
                onAutoAdvance = viewModel::setAutoAdvance,
                onFromMemory = viewModel::setFromMemory,
                onReveal = viewModel::reveal,
                onRecall = viewModel::recall,
                onAddToHifz = viewModel::addToHifz,
                onClose = onClose,
                onPlay = viewModel::togglePlay,
                onPlayFrom = viewModel::playFrom,
                onSkipPhrase = viewModel::skipPhrase,
                onSpeed = viewModel::setSpeed,
                onRepeat = viewModel::setRepeat,
                onUpdateReading = viewModel::updateReading,
                onDismissMarkHint = viewModel::dismissMarkHint,
            ),
        )
    }
}

@Composable
fun RunScreen(state: RunUiState, actions: RunActions) {
    val pageCount = state.items.size + 1
    val pager = rememberPagerState(initialPage = state.index) { pageCount }
    val motion = HahoTheme.motion
    val scope = rememberCoroutineScope()

    // ViewModel → pager (taps, auto-advance) with spring motion; pager → ViewModel on swipe.
    LaunchedEffect(state.index) {
        if (pager.currentPage != state.index) pager.animateScrollToPage(state.index, animationSpec = motion.cardAdvance())
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { actions.onPage(it) }
    }

    // Reading display: dua layout, progress line, translation and text size, like Dua Details.
    var readingSheet by rememberSaveable { mutableStateOf(false) }
    if (readingSheet) ReadingSheet(state.appSettings, actions.onUpdateReading) { readingSheet = false }
    val onSummary = state.index >= state.items.size
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        RunTopBar(state, pager, actions, onReading = { readingSheet = true })
        // The standard media bar, always in view; the cards end above it.
        GlassBarHost(
            modifier = Modifier.weight(1f),
            bar = {
                if (onSummary) {
                    RunSummaryBar(pager)
                } else {
                    RunBar(state, actions, onPrevious = { scope.launch { pager.animateScrollToPage(state.index - 1) } }, onNext = { scope.launch { pager.animateScrollToPage(state.index + 1) } })
                }
            },
        ) { barHeight ->
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().padding(bottom = barHeight),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = HahoTheme.dimens.screenGutter),
                pageSpacing = HahoTheme.spacing.md,
                key = { if (it < state.items.size) state.items[it].duaId + it else "summary" },
            ) { page ->
                if (page < state.items.size) RunCard(state, page, actions) else SummaryPage(state, actions)
            }
        }
    }
}

/**
 * The bar under the cards: the one-time hint, the mark-as-read row, then the same media bar as Dua Details. The row acts on the
 * card on screen; the card's own tap stays a shortcut for it.
 */
@Composable
private fun RunBar(state: RunUiState, actions: RunActions, onPrevious: () -> Unit, onNext: () -> Unit) {
    val page = state.index
    val dua = state.duas.getOrNull(page) ?: return
    val playback = state.playbackFor(page)
    val canListen = state.canListen(page)
    val segmented = dua.isSegmented && dua.phrases.size >= 2
    GlassBarSurface(insets = true) {
        Column(
            Modifier.padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        ) {
            if (state.showMarkHint) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.run_mark_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = actions.onDismissMarkHint) { Text(stringResource(R.string.dua_toggle_hint_dismiss)) }
                }
            }
            MarkRow(state, page, actions, onPrevious, onNext)
            when {
                dua.audio == null -> BarNote(stringResource(R.string.dua_no_audio))
                !canListen -> BarNote(stringResource(R.string.run_listen_hidden))
            }
            PlayerControls(
                isPlaying = playback.isPlaying,
                speed = playback.speed,
                repeat = playback.repeat,
                enabled = canListen,
                onToggle = actions.onPlay,
                onSpeed = actions.onSpeed,
                onRepeat = actions.onRepeat,
                onPreviousPhrase = { actions.onSkipPhrase(-1) },
                onNextPhrase = { actions.onSkipPhrase(1) },
                canPrevious = segmented && playback.activePhrase > 0,
                canNext = segmented && playback.activePhrase < dua.phrases.lastIndex,
                phraseCount = if (segmented) dua.phrases.size else 0,
                activePhrase = playback.activePhrase,
                onSeekPhrase = actions.onPlayFrom,
                showTrack = state.appSettings.showPhraseTrack,
            )
        }
    }
}

@Composable
private fun BarNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/**
 * Previous dua, the mark button and next dua. Read once: the button is a check you can undo (outline, then gold). Counted: it
 * shows "2/3" and counts on each tap, with an undo icon beside it once there is something to take back; gold with a check
 * at the target. Every control is a 48dp target or more; colour is never the only cue (the check and the number carry it).
 */
@Composable
private fun MarkRow(state: RunUiState, page: Int, actions: RunActions, onPrevious: () -> Unit, onNext: () -> Unit) {
    val item = state.items.getOrNull(page) ?: return
    val count = state.counts.getOrElse(page) { 0 }
    val done = state.isDone(page)
    val counted = item.target != null
    val countText = item.target?.let { stringResource(R.string.run_count, count, it) }
    val colors = AdwamTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MARK_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundControl(onClick = onPrevious, enabled = page > 0, size = MARK_SIDE, description = stringResource(R.string.run_previous_dua)) {
            Icon(painterResource(R.drawable.ic_chevron_left), null)
        }
        if (counted && count > 0) {
            RoundControl(onClick = actions.onUndo, size = MARK_SIDE, description = stringResource(R.string.run_undo)) {
                Icon(painterResource(R.drawable.ic_undo), null)
            }
        }
        val mainDescription = when {
            counted -> stringResource(R.string.run_tap_count)
            done -> stringResource(R.string.run_read_undo)
            else -> stringResource(R.string.run_tap_read)
        }
        val mainState = countText ?: stringResource(if (done) R.string.run_done else R.string.run_not_read)
        RoundControl(
            onClick = if (!counted && done) actions.onUndo else actions.onTap,
            enabled = !(counted && done),
            size = MARK_MAIN,
            description = mainDescription,
            container = if (done) colors.completeFill else Color.Transparent,
            content = if (done) colors.onCompleteFill else MaterialTheme.colorScheme.primary,
            border = if (done) null else BorderStroke(MARK_BORDER, MaterialTheme.colorScheme.primary),
            // A completed count keeps its gold: only the tap is switched off.
            dimWhenDisabled = false,
            modifier = Modifier.semantics {
                stateDescription = mainState
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            if (counted && !done) {
                Text(stringResource(R.string.run_count_short, count, item.target ?: 0), style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(painterResource(R.drawable.ic_check), null)
            }
        }
        RoundControl(onClick = onNext, size = MARK_SIDE, description = stringResource(R.string.run_next_dua)) {
            Icon(painterResource(R.drawable.ic_chevron_right), null)
        }
    }
}

/** A round outlined control: [size] wide and high, with a spoken [description]. Disabled ones are dimmed. */
@Composable
private fun RoundControl(
    onClick: () -> Unit,
    size: Dp,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Color.Transparent,
    content: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(HahoTheme.dimens.divider, MaterialTheme.colorScheme.outline),
    dimWhenDisabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (!enabled && dimWhenDisabled) 0.38f else 1f }
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon() }
    }
}

private val MARK_SIDE = 48.dp
private val MARK_MAIN = 60.dp
private val MARK_GAP = 6.dp
private val MARK_BORDER = 1.5.dp

/** On the summary page only the way back to the last dua. */
@Composable
private fun RunSummaryBar(pager: PagerState) {
    val scope = rememberCoroutineScope()
    GlassBarSurface(insets = true) {
        Row(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.spacing.sm, vertical = HahoTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
                modifier = Modifier.size(HahoTheme.dimens.largeTouchTarget),
            ) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.run_previous_dua))
            }
        }
    }
}

@Composable
private fun RunTopBar(state: RunUiState, pager: PagerState, actions: RunActions, onReading: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HahoTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = actions.onClose) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close))
            }
            Column(Modifier.weight(1f)) {
                Text(state.eventLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(
                    if (pager.currentPage < state.items.size) {
                        stringResource(R.string.run_position, pager.currentPage + 1, state.items.size)
                    } else {
                        stringResource(R.string.run_summary)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onReading) {
                Icon(painterResource(R.drawable.ic_text_fields), stringResource(R.string.dua_reading_settings))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.run_auto_advance)) },
                        trailingIcon = { Switch(checked = state.autoAdvance, onCheckedChange = null) },
                        onClick = { actions.onAutoAdvance(!state.autoAdvance) },
                    )
                    if (state.hifz.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.run_from_memory)) },
                            trailingIcon = { Switch(checked = state.fromMemory, onCheckedChange = null) },
                            onClick = { actions.onFromMemory(!state.fromMemory) },
                        )
                    }
                    state.duas.getOrNull(pager.currentPage)?.let { dua ->
                        // Stays open after adding, so the confirmation is seen (and read by TalkBack).
                        val saved = dua.id in state.saved
                        DropdownMenuItem(
                            text = { Text(stringResource(if (saved) R.string.run_in_hifz else R.string.run_add_to_hifz)) },
                            leadingIcon = { Icon(painterResource(if (saved) R.drawable.ic_check else R.drawable.ic_record_voice_over), null) },
                            enabled = !saved,
                            onClick = { actions.onAddToHifz(pager.currentPage) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.run_restart)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_restart_alt), null) },
                        onClick = {
                            menu = false
                            actions.onRestart()
                        },
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { if (state.items.isEmpty()) 0f else state.done / state.items.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter, vertical = HahoTheme.spacing.sm),
        )
    }
}

@Composable
private fun RunCard(state: RunUiState, page: Int, actions: RunActions) {
    val dua = state.duas.getOrNull(page) ?: return
    val item = state.items[page]
    val count = state.counts.getOrElse(page) { 0 }
    val done = state.isDone(page)
    val isCurrent = page == state.index
    val countText = item.target?.let { stringResource(R.string.run_count, count, it) }
    val stateText = when {
        done -> stringResource(R.string.run_done)
        countText != null -> countText
        else -> stringResource(R.string.run_not_read)
    }
    val playback = state.playbackFor(page)
    val tapLabel = stringResource(if (item.target != null) R.string.run_tap_count else R.string.run_tap_read)

    // At large text the answers are pinned below the scrolling content, like Memorize's ratings,
    // so both stay in view (audit M3).
    val pinRecall = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE && !state.isHidden(page) && state.asksRecall(page)

    GlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = HahoTheme.spacing.sm)
            .clickable(enabled = isCurrent && !done, onClickLabel = tapLabel, role = Role.Button, onClick = actions.onTap)
            .semantics { stateDescription = stateText },
    ) {
        Column(Modifier.fillMaxSize().padding(HahoTheme.spacing.xl)) {
            val scroll = rememberScrollState()
            // The text scrolls to the phrase that plays, on next, previous and as the audio moves on.
            val follow = rememberFollowPhrase(scroll, if (isCurrent) playback.activePhrase else -1)
            Column(
                Modifier.weight(1f).then(follow.viewportModifier).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
            ) {
                if (state.isHidden(page)) {
                    FromMemoryPanel(onReveal = { actions.onReveal(page) })
                    // The meaning stays as the cue; transliteration would give the Arabic away.
                    DuaTranslations(dua, state.prefs.copy(transliteration = false))
                } else {
                    // The reader's dua layout, as on Dua Details; the phrase being heard is highlighted. A tap on the text stays the
                    // card's count shortcut, so phrases do not start the audio from here (the bar's track does).
                    PhraseLines(
                        text = dua.arabic,
                        phrases = dua.phrases,
                        activePhrase = playback.activePhrase,
                        style = AdwamTheme.arabic.large(dua.isQuran),
                        layout = state.prefs.layout,
                        fill = state.prefs.fillFor(playback.isPlaying),
                        focusNav = dua.audio == null,
                        follow = follow,
                    )
                    // Below the Arabic: the reader checks the recitation against the text, then answers.
                    if (state.asksRecall(page) && !pinRecall) {
                        RecallCheck(onAnswer = { knew -> actions.onRecall(page, knew) })
                    }
                    state.recalled[page]?.let { due ->
                        Text(
                            stringResource(R.string.memorize_scheduled, formatLongDate(due)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    DuaTranslations(dua, state.prefs)
                }
            }
            if (pinRecall) {
                Spacer(Modifier.height(HahoTheme.spacing.sm))
                RecallCheck(onAnswer = { knew -> actions.onRecall(page, knew) })
            }
        }
    }
}

/** Stands in for the Arabic of a dua in "My hifz": recite it without looking, then check. */
@Composable
private fun FromMemoryPanel(onReveal: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(HahoTheme.dimens.divider, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(HahoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            Text(stringResource(R.string.run_from_memory), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.run_from_memory_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassOutlinedButton(onClick = onReveal) { Text(stringResource(R.string.run_show_text)) }
        }
    }
}

private const val LARGE_TEXT_SCALE = 1.5f

/** After the text of a dua recited from memory is shown: did the recitation hold without it? */
@Composable
private fun RecallCheck(onAnswer: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        Text(stringResource(R.string.run_recall_prompt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        // Side by side at normal text sizes; stacked at large ones, where a half-width
        // a half-width answer wraps to three lines and the pill turns into a blob.
        if (LocalDensity.current.fontScale >= LARGE_TEXT_SCALE) {
            Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                GlassOutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.memorize_not_yet)) }
                GlassOutlinedButton(onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.memorize_knew_it)) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                GlassOutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.memorize_not_yet)) }
                GlassOutlinedButton(onClick = { onAnswer(true) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.memorize_knew_it)) }
            }
        }
    }
}

@Composable
private fun SummaryPage(state: RunUiState, actions: RunActions) {
    val all = state.done == state.items.size
    Column(
        Modifier.fillMaxSize().padding(HahoTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg, Alignment.CenterVertically),
    ) {
        Surface(
            shape = CircleShape,
            color = if (all) AdwamTheme.colors.completeFill else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (all) AdwamTheme.colors.onCompleteFill else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AdwamDimens.completeDisc * 1.5f),
        ) {
            Icon(painterResource(if (all) R.drawable.ic_done_all else R.drawable.ic_format_list_numbered), null, Modifier.padding(HahoTheme.spacing.lg))
        }
        Text(
            if (all) stringResource(R.string.run_summary_done, state.eventLabel) else stringResource(R.string.run_summary_partial),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            pluralStringResource(R.plurals.run_summary_count, state.items.size, state.done, state.items.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlassButton(onClick = actions.onClose) { Text(stringResource(R.string.action_done)) }
        if (!all) {
            val firstOpen = state.items.indices.firstOrNull { !state.isDone(it) }
            if (firstOpen != null) {
                GlassOutlinedButton(onClick = { actions.onPage(firstOpen) }) { Text(stringResource(R.string.run_go_to_open)) }
            }
        }
        Text(
            stringResource(R.string.run_private_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
