package id.haho.muslimdaily.feature.memorize.session

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
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
import id.haho.muslimdaily.navigation.NavLabel
import androidx.compose.ui.platform.LocalDensity
import id.haho.design.glass.GlassIconButton
import id.haho.muslimdaily.core.designsystem.component.GlassBarHost
import id.haho.muslimdaily.feature.duas.components.FollowPhrase
import id.haho.muslimdaily.feature.duas.components.rememberFollowPhrase
import id.haho.muslimdaily.core.designsystem.component.GlassBarSurface
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
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
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.feature.settings.ReadingSheet
import id.haho.muslimdaily.domain.memorize.FadeLadder
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.PhraseLines
import id.haho.muslimdaily.feature.duas.components.SPEEDS
import id.haho.muslimdaily.feature.duas.components.PlayerControls
import id.haho.muslimdaily.feature.duas.components.RepeatToggle
import id.haho.muslimdaily.feature.duas.components.speedLabel
import kotlin.math.roundToInt
import id.haho.muslimdaily.feature.memorize.MemorizeSettingsControls
import id.haho.muslimdaily.feature.memorize.MemorizeSpeedControl
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import id.haho.muslimdaily.core.designsystem.component.ArabicText

data class MemorizeActions(
    val onStep: (Step) -> Unit = {},
    val onGuidedStep: (GuidedStep) -> Unit = {},
    val onRevealTest: () -> Unit = {},
    val onAdvanced: (Boolean) -> Unit = {},
    val onPractiseNow: () -> Unit = {},
    val onListen: () -> Unit = {},
    val onListenFrom: (Int) -> Unit = {},
    val onListenSkip: (Int) -> Unit = {},
    val onListenRepeat: () -> Unit = {},
    val onUpdateReading: (SettingsEditor.() -> Unit) -> Unit = {},
    val onBuild: () -> Unit = {},
    val onBuildRepeat: (Boolean) -> Unit = {},
    val onBuildSkip: (Int) -> Unit = {},
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
                onGuidedStep = viewModel::setGuidedStep,
                onRevealTest = viewModel::revealTest,
                onAdvanced = viewModel::setAdvanced,
                onPractiseNow = viewModel::practiseNow,
                onListen = viewModel::playListen,
                onListenFrom = viewModel::playListen,
                onListenSkip = viewModel::skipListenPhrase,
                onListenRepeat = viewModel::cycleListenRepeat,
                onUpdateReading = viewModel::updateReading,
                onBuild = viewModel::playBuild,
                onBuildRepeat = viewModel::setBuildRepeat,
                onBuildSkip = viewModel::skipBuildChunk,
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
 * The session screen. Guided (the default) shows one step at a time, see [GuidedSession]. Advanced:
 * the dua's text owns the screen, the step's main action and status sit in a docked sheet above the
 * bottom navigation, and the step's secondary controls open from that sheet. Check has no sheet:
 * its rating buttons are the content.
 */
@Composable
fun MemorizeSessionScreen(
    state: MemorizeUiState,
    actions: MemorizeActions,
    talkBack: Boolean = talkBackOn(),
) {
    // A–B: true between the first and the second phrase tap. Held here so closing the sheet keeps it.
    var abAwaiting by rememberSaveable(state.dua.id) { mutableStateOf(false) }
    // Recall: the meaning opens from the top bar's title line, the cue to recite from.
    var meaning by rememberSaveable(state.dua.id) { mutableStateOf(false) }
    // Reading display: dua layout, progress line, translation and text size, from the top bar on every step.
    var readingSheet by rememberSaveable { mutableStateOf(false) }
    if (readingSheet) ReadingSheet(state.appSettings, actions.onUpdateReading, layoutAppliesNot = state.step == Step.RECALL) { readingSheet = false }
    if (!state.advanced) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
            val dockMax = maxHeight * DOCK_MAX_SHARE
            Column(Modifier.fillMaxSize()) {
                SessionTopBar(state, actions, meaning, onReading = { readingSheet = true }) { meaning = !meaning }
                GuidedSession(state, actions, talkBack, meaning, dockMax)
            }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val dockMax = maxHeight * DOCK_MAX_SHARE
        Column(Modifier.fillMaxSize()) {
            SessionTopBar(state, actions, meaning, onReading = { readingSheet = true }) { meaning = !meaning }
            // The dock starts collapsed on every step: the step's main action and status stay in view, the chevron opens
            // the rest. Back closes it first. At 200% font the open dock takes at most 60% of the screen and scrolls inside.
            var dockOpen by rememberSaveable(state.dua.id, state.step) { mutableStateOf(false) }
            BackHandler(enabled = dockOpen) { dockOpen = false }
            val toggle: @Composable () -> Unit = { DockToggle(dockOpen) { dockOpen = !dockOpen } }
            val dock: (@Composable () -> Unit)? = when (state.step) {
                Step.LISTEN -> {{ Dock(false, dockMax, listenDock(state, actions), null) }} // the media bar is the whole dock
                Step.BUILD -> {{ Dock(dockOpen, dockMax, buildDock(state, actions, toggle), buildMore(state, actions, abAwaiting) { abAwaiting = it }) }}
                Step.RECALL -> {{ Dock(dockOpen, dockMax, recallBarDock(state, actions, toggle), recallMore(state, actions, talkBack)) }}
                Step.CHECK -> null // the ratings are the end of the content, see CheckStep
            }
            // The text runs under the glass dock and pads its end by the dock's height, so the last line scrolls clear of it.
            GlassBarHost(bar = dock, modifier = Modifier.weight(1f)) { barHeight ->
                val scroll = rememberScrollState()
                // Listen: the text scrolls to the phrase that plays, above the dock.
                val follow = rememberFollowPhrase(scroll, if (state.step == Step.LISTEN) state.playback.activePhrase else -1, barHeight)
                Column(
                    Modifier
                        .fillMaxSize()
                        .then(follow.viewportModifier)
                        .verticalScroll(scroll)
                        .padding(HahoTheme.dimens.screenGutter)
                        .padding(bottom = barHeight),
                    verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
                ) {
                    val hint = if (state.fromReview && state.step == Step.RECALL) R.string.memorize_hint_review else state.step.hintRes()
                    InfoNote(stringResource(hint))
                    when (state.step) {
                        Step.LISTEN -> ListenStep(state, actions, follow)
                        Step.BUILD -> BuildStep(state, bar = true)
                        Step.RECALL -> RecallStep(state, talkBack, meaning, bar = true)
                        Step.CHECK -> CheckStep(state, actions)
                    }
                }
            }
            SessionNavBar(state.step, actions.onStep)
        }
    }
}

private const val DOCK_MAX_SHARE = 0.6f

/** Where the recital cue's meaning is offered: wherever the text is hidden. */
private fun offersMeaning(state: MemorizeUiState) = if (state.advanced) {
    state.step == Step.RECALL
} else {
    state.guided == GuidedStep.GAPS || (state.guided == GuidedStep.TEST && !state.testRevealed)
}

@Composable
private fun SessionTopBar(state: MemorizeUiState, actions: MemorizeActions, meaning: Boolean, onReading: () -> Unit, onMeaning: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        // The bar is the same on every Memorize screen; the dua is the heading under it.
        Row(Modifier.fillMaxWidth().padding(horizontal = HahoTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close)) }
            Text(
                stringResource(R.string.memorize_session_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onReading) {
                Icon(painterResource(R.drawable.ic_text_fields), stringResource(R.string.dua_reading_settings))
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.memorize_session_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.memorize_switch_listen)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_headphones), null) },
                        enabled = state.hasAudio,
                        onClick = { menu = false; actions.onListenOnly() },
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = HahoTheme.dimens.screenGutter).semantics(mergeDescendants = true) { heading() }) {
            Text(state.eventLabel, style = MaterialTheme.typography.titleLarge)
            if (state.positionOf > 1 && state.position > 0) {
                Text(
                    stringResource(R.string.memorize_position, state.position, state.positionOf),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (offersMeaning(state)) {
            // The cue is the heading; its meaning is one tap away, on its own line so it never breaks inside the word.
            val shown = stringResource(if (meaning) R.string.memorize_dock_state_expanded else R.string.memorize_dock_state_collapsed)
            Text(
                stringResource(R.string.memorize_cue_meaning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                softWrap = false,
                modifier = Modifier
                    .padding(start = HahoTheme.dimens.screenGutter - HahoTheme.spacing.sm)
                    .clickable(role = Role.Button, onClick = onMeaning)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = HahoTheme.spacing.sm)
                    .semantics { stateDescription = shown },
            )
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
 * needs; [more] holds the step's other controls, shown under it in Advanced.
 */
@Composable
internal fun Dock(
    expanded: Boolean,
    maxHeight: Dp,
    collapsed: @Composable ColumnScope.() -> Unit,
    more: (@Composable ColumnScope.() -> Unit)?,
    /** Pad the gesture bar: on for a screen with no tab bar under the dock. */
    insets: Boolean = false,
) {
    GlassBarSurface(expanded = expanded && more != null, insets = insets) {
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .animateContentSize()
                .padding(horizontal = HahoTheme.dimens.screenGutter)
                .padding(vertical = HahoTheme.spacing.sm),
        ) {
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
internal fun DockToggle(expanded: Boolean, onClick: () -> Unit) {
    val state = stringResource(if (expanded) R.string.memorize_dock_state_expanded else R.string.memorize_dock_state_collapsed)
    // Icon only: the label is spoken, and the 48 dp button sits centred on the main button.
    IconButton(onClick = onClick, modifier = Modifier.semantics { stateDescription = state }) {
        Icon(
            painterResource(if (expanded) R.drawable.ic_keyboard_arrow_down else R.drawable.ic_keyboard_arrow_up),
            stringResource(if (expanded) R.string.memorize_dock_less else R.string.memorize_dock_more),
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
internal fun ArabicCard(
    state: MemorizeUiState,
    active: Int = state.playback.activePhrase,
    focus: Boolean = false,
    layout: PhraseLayout = PhraseLayout.FOCUS,
    focusNav: Boolean = true,
    onPlayFrom: ((Int) -> Unit)? = null,
    follow: FollowPhrase? = null,
) {
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(HahoTheme.spacing.xl)) {
            val style = AdwamTheme.arabic.large(state.dua.isQuran)
            if (focus) {
                PhraseLines(state.dua.arabic, state.dua.phrases, active, style, layout = layout, focusNav = focusNav, onPlayFrom = onPlayFrom, follow = follow)
            } else {
                HighlightedArabic(state.dua.arabic, state.dua.phrases, active, style)
            }
        }
    }
}

@Composable
internal fun RestartButton(onStart: () -> Unit) {
    TextButton(onClick = onStart) {
        Icon(painterResource(R.drawable.ic_replay), null, Modifier.size(HahoTheme.dimens.smallIcon))
        Text(stringResource(R.string.memorize_restart), Modifier.padding(start = HahoTheme.spacing.xs))
    }
}

@Composable
internal fun ListenStep(state: MemorizeUiState, actions: MemorizeActions? = null, follow: FollowPhrase? = null) {
    // With the media bar below, the card needs no Back/Next of its own. Without audio there is no bar, so it keeps them.
    ArabicCard(
        state,
        focus = true,
        layout = if (actions != null) state.phraseLayout else PhraseLayout.FOCUS,
        focusNav = actions == null || !state.hasAudio,
        onPlayFrom = if (actions != null && state.hasAudio) actions.onListenFrom else null,
        follow = follow,
    )
    if (!state.hasAudio) InfoNote(stringResource(R.string.dua_no_audio), icon = R.drawable.ic_volume_off)
    if (state.playback.failed) {
        InfoNote(
            stringResource(R.string.audio_failed),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            icon = R.drawable.ic_volume_off,
        )
    }
}

internal fun listenLoaded(state: MemorizeUiState) = state.playing == Playing.LISTEN && !state.playback.ended

/** Listen's transport is the same media bar as the dua player: track, repeat, previous, play, next, speed. */
private fun listenDock(state: MemorizeUiState, actions: MemorizeActions): @Composable ColumnScope.() -> Unit = {
    val loaded = listenLoaded(state)
    val active = if (loaded) state.playback.activePhrase else -1
    PlayerControls(
        isPlaying = loaded && state.playback.isPlaying,
        speed = state.settings.speed,
        repeat = false,
        enabled = state.hasAudio,
        onToggle = if (loaded) actions.onToggle else actions.onListen,
        onSpeed = actions.onSpeed,
        onRepeat = {},
        onPreviousPhrase = { actions.onListenSkip(-1) },
        onNextPhrase = { actions.onListenSkip(1) },
        canPrevious = active > 0,
        canNext = active < state.dua.phrases.lastIndex,
        phraseCount = state.dua.phrases.size,
        activePhrase = active,
        onSeekPhrase = actions.onListenFrom,
        repeatControl = { ListenRepeatButton(state.listenRepeat, state.hasAudio, actions.onListenRepeat) },
        speeds = LISTEN_SPEEDS,
        showTrack = state.appSettings.showPhraseTrack,
    )
}

private val LISTEN_SPEEDS = SPEEDS.filter { it <= 1f }

/** One button that cycles twice, loop, once; the mode is a badge and is spoken. 48 dp. */
@Composable
private fun ListenRepeatButton(mode: ListenRepeat, enabled: Boolean, onCycle: () -> Unit) {
    val state = stringResource(
        when (mode) {
            ListenRepeat.TWICE -> R.string.memorize_repeat_twice
            ListenRepeat.LOOP -> R.string.memorize_repeat_loop
            ListenRepeat.ONCE -> R.string.memorize_repeat_once
        },
    )
    IconButton(onClick = onCycle, enabled = enabled, modifier = Modifier.size(HahoTheme.dimens.minTouchTarget).semantics { stateDescription = state }) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_repeat),
                stringResource(R.string.player_repeat),
                tint = if (mode == ListenRepeat.ONCE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
            val badge = when (mode) {
                ListenRepeat.TWICE -> "2"
                ListenRepeat.LOOP -> "∞"
                ListenRepeat.ONCE -> null
            }
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.offset(x = 12.dp, y = (-10).dp))
            }
        }
    }
}

@Composable
internal fun BuildStep(state: MemorizeUiState, bar: Boolean = false) {
    // With the media bar the card follows the reader's layout and has no Back/Next of its own (it keeps them without audio).
    if (bar) {
        ArabicCard(state, focus = true, layout = state.phraseLayout, focusNav = !state.hasAudio)
    } else {
        ArabicCard(state, focus = true)
    }
    if (!state.segmented) InfoNote(stringResource(R.string.memorize_not_segmented))
    if (state.hasAudio && state.dua.hasUnheardPhrases) InfoNote(stringResource(R.string.dua_unheard_note), icon = R.drawable.ic_volume_off)
}

/**
 * Build's transport is the standard media bar: repeat (loop what plays until stopped), previous and next segment, play, speed.
 * Play plays the picked part, the whole dua by default (the cumulative build).
 */
private fun buildDock(state: MemorizeUiState, actions: MemorizeActions, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    val building = state.playing == Playing.BUILD && !state.playback.ended
    val loaded = building || (state.playing == Playing.AB && !state.playback.ended)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (loaded) {
                BuildProgress(state)
            } else if (partPicked(state)) {
                Text(
                    stringResource(R.string.memorize_build_part, state.ab.first + 1, state.ab.last + 1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(stringResource(R.string.memorize_build_start), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        toggle()
    }
    val chunk = buildChunk(state)
    PlayerControls(
        isPlaying = loaded && state.playback.isPlaying,
        speed = state.settings.speed,
        repeat = state.buildRepeat,
        enabled = state.hasAudio,
        onToggle = if (loaded) actions.onToggle else actions.onBuild,
        onSpeed = actions.onSpeed,
        onRepeat = actions.onBuildRepeat,
        onPreviousPhrase = { actions.onBuildSkip(-1) },
        onNextPhrase = { actions.onBuildSkip(1) },
        canPrevious = building && chunk > 0,
        canNext = building && chunk < buildChunks(state) - 1,
        phraseCount = state.dua.phrases.size,
        activePhrase = if (loaded) state.playback.activePhrase else -1,
        speeds = LISTEN_SPEEDS,
        showTrack = state.appSettings.showPhraseTrack,
        trackSeekable = false,
        previousLabel = stringResource(R.string.player_previous_segment),
        nextLabel = stringResource(R.string.player_next_segment),
    )
}

/** A part of the dua is picked (not the whole), so play plays that part. */
private fun partPicked(state: MemorizeUiState) = state.segmented && state.ab != AbSelection.whole(state.dua.phrases.size).range

/** Build repeats each segment [MemorizeSettings.loopsPerChunk] times, a play and a pause per loop. */
private fun buildChunks(state: MemorizeUiState) = if (state.segmented && state.playing == Playing.BUILD) state.dua.phrases.count { it.segment != null } else 1

private fun buildChunk(state: MemorizeUiState) = state.playback.sequenceIndex.coerceAtLeast(0) / (2 * state.settings.loopsPerChunk.coerceIn(1, 10))

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

/** What plays and whose turn it is, or "Paused": a live region, so TalkBack announces each change. */
@Composable
internal fun BuildProgress(state: MemorizeUiState) {
    val loops = state.settings.loopsPerChunk.coerceIn(1, 10)
    val i = state.playback.sequenceIndex.coerceAtLeast(0)
    val part = state.playing == Playing.AB
    val chunks = buildChunks(state)
    val detail = if (part) {
        stringResource(R.string.memorize_build_part_progress, state.ab.first + 1, state.ab.last + 1, (i / 2 + 1).coerceAtMost(loops), loops)
    } else {
        stringResource(R.string.memorize_build_progress, (buildChunk(state) + 1).coerceAtMost(chunks), chunks, (i % (2 * loops)) / 2 + 1, loops)
    }
    val title = when {
        !state.playback.isPlaying -> stringResource(R.string.memorize_paused)
        state.playback.inPause -> stringResource(R.string.memorize_your_turn)
        else -> stringResource(R.string.memorize_listen_now)
    }
    Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Pick the part Play plays: the dua's phrases are the targets. The first tap sets one end, the second the other
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
internal fun RecallStep(state: MemorizeUiState, talkBack: Boolean, meaning: Boolean, bar: Boolean = false) {
    // With the action row in the dock, the stage and hint level sit above the text instead of taking a row in the dock.
    if (bar) RecallStageLine(state)
    if (meaning) {
        GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(HahoTheme.spacing.lg)) { DuaTranslations(state.dua, state.reading) }
        }
    }
    if (state.practising) {
        // Guided has no hint levels to pick, so practice ends when every marked word is recited.
        InfoNote(stringResource(if (state.advanced) R.string.memorize_practising else R.string.memorize_practising_guided), icon = R.drawable.ic_replay)
    }
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

/** The stage and the hint level in words, above the text: where the learner is, not a control. */
@Composable
private fun RecallStageLine(state: MemorizeUiState) {
    val hints = when {
        state.practising -> stringResource(R.string.memorize_hints_missed)
        state.fade == FadeLevel.NONE -> stringResource(R.string.memorize_hints_off)
        else -> stringResource(R.string.memorize_hints_hidden, (state.fade.fraction * 100).toInt())
    }
    val line = if (state.stage >= 1 && !state.practising) "${stringResource(R.string.memorize_stage, state.stage, FadeLadder.LAST)} · $hints" else hints
    Text(line, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

/**
 * Recall's dock: Done on the left, play with gaps in the centre, Next word on the right (it is the action tapped most, so it sits under the
 * right thumb), and the settings chevron. Speed lives in the settings; there is no repeat here.
 */
internal fun recallBarDock(state: MemorizeUiState, actions: MemorizeActions, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    val playing = state.playing == Playing.RECALL && !state.playback.ended
    val doneDescription = stringResource(R.string.memorize_recited_description)
    if (playing) {
        Text(
            if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    val done: @Composable (Modifier) -> Unit = { m ->
        GlassButton(onClick = actions.onFinishRecital, modifier = m.semantics { contentDescription = doneDescription }) { Text(stringResource(R.string.memorize_recited)) }
    }
    val next: (@Composable (Modifier) -> Unit)? = if (hasHints(state)) {
        { m ->
            // "Next" in the bar; TalkBack still says what it opens.
            val spoken = stringResource(R.string.memorize_reveal_next_description)
            GlassOutlinedButton(onClick = actions.onRevealNext, enabled = state.hiddenWords.isNotEmpty(), modifier = m.semantics { contentDescription = spoken }) {
                Text(stringResource(R.string.memorize_reveal_next))
            }
        }
    } else {
        null
    }
    // Play with gaps needs phrase segments and audio, like the A–B loop: hidden rather than disabled until the audio is marked.
    val play: (@Composable () -> Unit)? = if (state.segmented) {
        {
            GlassIconButton(
                onClick = if (playing) actions.onToggle else actions.onRecallGaps,
                enabled = state.hasAudio && (playing || state.hiddenWords.isNotEmpty()),
                modifier = Modifier.size(AdwamDimens.playerCompactButton),
            ) {
                Icon(
                    painterResource(if (playing && state.playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    stringResource(if (playing && state.playback.isPlaying) R.string.player_pause else R.string.memorize_play_with_gaps),
                    Modifier.size(HahoTheme.dimens.icon * 1.25f),
                )
            }
        }
    } else {
        null
    }
    // Three buttons and a chevron do not fit side by side at large text: from 1.3x they stack, as the old row did.
    if (LocalDensity.current.fontScale > STACK_FONT_SCALE) {
        Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            done(Modifier.fillMaxWidth())
            next?.invoke(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                if (play != null) play() else Spacer(Modifier.size(AdwamDimens.playerCompactButton))
                toggle()
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            done(Modifier.weight(1f))
            play?.invoke()
            next?.invoke(Modifier.weight(1f))
            toggle()
        }
    }
}

internal fun recallDock(state: MemorizeUiState, actions: MemorizeActions, showStage: Boolean = true, toggle: @Composable () -> Unit): @Composable ColumnScope.() -> Unit = {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            if (state.playing == Playing.RECALL && state.playback.isPlaying) {
                Text(
                    if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            // Guided has its own "Langkah n dari 4" in the header; a second counter would disagree.
            if (showStage && state.stage >= 1 && !state.practising) {
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
            val spoken = stringResource(R.string.memorize_reveal_next_description)
            GlassOutlinedButton(onClick = actions.onRevealNext, enabled = state.hiddenWords.isNotEmpty(), modifier = m.semantics { contentDescription = spoken }) {
                Text(stringResource(R.string.memorize_reveal_next))
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

internal const val STACK_FONT_SCALE = 1.3f

/** A check mark on a selected chip, so the state is not carried by the fill alone (WCAG 1.4.1, 1.4.11). */
private fun selectedCheck(selected: Boolean): (@Composable () -> Unit)? =
    if (selected) {
        { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
    } else {
        null
    }

private fun recallMore(state: MemorizeUiState, actions: MemorizeActions, talkBack: Boolean): @Composable ColumnScope.() -> Unit = {
    val gaps = state.segmented && state.hasAudio
    Text(stringResource(R.string.memorize_fade_level), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
        FadeLevel.entries.forEach { level ->
            // While practising, the missed words are hidden, not a share of them: no level is current.
            FilterChip(
                selected = !state.practising && state.fade == level,
                onClick = { actions.onFade(level) },
                modifier = Modifier.heightIn(min = HahoTheme.dimens.minTouchTarget),
                leadingIcon = selectedCheck(!state.practising && state.fade == level),
                label = { Text(stringResource(R.string.memorize_fade_percent, (level.fraction * 100).toInt())) },
            )
        }
    }
    if (!state.practising && (state.fade == FadeLevel.LIGHT || state.fade == FadeLevel.MEDIUM)) {
        Text(stringResource(R.string.memorize_fade_order), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
            FadeOrder.entries.forEach { order ->
                FilterChip(
                    selected = state.fadeOrder == order,
                    onClick = { actions.onFadeOrder(order) },
                    modifier = Modifier.heightIn(min = HahoTheme.dimens.minTouchTarget),
                    leadingIcon = selectedCheck(state.fadeOrder == order),
                    label = { Text(stringResource(order.labelRes())) },
                )
            }
        }
    }
    if (hasHints(state)) {
        FilterChip(
            selected = state.firstLetters,
            onClick = { actions.onFirstLetters(!state.firstLetters) },
            modifier = Modifier.heightIn(min = HahoTheme.dimens.minTouchTarget),
            leadingIcon = selectedCheck(state.firstLetters),
            label = { Text(stringResource(R.string.memorize_first_letters)) },
        )
    }
    if (talkBack && gaps) InfoNote(stringResource(R.string.memorize_talkback_recall), icon = R.drawable.ic_headphones)
    // Play with gaps plays at the speed Listen and Build use; the bar has no speed button here.
    if (state.segmented) MemorizeSpeedControl(state.settings, actions.onSpeed)
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
                    FadeLadder.Outcome.DONE -> if (state.advanced) R.string.memorize_attempt_done else R.string.memorize_attempt_done_guided
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
    // The ratings close the content instead of floating over it: the word grid is never covered.
    if (rated != null) RatedResult(state, rated, actions) else Column { checkDock(state, actions).invoke(this) }
}

/**
 * Check's ratings, at the end of the content. Three equal buttons in one row, each with its interval underneath; from a larger
 * font they stack full width with the full wording. The suggested rating (only when words were marked) is
 * filled and named in text, so colour is not the only cue.
 */
private fun checkDock(state: MemorizeUiState, actions: MemorizeActions): @Composable ColumnScope.() -> Unit = {
    val markable = Recall.recallable(state.words).size
    val current = state.review?.state
    // Only words marked count as evidence; with none marked, nothing is suggested.
    val suggested = if (state.missedWords.isEmpty()) null else Leitner.suggestedRating(state.missedWords.size, markable)
    Text(stringResource(R.string.memorize_rate_prompt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    if (suggested != null) {
        Text(
            stringResource(R.string.memorize_suggested_rating, stringResource(suggested.labelRes())),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val stacked = LocalDensity.current.fontScale > RATING_ROW_MAX_FONT_SCALE
    val today = java.time.LocalDate.now()
    val buttons: @Composable (Modifier) -> Unit = { m ->
        Rating.entries.forEach { rating ->
            val days = Leitner.INTERVALS[Leitner.rate(current, rating, today).box - 1]
            RatingButton(rating, days, rating == suggested, compact = !stacked, onClick = { actions.onRate(rating) }, modifier = m)
        }
    }
    if (stacked) {
        Column(Modifier.padding(top = HahoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            buttons(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.padding(top = HahoTheme.spacing.sm).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
        ) {
            buttons(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Row mode shortens the interval to "3 days" and trims the side padding; TalkBack always hears the full wording. */
@Composable
private fun RatingButton(rating: Rating, days: Int, suggested: Boolean, compact: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val full = pluralStringResource(R.plurals.memorize_next_in_days, days, days)
    val spoken = stringResource(rating.labelRes()) + ", " + if (suggested) stringResource(R.string.memorize_suggested_in_days, full) else full
    val interval = if (compact) pluralStringResource(R.plurals.memorize_next_in_days_short, days, days) else full
    val content: @Composable RowScope.() -> Unit = {
        Column(Modifier.padding(vertical = HahoTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(rating.labelRes()), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(interval, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
    val m = modifier.semantics { contentDescription = spoken }
    val padding = if (compact) PaddingValues(horizontal = HahoTheme.spacing.sm, vertical = HahoTheme.spacing.sm) else ButtonDefaults.ContentPadding
    if (suggested) {
        GlassButton(onClick = onClick, modifier = m, contentPadding = padding, content = content)
    } else {
        GlassOutlinedButton(onClick = onClick, modifier = m, contentPadding = padding, content = content)
    }
}

/** Three equal buttons need the width: from this font scale they stack instead. */
private const val RATING_ROW_MAX_FONT_SCALE = 1.15f

/** After rating: when the dua comes back, then the next due dua (in a review) or Done. */
@Composable
internal fun RatedResult(state: MemorizeUiState, rated: LeitnerState, actions: MemorizeActions, practiseNow: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(HahoTheme.spacing.lg).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Text(stringResource(R.string.memorize_scheduled, formatLongDate(rated.due)), style = MaterialTheme.typography.titleMedium)
            // The Leitner box number is an internal detail and means nothing to the reader: only say when it counts as memorized.
            if (Leitner.isMemorized(rated)) Text(stringResource(R.string.memorize_memorized), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (practiseNow) {
        GlassButton(onClick = actions.onPractiseNow, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_replay), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(stringResource(R.string.memorize_practise_now), Modifier.padding(start = HahoTheme.spacing.sm))
        }
    }
    val next = state.nextDueId
    if (next != null) {
        val primary = !practiseNow
        if (primary) {
            GlassButton(onClick = { actions.onNext(next) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.memorize_next_review)) }
        } else {
            GlassOutlinedButton(onClick = { actions.onNext(next) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.memorize_next_review)) }
        }
    }
    GlassOutlinedButton(onClick = actions.onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done)) }
}

/** Mark the words missed in the recital; they can then be practised on their own in Recall. */
@Composable
internal fun MarkMissed(state: MemorizeUiState, actions: MemorizeActions) {
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

internal fun Rating.labelRes() = when (this) {
    Rating.AGAIN -> R.string.memorize_not_yet
    Rating.HARD -> R.string.memorize_with_help
    Rating.EASY -> R.string.memorize_knew_it
}
