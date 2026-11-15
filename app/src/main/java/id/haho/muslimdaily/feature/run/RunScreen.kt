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
import id.haho.design.glass.GlassIconButton
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.PhraseTrack
import id.haho.muslimdaily.feature.duas.components.RepeatToggle
import id.haho.muslimdaily.feature.duas.components.SpeedButton
import id.haho.muslimdaily.feature.duas.components.speedLabel
import id.haho.muslimdaily.feature.memorize.session.Dock
import id.haho.muslimdaily.feature.memorize.session.DockToggle
import id.haho.muslimdaily.feature.memorize.session.STACK_FONT_SCALE
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
            ),
        )
    }
}

@Composable
fun RunScreen(state: RunUiState, actions: RunActions, dockExpanded: Boolean = false) {
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

    var expanded by rememberSaveable { mutableStateOf(dockExpanded) }
    BackHandler(enabled = expanded) { expanded = false }
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
      val dockMax = maxHeight * DOCK_MAX_SHARE
      Column(Modifier.fillMaxSize()) {
        RunTopBar(state, pager, actions)
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = HahoTheme.dimens.screenGutter),
            pageSpacing = HahoTheme.spacing.md,
            key = { if (it < state.items.size) state.items[it].duaId + it else "summary" },
        ) { page ->
            if (page < state.items.size) {
                RunCard(
                    state, page, actions,
                    onPrevious = { scope.launch { pager.animateScrollToPage(page - 1) } },
                    onNext = { scope.launch { pager.animateScrollToPage(page + 1) } },
                )
            } else {
                SummaryPage(state, actions)
            }
        }
        if (pager.currentPage >= state.items.size) {
            RunBottomBar(pager)
        } else {
            RunAudioDock(state, pager.currentPage, actions, expanded, dockMax) { expanded = !expanded }
        }
      }
    }
}

private const val DOCK_MAX_SHARE = 0.6f

/**
 * The card's audio, docked like Memorize's: play and what will play stay in view; repeat, the
 * phrase track and speed open from the toggle. It sits outside the card, so no tap on it counts.
 */
@Composable
private fun RunAudioDock(state: RunUiState, page: Int, actions: RunActions, expanded: Boolean, maxHeight: Dp, onToggleExpanded: () -> Unit) {
    val dua = state.duas.getOrNull(page) ?: return
    val playback = state.playbackFor(page)
    val canListen = state.canListen(page)
    val segmented = dua.isSegmented && dua.phrases.size >= 2
    val toggle: @Composable () -> Unit = { DockToggle(expanded, onToggleExpanded) }
    val stacked = LocalDensity.current.fontScale > STACK_FONT_SCALE
    val status = when {
        dua.audio == null -> stringResource(R.string.dua_no_audio)
        !canListen -> stringResource(R.string.run_listen_hidden)
        segmented && playback.activePhrase >= 0 ->
            stringResource(R.string.dua_phrase_position, playback.activePhrase + 1, dua.phrases.size) + " · " + speedLabel(playback.speed)
        else -> speedLabel(playback.speed)
    }
    Column {
        Dock(
            expanded = expanded && canListen,
            maxHeight = maxHeight,
            collapsed = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
                    GlassIconButton(onClick = actions.onPlay, enabled = canListen, modifier = Modifier.size(AdwamDimens.playerCompactButton)) {
                        Icon(
                            painterResource(if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                            stringResource(if (playback.isPlaying) R.string.player_pause else R.string.player_play),
                            Modifier.size(HahoTheme.dimens.icon * 1.25f),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.run_listen), style = MaterialTheme.typography.titleSmall)
                        // Open, the phrase track and the speed button below say the same.
                        if (!(expanded && canListen)) {
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (stacked && canListen) Box(Modifier.align(Alignment.End)) { toggle() }
                    }
                    if (!stacked && canListen) toggle()
                }
            },
            more = if (canListen) {
                {
                    if (segmented) {
                        PhraseTrack(dua.phrases.size, playback.activePhrase, enabled = true, onSeek = actions.onPlayFrom)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        RepeatToggle(playback.repeat, enabled = true, onRepeat = actions.onRepeat)
                        if (segmented) {
                            IconButton(onClick = { actions.onSkipPhrase(-1) }, enabled = playback.activePhrase > 0) {
                                Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.player_previous_phrase))
                            }
                            IconButton(onClick = { actions.onSkipPhrase(1) }, enabled = playback.activePhrase < dua.phrases.lastIndex) {
                                Icon(painterResource(R.drawable.ic_skip_next), stringResource(R.string.player_next_phrase))
                            }
                        }
                        SpeedButton(playback.speed, enabled = true, onSpeed = actions.onSpeed)
                    }
                }
            } else {
                null
            },
        )
        // The dock's surface runs under the gesture bar.
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

@Composable
private fun RunTopBar(state: RunUiState, pager: PagerState, actions: RunActions) {
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
private fun RunCard(state: RunUiState, page: Int, actions: RunActions, onPrevious: () -> Unit, onNext: () -> Unit) {
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
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
            ) {
                if (state.isHidden(page)) {
                    FromMemoryPanel(onReveal = { actions.onReveal(page) })
                    // The meaning stays as the cue; transliteration would give the Arabic away.
                    DuaTranslations(dua, state.prefs.copy(transliteration = false))
                } else {
                    val active = state.playbackFor(page).activePhrase
                    // Follows the audio like the dua page: the phrase being heard is highlighted.
                    if (active >= 0) {
                        HighlightedArabic(dua.arabic, dua.phrases, active, AdwamTheme.arabic.large(dua.isQuran))
                    } else {
                        ArabicText(dua.arabic, style = AdwamTheme.arabic.large(dua.isQuran))
                    }
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
            CountArea(
                done = done, countText = countText, isCurrent = isCurrent, count = count,
                onPrevious = onPrevious.takeIf { page > 0 },
                onNext = onNext,
                onUndo = actions.onUndo.takeIf { count > 0 && isCurrent },
            )
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
        // "Perlu lihat teks" wraps to three lines and the pill turns into a blob.
        if (LocalDensity.current.fontScale >= LARGE_TEXT_SCALE) {
            Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                GlassOutlinedButton(onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.run_recall_knew)) }
                GlassOutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.run_recall_needed)) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
                GlassOutlinedButton(onClick = { onAnswer(true) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_recall_knew)) }
                GlassOutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.run_recall_needed)) }
            }
        }
    }
}

/** Count display with a spring "bump" on each tap; a gold disc with a dark check when complete. */
@Composable
private fun CountArea(
    done: Boolean,
    countText: String?,
    isCurrent: Boolean,
    count: Int,
    onPrevious: (() -> Unit)?,
    onNext: () -> Unit,
    onUndo: (() -> Unit)?,
) {
    val scale = remember { Animatable(1f) }
    val motion = HahoTheme.motion
    // At large text the big count and the hint take half the card and push the answer buttons
    // out of sight, so the footer shrinks to one line between the arrows (audit M3).
    val compact = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE
    LaunchedEffect(count) {
        if (count > 0 && !motion.reduced) {
            scale.snapTo(1.15f)
            scale.animateTo(1f, motion.countTap())
        }
    }
    // Previous and next sit in line with the tap hint; they take their own taps, so the card's
    // "count" tap does not fire from them.
    Row(Modifier.fillMaxWidth().padding(top = HahoTheme.spacing.lg), verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Bottom) {
        Box(Modifier.size(HahoTheme.dimens.largeTouchTarget)) {
            if (onPrevious != null) {
                IconButton(onClick = onPrevious, modifier = Modifier.fillMaxSize()) {
                    Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.run_previous))
                }
            }
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
        ) {
            if (done) {
                Surface(
                    shape = CircleShape,
                    color = AdwamTheme.colors.completeFill,
                    contentColor = AdwamTheme.colors.onCompleteFill,
                    modifier = Modifier.size(AdwamDimens.completeDisc),
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.padding(HahoTheme.spacing.md))
                }
                Text(stringResource(R.string.run_done), style = MaterialTheme.typography.titleMedium)
            } else if (countText != null) {
                Text(
                    countText,
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                        .then(if (isCurrent) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
                )
                if (!compact) {
                    Text(stringResource(R.string.run_tap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else if (!compact) {
                Spacer(Modifier.height(HahoTheme.spacing.sm))
                Text(stringResource(R.string.run_tap_read_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            onUndo?.let {
                TextButton(onClick = it) {
                    Icon(painterResource(R.drawable.ic_remove), null, Modifier.size(HahoTheme.dimens.smallIcon))
                    Text(stringResource(R.string.run_undo), Modifier.padding(start = HahoTheme.spacing.xs))
                }
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(HahoTheme.dimens.largeTouchTarget)) {
            Icon(painterResource(R.drawable.ic_skip_next), stringResource(R.string.run_next))
        }
    }
}

/** Only the summary page needs it: on a card, previous and next sit inside the card. */
@Composable
private fun RunBottomBar(pager: PagerState) {
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = HahoTheme.dimens.largeTouchTarget).padding(horizontal = HahoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            enabled = pager.currentPage > 0,
            modifier = Modifier.size(HahoTheme.dimens.largeTouchTarget),
        ) {
            Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.run_previous))
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
