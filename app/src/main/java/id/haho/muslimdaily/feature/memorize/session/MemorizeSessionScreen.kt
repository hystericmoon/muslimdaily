package id.haho.muslimdaily.feature.memorize.session

import androidx.compose.ui.graphics.Color
import id.haho.design.glass.GlassBottomSheet
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import id.haho.muslimdaily.domain.memorize.FadeLadder
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
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

@Composable
fun MemorizeSessionScreen(state: MemorizeUiState, actions: MemorizeActions, talkBack: Boolean = talkBackOn()) {
    var settingsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
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
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = spoken },
                )
            }
            IconButton(onClick = actions.onListenOnly, enabled = state.hasAudio) {
                Icon(painterResource(R.drawable.ic_headphones), stringResource(R.string.memorize_listen_only))
            }
            IconButton(onClick = { settingsOpen = true }) {
                Icon(painterResource(R.drawable.ic_tune), stringResource(R.string.memorize_settings))
            }
        }
        val steps = Step.entries
        PrimaryScrollableTabRow(
            selectedTabIndex = state.step.ordinal,
            edgePadding = HahoTheme.dimens.screenGutter,
            containerColor = Color.Transparent,
        ) {
            steps.forEach { step ->
                Tab(
                    selected = state.step == step,
                    onClick = { actions.onStep(step) },
                    text = { Text(stringResource(R.string.memorize_step_n, step.ordinal + 1, stringResource(step.labelRes())), maxLines = 1) },
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(HahoTheme.dimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
        ) {
            val hint = if (state.fromReview && state.step == Step.RECALL) R.string.memorize_hint_review else state.step.hintRes()
            InfoNote(stringResource(hint))
            when (state.step) {
                Step.LISTEN -> ListenStep(state, actions)
                Step.BUILD -> BuildStep(state, actions)
                Step.RECALL -> RecallStep(state, actions, talkBack)
                Step.CHECK -> CheckStep(state, actions)
            }
        }
    }
    if (settingsOpen) {
        GlassBottomSheet(onDismissRequest = { settingsOpen = false }) {
            MemorizeSettingsControls(
                settings = state.settings,
                onLoops = actions.onLoops,
                onPauseMode = actions.onPauseMode,
                onPauseSeconds = actions.onPauseSeconds,
                onSpeed = actions.onSpeed,
                modifier = Modifier.padding(bottom = HahoTheme.spacing.xxl),
            )
        }
    }
}

private fun Step.labelRes() = when (this) {
    Step.LISTEN -> R.string.memorize_step_listen
    Step.BUILD -> R.string.memorize_step_build
    Step.RECALL -> R.string.memorize_step_recall
    Step.CHECK -> R.string.memorize_step_check
}

private fun Step.hintRes() = when (this) {
    Step.LISTEN -> R.string.memorize_hint_listen
    Step.BUILD -> R.string.memorize_hint_build
    Step.RECALL -> R.string.memorize_hint_recall
    Step.CHECK -> R.string.memorize_hint_check
}

@Composable
private fun ArabicCard(state: MemorizeUiState, active: Int = state.playback.activePhrase) {
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(HahoTheme.spacing.xl)) {
            HighlightedArabic(state.dua.arabic, state.dua.phrases, active, AdwamTheme.arabic.large(state.dua.isQuran))
        }
    }
}

@Composable
private fun PlayRow(isPlaying: Boolean, enabled: Boolean, startLabel: String, onStart: () -> Unit, onToggle: () -> Unit, loaded: Boolean, speed: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.md)) {
        if (loaded) {
            GlassIconButton(onClick = onToggle, modifier = Modifier.size(AdwamDimens.playerMainButton)) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                    Modifier.size(HahoTheme.dimens.icon * 1.5f),
                )
            }
            TextButton(onClick = onStart) {
                Icon(painterResource(R.drawable.ic_replay), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Text(stringResource(R.string.memorize_restart), Modifier.padding(start = HahoTheme.spacing.xs))
            }
        } else {
            GlassButton(onClick = onStart, enabled = enabled, modifier = Modifier.padding(vertical = HahoTheme.spacing.xs)) {
                Icon(painterResource(R.drawable.ic_play_arrow), null)
                Spacer(Modifier.width(HahoTheme.spacing.sm))
                Text(startLabel)
            }
        }
        Text(speedLabel(speed), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ListenStep(state: MemorizeUiState, actions: MemorizeActions) {
    ArabicCard(state)
    if (!state.hasAudio) InfoNote(stringResource(R.string.dua_no_audio), icon = R.drawable.ic_volume_off)
    if (state.playback.failed) {
        InfoNote(
            stringResource(R.string.audio_failed),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            icon = R.drawable.ic_volume_off,
        )
    }
    PlayRow(
        isPlaying = state.playback.isPlaying,
        enabled = state.hasAudio,
        startLabel = stringResource(R.string.memorize_listen_twice),
        onStart = actions.onListen,
        onToggle = actions.onToggle,
        loaded = state.playing == Playing.LISTEN && !state.playback.ended,
        speed = state.settings.speed,
    )
    NextStepButton(Step.BUILD, actions)
}

@Composable
private fun BuildStep(state: MemorizeUiState, actions: MemorizeActions) {
    ArabicCard(state)
    if (!state.segmented) InfoNote(stringResource(R.string.memorize_not_segmented))
    val loaded = (state.playing == Playing.BUILD || state.playing == Playing.AB) && !state.playback.ended
    if (loaded) BuildProgress(state)
    PlayRow(
        isPlaying = state.playback.isPlaying,
        enabled = state.hasAudio,
        startLabel = stringResource(R.string.memorize_build_start),
        onStart = actions.onBuild,
        onToggle = actions.onToggle,
        loaded = state.playing == Playing.BUILD && !state.playback.ended,
        speed = state.settings.speed,
    )
    if (state.segmented && state.dua.phrases.size > 1) AbLoop(state, actions)
    NextStepButton(Step.RECALL, actions)
}

@Composable
private fun BuildProgress(state: MemorizeUiState) {
    val loops = state.settings.loopsPerChunk.coerceIn(1, 10)
    val i = state.playback.sequenceIndex.coerceAtLeast(0)
    val chunk = i / (2 * loops)
    val loop = (i % (2 * loops)) / 2 + 1
    val chunks = if (state.segmented && state.playing == Playing.BUILD) state.dua.phrases.size else 1
    Surface(
        color = if (state.playback.inPause) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (state.playback.inPause) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(HahoTheme.spacing.lg).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Text(
                if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.memorize_build_progress, (chunk + 1).coerceAtMost(chunks), chunks, loop, loops),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AbLoop(state: MemorizeUiState, actions: MemorizeActions) {
    GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(HahoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            Text(stringResource(R.string.memorize_ab_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.memorize_ab_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val n = state.dua.phrases.size
            PhrasePicker(stringResource(R.string.memorize_ab_from), n, state.ab.first) { a -> actions.onSetAb(a..maxOf(a, state.ab.last)) }
            PhrasePicker(stringResource(R.string.memorize_ab_to), n, state.ab.last) { b -> actions.onSetAb(minOf(state.ab.first, b)..b) }
            GlassOutlinedButton(onClick = actions.onAb, enabled = state.hasAudio) {
                Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(HahoTheme.dimens.smallIcon))
                Text(
                    stringResource(R.string.memorize_ab_play, state.ab.first + 1, state.ab.last + 1),
                    Modifier.padding(start = HahoTheme.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun PhrasePicker(label: String, count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
            repeat(count) { i ->
                FilterChip(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    label = { Text(stringResource(R.string.memorize_phrase_n, i + 1)) },
                )
            }
        }
    }
}

/** What the dua is for, and on request its meaning: the cue to recite from before any Arabic shows. */
@Composable
private fun CueCard(state: MemorizeUiState) {
    var meaning by rememberSaveable(state.dua.id) { mutableStateOf(false) }
    GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(HahoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
            Text(
                stringResource(R.string.memorize_cue_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(state.eventLabel, style = MaterialTheme.typography.titleMedium)
            if (state.positionOf > 1 && state.position > 0) {
                Text(
                    stringResource(R.string.memorize_cue_position, state.position, state.positionOf),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (meaning) DuaTranslations(state.dua, state.reading)
            TextButton(onClick = { meaning = !meaning }) {
                Text(stringResource(if (meaning) R.string.memorize_cue_hide_meaning else R.string.memorize_cue_show_meaning))
            }
        }
    }
}

@Composable
private fun RecallStep(state: MemorizeUiState, actions: MemorizeActions, talkBack: Boolean) {
    CueCard(state)
    if (state.practising) InfoNote(stringResource(R.string.memorize_practising), icon = R.drawable.ic_replay)
    // Play with gaps needs phrase segments and audio; without them the hints point to the reveal button.
    val gaps = state.segmented && state.hasAudio
    // The text comes first: it is what the learner recites from. The fade controls tune it below.
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
    if (state.fade != FadeLevel.NONE) {
        // Reveal as you go: recite the next word aloud, then show it. The word shown is announced
        // as Arabic text (a stateDescription can't carry the Arabic locale, so the UI voice would read it).
        val last = state.lastRevealed?.text.orEmpty()
        GlassOutlinedButton(onClick = actions.onRevealNext, enabled = state.hiddenWords.isNotEmpty()) {
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(stringResource(R.string.memorize_reveal_next), Modifier.padding(start = HahoTheme.spacing.sm))
        }
        if (last.isNotEmpty()) {
            ArabicText(
                last,
                style = AdwamTheme.arabic.body(state.dua.isQuran),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    RecitalCheck(state, actions)
    val levels = FadeLevel.entries
    Text(stringResource(R.string.memorize_fade_level), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm)) {
        levels.forEach { level ->
            FilterChip(
                selected = state.fade == level,
                onClick = { actions.onFade(level) },
                label = { Text(stringResource(R.string.memorize_fade_percent, (level.fraction * 100).toInt())) },
            )
        }
    }
    if (state.fade == FadeLevel.LIGHT || state.fade == FadeLevel.MEDIUM) {
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
    if (state.fade != FadeLevel.NONE) {
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
    if (state.playing == Playing.RECALL && state.playback.isPlaying) {
        Text(
            if (state.playback.inPause) stringResource(R.string.memorize_your_turn) else stringResource(R.string.memorize_listen_now),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    NextStepButton(Step.CHECK, actions)
}

/** Ends a recital: the hints fade, stay or come back depending on the words revealed (vanishing cues). */
@Composable
private fun RecitalCheck(state: MemorizeUiState, actions: MemorizeActions) {
    if (state.stage >= 1 && !state.practising) {
        Text(
            stringResource(R.string.memorize_stage, state.stage, FadeLadder.LAST),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    GlassButton(onClick = actions.onFinishRecital, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.memorize_recited))
    }
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

@Composable
private fun NextStepButton(next: Step, actions: MemorizeActions) {
    TextButton(onClick = { actions.onStep(next) }) {
        Text(stringResource(R.string.memorize_next_step, stringResource(next.labelRes())))
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.padding(start = HahoTheme.spacing.xs))
    }
}
