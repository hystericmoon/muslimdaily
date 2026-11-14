package id.haho.muslimdaily.feature.memorize.session

import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.haho.design.glass.Glass
import id.haho.muslimdaily.feature.memorize.MemorizeSpeedControl
import id.haho.muslimdaily.navigation.NavLabel
import androidx.compose.ui.platform.LocalDensity
import id.haho.design.glass.GlassIconButton
import id.haho.design.glass.GlassOutlinedButton
import id.haho.design.glass.GlassTonalButton
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassCard
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.ui.formatLongDate
import id.haho.muslimdaily.data.settings.PauseMode
import id.haho.muslimdaily.data.settings.PhraseLayout
import id.haho.muslimdaily.domain.memorize.FadeLadder
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.PhraseLines
import id.haho.muslimdaily.feature.duas.components.speedLabel
import id.haho.muslimdaily.feature.memorize.MemorizeSettingsControls
import androidx.compose.ui.semantics.contentDescription
import id.haho.muslimdaily.core.designsystem.component.ArabicText

data class MemorizeActions(
    val onStep: (Step) -> Unit = {},
    val onListen: () -> Unit = {},
    val onBuild: () -> Unit = {},
    val onAb: () -> Unit = {},
    val onSetAb: (IntRange) -> Unit = {},
    val onRecallGaps: () -> Unit = {},
    val onToggle: () -> Unit = {},
    val onFade: (FadeLevel) -> Unit = {},
    val onFadeOrder: (FadeOrder) -> Unit = {},
    val onFirstLetters: (Boolean) -> Unit = {},
    val onRevealNext: () -> Unit = {},
    val onFinishRecital: () -> Unit = {},
    val onRate: (Rating) -> Unit = {},
    val onToggleMissed: (Int) -> Unit = {},
    val onPractiseMissed: () -> Unit = {},
    val onLoops: (Int) -> Unit = {},
    val onPauseMode: (PauseMode) -> Unit = {},
    val onPauseSeconds: (Int) -> Unit = {},
    val onSpeed: (Float) -> Unit = {},
    val onListenOnly: () -> Unit = {},
    val onNext: (String) -> Unit = {},
    val onClose: () -> Unit = {},
)

@Composable
fun MemorizeSessionRoute(
    onClose: () -> Unit,
    onListenOnly: (String) -> Unit,
    onNext: (String) -> Unit,
    viewModel: MemorizeSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let {
        MemorizeSessionScreen(
            it,
            MemorizeActions(
                onStep = viewModel::setStep,
                onListen = viewModel::playListen,
                onBuild = viewModel::playBuild,
                onAb = viewModel::playAb,
                onSetAb = viewModel::setAb,
                onRecallGaps = viewModel::playRecallWithGaps,
                onToggle = viewModel::toggle,
                onFade = viewModel::setFade,
                onFadeOrder = viewModel::setFadeOrder,
                onFirstLetters = viewModel::setFirstLetters,
                onRevealNext = viewModel::revealNext,
                onFinishRecital = viewModel::finishRecital,
                onRate = viewModel::rate,
                onToggleMissed = viewModel::toggleMissed,
                onPractiseMissed = viewModel::practiseMissed,
                onLoops = { n -> viewModel.updateSettings(loops = n) },
                onPauseMode = { m -> viewModel.updateSettings(pauseMode = m) },
                onPauseSeconds = { s -> viewModel.updateSettings(pauseSeconds = s) },
                onSpeed = { s -> viewModel.updateSettings(speed = s) },
                onListenOnly = { onListenOnly(viewModel.duaId) },
                onNext = onNext,
                onClose = onClose,
            ),
        )
    }
}

@Composable
private fun talkBackOn(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) { context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true }
}

/**
 * The session screen: the dua's text owns the screen, the step's main action and status sit in a
 * docked sheet above the bottom navigation, and the step's secondary controls open from that sheet.
 * Check has no sheet: its rating buttons are the content.
 */
@Composable
fun MemorizeSessionScreen(
    state: MemorizeUiState,
    actions: MemorizeActions,
    talkBack: Boolean = talkBackOn(),
    startExpanded: Boolean = false,
) {
    // Each step opens with its sheet collapsed; Back closes an open sheet before it leaves the screen.
    var expanded by rememberSaveable(state.step) { mutableStateOf(startExpanded) }
    BackHandler(enabled = expanded) { expanded = false }
    // A–B: true between the first and the second phrase tap. Held here so closing the sheet keeps it.
    var abAwaiting by rememberSaveable(state.dua.id) { mutableStateOf(false) }
    // Recall: the meaning opens from the top bar's title line, the cue to recite from.
    var meaning by rememberSaveable(state.dua.id) { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val dockMax = maxHeight * DOCK_MAX_SHARE
        Column(Modifier.fillMaxSize()) {
            SessionTopBar(state, actions, meaning) { meaning = !meaning }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(HahoTheme.dimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
            ) {
                val hint = if (state.fromReview && state.step == Step.RECALL) R.string.memorize_hint_review else state.step.hintRes()
                // The step hint gives way to the text while the sheet is open.
                if (!expanded) InfoNote(stringResource(hint))
                when (state.step) {
                    Step.LISTEN -> ListenStep(state)
                    Step.BUILD -> BuildStep(state)
                    Step.RECALL -> RecallStep(state, talkBack, meaning)
                    Step.CHECK -> CheckStep(state, actions)
                }
            }
            // Big text at 200% font: an open sheet takes at most 60% of the screen and scrolls inside.
            val toggle: @Composable () -> Unit = { DockToggle(expanded) { expanded = !expanded } }
            when (state.step) {
                Step.LISTEN -> Dock(expanded, dockMax, listenDock(state, actions, toggle), listenMore(state, actions))
                Step.BUILD -> Dock(expanded, dockMax, buildDock(state, actions, toggle), buildMore(state, actions, abAwaiting) { abAwaiting = it })
                Step.RECALL -> Dock(expanded, dockMax, recallDock(state, actions, toggle), recallMore(state, actions, talkBack))
                Step.CHECK -> Unit
            }
            SessionNavBar(state.step, actions.onStep)
        }
    }
}

private const val DOCK_MAX_SHARE = 0.6f

@Composable
private fun SessionTopBar(state: MemorizeUiState, actions: MemorizeActions, meaning: Boolean, onMeaning: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close)) }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(stringResource(R.string.memorize_session_title), style = MaterialTheme.typography.titleMedium)
            val positioned = state.positionOf > 1 && state.position > 0
            val subtitle = if (positioned) {
                stringResource(R.string.memorize_event_position, state.eventLabel, state.position, state.positionOf)
            } else {
                state.eventLabel
            }
            // "·" is dropped or spelled out by TTS engines; TalkBack hears "Dzikir pagi, doa 3 dari 19".
            val spoken = if (positioned) {
                stringResource(R.string.memorize_event_position_spoken, state.eventLabel, state.position, state.positionOf)
            } else {
                subtitle
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = spoken },
                )
                if (state.step == Step.RECALL) {
                    // The cue is the title line; its meaning is one tap away. 48 dp target, and a button for TalkBack.
                    val shown = stringResource(if (meaning) R.string.memorize_dock_state_expanded else R.string.memorize_dock_state_collapsed)
                    Text(
                        stringResource(R.string.memorize_cue_meaning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onMeaning)
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = HahoTheme.spacing.sm)
                            .semantics { stateDescription = shown },
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.memorize_session_menu))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.memorize_listen_only)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_headphones), null) },
                    enabled = state.hasAudio,
                    onClick = { menu = false; actions.onListenOnly() },
                )
            }
        }
    }
}

@Composable
private fun SessionNavBar(selected: Step, onStep: (Step) -> Unit) {
    val g = Glass.colors
    NavigationBar(containerColor = g.chrome, tonalElevation = 0.dp) {
        Step.entries.forEach { step ->
            NavigationBarItem(
                selected = step == selected,
                onClick = { onStep(step) },
                icon = { Icon(painterResource(step.iconRes()), null) },
                label = { NavLabel(stringResource(step.labelRes())) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = g.tinted),
            )
        }
    }
}

/**
 * The docked sheet. [collapsed] holds the step's main action and its status, every action the step
 * needs; [more] holds the optional tuning and opens from the toggle inside [collapsed].
 */
@Composable
private fun Dock(
    expanded: Boolean,
    maxHeight: Dp,
    collapsed: @Composable ColumnScope.() -> Unit,
    more: (@Composable ColumnScope.() -> Unit)?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .animateContentSize()
                .padding(horizontal = HahoTheme.dimens.screenGutter)
                .padding(top = HahoTheme.spacing.sm, bottom = HahoTheme.spacing.md),
        ) {
            // Decorative: the toggle button is the control, and a drag would break WCAG 2.5.7.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.height(HahoTheme.spacing.sm))
            collapsed()
            if (expanded && more != null) {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(top = HahoTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
                    content = more,
                )
            }
        }
    }
}

@Composable
private fun DockToggle(expanded: Boolean, onClick: () -> Unit) {
    val state = stringResource(if (expanded) R.string.memorize_dock_state_expanded else R.string.memorize_dock_state_collapsed)
    TextButton(onClick = onClick, modifier = Modifier.semantics { stateDescription = state }) {
        Text(stringResource(if (expanded) R.string.memorize_dock_less else R.string.action_more))
        Icon(
            painterResource(if (expanded) R.drawable.ic_keyboard_arrow_down else R.drawable.ic_keyboard_arrow_up),
            null,
            Modifier.padding(start = HahoTheme.spacing.xs).size(HahoTheme.dimens.smallIcon),
        )
    }
}

private fun Step.labelRes() = when (this) {
    Step.LISTEN -> R.string.memorize_step_listen
    Step.BUILD -> R.string.memorize_step_build
    Step.RECALL -> R.string.memorize_step_recall
    Step.CHECK -> R.string.memorize_step_check
}

private fun Step.iconRes() = when (this) {
    Step.LISTEN -> R.drawable.ic_music_note
    Step.BUILD -> R.drawable.ic_format_list_numbered
    Step.RECALL -> R.drawable.ic_psychology
    Step.CHECK -> R.drawable.ic_check
}

private fun Step.hintRes() = when (this) {
    Step.LISTEN -> R.string.memorize_hint_listen
    Step.BUILD -> R.string.memorize_hint_build
    Step.RECALL -> R.string.memorize_hint_recall
    Step.CHECK -> R.string.memorize_hint_check
}

/**
 * The dua's Arabic. With [focus], one phrase at a time that follows the audio, with back and next
 * to step through it: learning a chunk at a time keeps the load on working memory low. Check
 * shows the whole text, to compare the recital with.
 */
@Composable
private fun ArabicCard(state: MemorizeUiState, active: Int = state.playback.activePhrase, focus: Boolean = false) {
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(HahoTheme.spacing.xl)) {
            val style = AdwamTheme.arabic.large(state.dua.isQuran)
            if (focus) {
                PhraseLines(state.dua.arabic, state.dua.phrases, active, style, layout = PhraseLayout.FOCUS)
            } else {
                HighlightedArabic(state.dua.arabic, state.dua.phrases, active, style)
            }
        }
    }
}

/** The step's transport: pause and play once a sequence is loaded, its start button before that. */
@Composable
private fun PlayDock(
    loaded: Boolean,
    isPlaying: Boolean,
    enabled: Boolean,
    startLabel: String,
    onStart: () -> Unit,
    onToggle: () -> Unit,
    toggle: @Composable () -> Unit,
    status: (@Composable () -> Unit)? = null,
) {
    // A start button beside the toggle gets squeezed into a blob at large text: stack them from 1.3x.
    val stacked = !loaded && LocalDensity.current.fontScale > STACK_FONT_SCALE
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
        if (loaded) {
            GlassIconButton(onClick = onToggle, modifier = Modifier.size(AdwamDimens.playerMainButton)) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                    Modifier.size(HahoTheme.dimens.icon * 1.5f),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            status?.invoke()
            if (!loaded) {
                GlassButton(onClick = onStart, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_play_arrow), null)
                    Spacer(Modifier.width(HahoTheme.spacing.sm))
                    Text(startLabel)
                }
            }
            if (stacked) Box(Modifier.align(Alignment.End)) { toggle() }
        }
        if (!stacked) toggle()
    }
}

@Composable
private fun RestartButton(onStart: () -> Unit) {
    TextButton(onClick = onStart) {
        Icon(painterResource(R.drawable.ic_replay), null, Modifier.size(HahoTheme.dimens.smallIcon))
        Text(stringResource(R.string.memorize_restart), Modifier.padding(start = HahoTheme.spacing.xs))
    }
}

@Composable
private fun ListenStep(state: MemorizeUiState) {
    ArabicCard(state, focus = true)
    if (!state.hasAudio) InfoNote(stringResource(R.string.dua_no_audio), icon = R.drawable.ic_volume_off)
    if (state.playback.failed) {
        InfoNote(
            stringResource(R.string.audio_failed),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            icon = R.drawable.ic_volume_off,
        )
    }
}

private fun listenLoaded(state: MemorizeUiState) = state.playing == Playing.LISTEN && !state.playback.ended

private fun listenDock(state: MemorizeUiState, actions: MemorizeActions, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    PlayDock(
        loaded = listenLoaded(state),
        isPlaying = state.playback.isPlaying,
        enabled = state.hasAudio,
        startLabel = stringResource(R.string.memorize_listen_twice),
        onStart = actions.onListen,
        onToggle = actions.onToggle,
        toggle = toggle,
        status = { Text(speedLabel(state.settings.speed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

private fun listenMore(state: MemorizeUiState, actions: MemorizeActions): @Composable ColumnScope.() -> Unit = {
    if (listenLoaded(state)) RestartButton(actions.onListen)
    MemorizeSpeedControl(state.settings, actions.onSpeed)
}

@Composable
private fun BuildStep(state: MemorizeUiState) {
    ArabicCard(state, focus = true)
    if (!state.segmented) InfoNote(stringResource(R.string.memorize_not_segmented))
    if (state.hasAudio && state.dua.hasUnheardPhrases) InfoNote(stringResource(R.string.dua_unheard_note), icon = R.drawable.ic_volume_off)
}

private fun buildDock(state: MemorizeUiState, actions: MemorizeActions, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    val progress = (state.playing == Playing.BUILD || state.playing == Playing.AB) && !state.playback.ended
    PlayDock(
        loaded = state.playing == Playing.BUILD && !state.playback.ended,
        isPlaying = state.playback.isPlaying,
        enabled = state.hasAudio,
        startLabel = stringResource(R.string.memorize_build_start),
        onStart = actions.onBuild,
        onToggle = actions.onToggle,
        toggle = toggle,
        status = if (progress) ({ BuildProgress(state) }) else null,
    )
}

private fun buildMore(
    state: MemorizeUiState,
    actions: MemorizeActions,
    abAwaiting: Boolean,
    onAbAwaiting: (Boolean) -> Unit,
): @Composable ColumnScope.() -> Unit = {
    if (state.playing == Playing.BUILD && !state.playback.ended) RestartButton(actions.onBuild)
    if (state.segmented && state.dua.phrases.size > 1) AbLoop(state, actions, abAwaiting, onAbAwaiting)
    MemorizeSettingsControls(
        settings = state.settings,
        onLoops = actions.onLoops,
        onPauseMode = actions.onPauseMode,
        onPauseSeconds = actions.onPauseSeconds,
        onSpeed = actions.onSpeed,
    )
}

/** Whose turn it is and where in the build: a live region, so TalkBack announces each change. */
@Composable
private fun BuildProgress(state: MemorizeUiState) {
    val loops = state.settings.loopsPerChunk.coerceIn(1, 10)
    val i = state.playback.sequenceIndex.coerceAtLeast(0)
    val chunk = i / (2 * loops)
    val loop = (i % (2 * loops)) / 2 + 1
    val chunks = if (state.segmented && state.playing == Playing.BUILD) state.dua.phrases.count { it.segment != null } else 1
    Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(
            if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.memorize_build_progress, (chunk + 1).coerceAtMost(chunks), chunks, loop, loops),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Repeat a part: the dua's phrases are the targets. The first tap sets one end, the second the other
 * (either order), a third starts over. Taps only, no dragging (WCAG 2.5.7).
 */
@Composable
private fun AbLoop(state: MemorizeUiState, actions: MemorizeActions, awaitingSecond: Boolean, onAwaiting: (Boolean) -> Unit) {
    val phrases = state.dua.phrases
    val selection = AbSelection(state.ab, awaitingSecond)
    val whole = AbSelection.whole(phrases.size).range
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.memorize_ab_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = { actions.onSetAb(whole); onAwaiting(false) }, enabled = state.ab != whole) {
                Text(stringResource(R.string.memorize_ab_whole))
            }
        }
        Text(
            stringResource(if (awaitingSecond) R.string.memorize_ab_body_last else R.string.memorize_ab_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        phrases.forEachIndexed { i, phrase ->
            PhraseRow(
                number = i + 1,
                text = phrase.text,
                isQuran = state.dua.isQuran,
                inRange = i in state.ab,
                isFrom = i == state.ab.first,
                isTo = i == state.ab.last && !awaitingSecond,
                onClick = {
                    val next = selection.tap(i)
                    actions.onSetAb(next.range)
                    onAwaiting(next.awaitingSecond)
                },
            )
        }
        GlassButton(onClick = actions.onAb, enabled = state.hasAudio, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(
                stringResource(R.string.memorize_ab_play, state.ab.first + 1, state.ab.last + 1),
                Modifier.padding(start = HahoTheme.spacing.sm),
            )
        }
    }
}

/** One phrase as a full-width target (at least 48 dp): number, Arabic, and a From/To tag on the ends of the range. */
@Composable
private fun PhraseRow(number: Int, text: String, isQuran: Boolean, inRange: Boolean, isFrom: Boolean, isTo: Boolean, onClick: () -> Unit) {
    val container = if (inRange) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content = if (inRange) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val position = stringResource(
        when {
            isFrom && isTo -> R.string.memorize_ab_state_both
            isFrom -> R.string.memorize_ab_state_from
            isTo -> R.string.memorize_ab_state_to
            inRange -> R.string.memorize_ab_state_in
            else -> R.string.memorize_ab_state_out
        },
    )
    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        border = if (inRange) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = inRange
                stateDescription = position
            },
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = HahoTheme.spacing.md, vertical = HahoTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md),
        ) {
            // "2" alone is not a useful announcement; the Arabic follows in its own language.
            val spoken = stringResource(R.string.memorize_phrase_n, number)
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            )
            ArabicText(
                text,
                style = AdwamTheme.arabic.body(isQuran),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            if (isFrom || isTo) {
                // Not colour alone (WCAG 1.4.1): the ends of the range carry words too.
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.clearAndSetSemantics {}) {
                    if (isFrom) RangeTag(stringResource(R.string.memorize_ab_from))
                    if (isTo) RangeTag(stringResource(R.string.memorize_ab_to))
                }
            }
        }
    }
}

@Composable
private fun RangeTag(label: String) {
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = HahoTheme.spacing.xs, vertical = 2.dp))
    }
}

@Composable
private fun RecallStep(state: MemorizeUiState, talkBack: Boolean, meaning: Boolean) {
    if (meaning) {
        GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(HahoTheme.spacing.lg)) { DuaTranslations(state.dua, state.reading) }
        }
    }
    if (state.practising) InfoNote(stringResource(R.string.memorize_practising), icon = R.drawable.ic_replay)
    // Play with gaps needs phrase segments and audio; without them the hints point to the reveal button.
    val gaps = state.segmented && state.hasAudio
    // The text comes first: it is what the learner recites from. The fade controls tune it in the sheet.
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        RecallText(
            words = state.words,
            hidden = state.hiddenWords,
            style = AdwamTheme.arabic.large(state.dua.isQuran),
            hiddenDescription = pluralStringResource(
                if (gaps) R.plurals.memorize_hidden_words else R.plurals.memorize_hidden_words_reveal,
                state.hiddenWords.size,
                state.hiddenWords.size,
            ),
            firstLetters = state.firstLetters,
            modifier = Modifier.padding(HahoTheme.spacing.xl),
        )
    }
    // The word shown last by "reveal as you go" is announced as Arabic text (a stateDescription can't
    // carry the Arabic locale, so the UI voice would read it).
    val last = state.lastRevealed?.text.orEmpty()
    if (last.isNotEmpty()) {
        ArabicText(
            last,
            style = AdwamTheme.arabic.body(state.dua.isQuran),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    RecitalOutcome(state)
}

/** Practice hides the missed words whatever the fade level, so it always needs the reveal. */
private fun hasHints(state: MemorizeUiState) = state.fade != FadeLevel.NONE || state.practising

private fun recallDock(state: MemorizeUiState, actions: MemorizeActions, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            if (state.playing == Playing.RECALL && state.playback.isPlaying) {
                Text(
                    if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.stage >= 1 && !state.practising) {
                Text(
                    stringResource(R.string.memorize_stage, state.stage, FadeLadder.LAST),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // The level in words, not only as gaps in the text.
            Text(
                when {
                    state.practising -> stringResource(R.string.memorize_hints_missed)
                    state.fade == FadeLevel.NONE -> stringResource(R.string.memorize_hints_off)
                    else -> stringResource(R.string.memorize_hints_hidden, (state.fade.fraction * 100).toInt())
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        toggle()
    }
    // Side by side two half-width buttons break words at large text sizes: stack them from 1.3x.
    val stacked = LocalDensity.current.fontScale > STACK_FONT_SCALE
    val buttons: @Composable (Modifier) -> Unit = { m ->
        if (hasHints(state)) {
            GlassOutlinedButton(onClick = actions.onRevealNext, enabled = state.hiddenWords.isNotEmpty(), modifier = m) {
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Text(stringResource(R.string.memorize_reveal_next), Modifier.padding(start = HahoTheme.spacing.sm))
            }
        }
        GlassButton(onClick = actions.onFinishRecital, modifier = m) { Text(stringResource(R.string.memorize_recited)) }
    }
    if (stacked) {
        Column(Modifier.padding(top = HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            buttons(Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.padding(top = HahoTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            buttons(Modifier.weight(1f))
        }
    }
}

private const val STACK_FONT_SCALE = 1.3f

private fun recallMore(state: MemorizeUiState, actions: MemorizeActions, talkBack: Boolean): @Composable ColumnScope.() -> Unit = {
    val gaps = state.segmented && state.hasAudio
    Text(stringResource(R.string.memorize_fade_level), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        FadeLevel.entries.forEach { level ->
            // While practising, the missed words are hidden, not a share of them: no level is current.
            FilterChip(
                selected = !state.practising && state.fade == level,
                onClick = { actions.onFade(level) },
                label = { Text(stringResource(R.string.memorize_fade_percent, (level.fraction * 100).toInt())) },
            )
        }
    }
    if (!state.practising && (state.fade == FadeLevel.LIGHT || state.fade == FadeLevel.MEDIUM)) {
        Text(stringResource(R.string.memorize_fade_order), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            FadeOrder.entries.forEach { order ->
                FilterChip(
                    selected = state.fadeOrder == order,
                    onClick = { actions.onFadeOrder(order) },
                    label = { Text(stringResource(order.labelRes())) },
                )
            }
        }
    }
    if (hasHints(state)) {
        FilterChip(
            selected = state.firstLetters,
            onClick = { actions.onFirstLetters(!state.firstLetters) },
            label = { Text(stringResource(R.string.memorize_first_letters)) },
        )
    }
    if (talkBack && gaps) InfoNote(stringResource(R.string.memorize_talkback_recall), icon = R.drawable.ic_headphones)
    // Gaps need phrase segments, like the A–B loop: hidden rather than shown disabled until the audio is marked.
    if (state.segmented) {
        GlassTonalButton(onClick = actions.onRecallGaps, enabled = state.hasAudio && state.hiddenWords.isNotEmpty()) {
            Icon(painterResource(R.drawable.ic_graphic_eq), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(stringResource(R.string.memorize_play_with_gaps), Modifier.padding(start = HahoTheme.spacing.sm))
        }
    }
}

/** How the last recital moved the hints (vanishing cues), and how many missed words are left in practice. */
@Composable
private fun RecitalOutcome(state: MemorizeUiState) {
    state.attempt?.let { outcome ->
        Text(
            stringResource(
                when (outcome) {
                    FadeLadder.Outcome.UP -> R.string.memorize_attempt_up
                    FadeLadder.Outcome.SAME -> R.string.memorize_attempt_same
                    FadeLadder.Outcome.DOWN -> R.string.memorize_attempt_down
                    FadeLadder.Outcome.DONE -> R.string.memorize_attempt_done
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    state.practiceLeft?.let { left ->
        Text(
            if (left == 0) stringResource(R.string.memorize_practice_done) else pluralStringResource(R.plurals.memorize_practice_left, left, left),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

private fun FadeOrder.labelRes() = when (this) {
    FadeOrder.RANDOM -> R.string.memorize_fade_random
    FadeOrder.FROM_END -> R.string.memorize_fade_from_end
}

@Composable
private fun CheckStep(state: MemorizeUiState, actions: MemorizeActions) {
    val rated = state.rated
    val markable = Recall.recallable(state.words).size
    if (rated == null && markable > 0) {
        MarkMissed(state, actions)
    } else {
        ArabicCard(state, active = -1)
    }
    if (rated == null) {
        Text(stringResource(R.string.memorize_rate_prompt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val current = state.review?.state
        // Only words marked count as evidence; with none marked, nothing is suggested.
        val suggested = if (state.missedWords.isEmpty()) null else Leitner.suggestedRating(state.missedWords.size, markable)
        Rating.entries.forEach { rating ->
            val next = Leitner.rate(current, rating, java.time.LocalDate.now())
            val days = Leitner.INTERVALS[next.box - 1]
            val content: @Composable () -> Unit = {
                Column(Modifier.padding(vertical = HahoTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(rating.labelRes()), style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (rating == suggested) {
                            stringResource(R.string.memorize_suggested_in_days, pluralStringResource(R.plurals.memorize_next_in_days, days, days))
                        } else {
                            pluralStringResource(R.plurals.memorize_next_in_days, days, days)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (rating == suggested) {
                GlassButton(onClick = { actions.onRate(rating) }, modifier = Modifier.fillMaxWidth()) { content() }
            } else {
                GlassOutlinedButton(onClick = { actions.onRate(rating) }, modifier = Modifier.fillMaxWidth()) { content() }
            }
        }
    } else {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(HahoTheme.spacing.lg).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.memorize_scheduled, formatLongDate(rated.due)), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (Leitner.isMemorized(rated)) stringResource(R.string.memorize_memorized) else stringResource(R.string.memorize_box_n, rated.box),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        val next = state.nextDueId
        if (next != null) {
            GlassButton(onClick = { actions.onNext(next) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.memorize_next_review)) }
        }
        GlassOutlinedButton(onClick = actions.onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done)) }
    }
}

/** Mark the words missed in the recital; they can then be practised on their own in Recall. */
@Composable
private fun MarkMissed(state: MemorizeUiState, actions: MemorizeActions) {
    Text(stringResource(R.string.memorize_mark_missed), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Text(stringResource(R.string.memorize_mark_missed_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        MissedWords(
            words = state.words,
            missed = state.missedWords,
            style = AdwamTheme.arabic.large(state.dua.isQuran),
            onToggle = actions.onToggleMissed,
            modifier = Modifier.padding(HahoTheme.spacing.xl),
        )
    }
    val count = state.missedWords.size
    if (count > 0) {
        GlassTonalButton(onClick = actions.onPractiseMissed, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_replay), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(pluralStringResource(R.plurals.memorize_practise_missed, count, count), Modifier.padding(start = HahoTheme.spacing.sm))
        }
    }
}

private fun Rating.labelRes() = when (this) {
    Rating.AGAIN -> R.string.memorize_rate_again
    Rating.HARD -> R.string.memorize_rate_hard
    Rating.EASY -> R.string.memorize_rate_easy
}
