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
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.speedLabel
import id.haho.muslimdaily.feature.memorize.MemorizeSettingsControls

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
    val onRate: (Rating) -> Unit = {},
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
                onRate = viewModel::rate,
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
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.memorize_session_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(state.eventLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val levels = FadeLevel.entries
    Text(stringResource(R.string.memorize_fade_level), style = MaterialTheme.typography.titleSmall)
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
        Text(stringResource(R.string.memorize_fade_order), style = MaterialTheme.typography.titleSmall)
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
    GlassCard(shape = MaterialTheme.shapes.extraLarge) {
        RecallText(
            words = state.words,
            hidden = state.hiddenWords,
            style = AdwamTheme.arabic.large(state.dua.isQuran),
            hiddenDescription = pluralStringResource(R.plurals.memorize_hidden_words, state.hiddenWords.size, state.hiddenWords.size),
            firstLetters = state.firstLetters,
            modifier = Modifier.padding(HahoTheme.spacing.xl),
        )
    }
    if (state.fade != FadeLevel.NONE) {
        // Reveal as you go: recite the next word aloud, then show it. TalkBack hears the word shown.
        val last = state.lastRevealed?.text.orEmpty()
        GlassOutlinedButton(
            onClick = actions.onRevealNext,
            enabled = state.hiddenWords.isNotEmpty(),
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (last.isNotEmpty()) stateDescription = last
            },
        ) {
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(HahoTheme.dimens.smallIcon))
            Text(stringResource(R.string.memorize_reveal_next), Modifier.padding(start = HahoTheme.spacing.sm))
        }
    }
    if (talkBack) InfoNote(stringResource(R.string.memorize_talkback_recall), icon = R.drawable.ic_headphones)
    GlassTonalButton(onClick = actions.onRecallGaps, enabled = state.hasAudio && state.segmented && state.hiddenWords.isNotEmpty()) {
        Icon(painterResource(R.drawable.ic_graphic_eq), null, Modifier.size(HahoTheme.dimens.smallIcon))
        Text(stringResource(R.string.memorize_play_with_gaps), Modifier.padding(start = HahoTheme.spacing.sm))
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

private fun FadeOrder.labelRes() = when (this) {
    FadeOrder.RANDOM -> R.string.memorize_fade_random
    FadeOrder.FROM_END -> R.string.memorize_fade_from_end
}

@Composable
private fun CheckStep(state: MemorizeUiState, actions: MemorizeActions) {
    ArabicCard(state, active = -1)
    val rated = state.rated
    if (rated == null) {
        Text(stringResource(R.string.memorize_rate_prompt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        val current = state.review?.state
        Rating.entries.forEach { rating ->
            val next = Leitner.rate(current, rating, java.time.LocalDate.now())
            val days = Leitner.INTERVALS[next.box - 1]
            GlassOutlinedButton(onClick = { actions.onRate(rating) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = HahoTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(rating.labelRes()), style = MaterialTheme.typography.titleSmall)
                    Text(
                        pluralStringResource(R.plurals.memorize_next_in_days, days, days),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
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
