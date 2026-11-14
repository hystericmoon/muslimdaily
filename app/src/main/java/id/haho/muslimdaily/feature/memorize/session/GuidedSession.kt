package id.haho.muslimdaily.feature.memorize.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import id.haho.design.component.InfoNote
import id.haho.design.glass.GlassButton
import id.haho.design.glass.GlassCard
import id.haho.design.glass.GlassIconButton
import id.haho.design.glass.GlassOutlinedButton
import id.haho.design.glass.GlassTonalButton
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.AdwamDimens
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import java.time.LocalDate

/**
 * Guided mode: Listen, Repeat after it, Read with gaps, Test yourself, one task per screen in that
 * order, with Next to move on and no settings in view. Same engine as the Advanced tabs: the build
 * loop, the fade ladder (which sets the gaps on its own) and the review schedule. Back goes to the
 * previous step; reviews open on Test yourself.
 */
@Composable
internal fun ColumnScope.GuidedSession(state: MemorizeUiState, actions: MemorizeActions, talkBack: Boolean, meaning: Boolean, dockMax: Dp) {
    val step = state.guided
    val back = step.ordinal > 0 && !(state.fromReview && step == GuidedStep.TEST)
    BackHandler(enabled = back) { actions.onGuidedStep(GuidedStep.entries[step.ordinal - 1]) }
    GuidedProgress(step)
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(HahoTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.lg),
    ) {
        when (step) {
            GuidedStep.LISTEN -> {
                InfoNote(stringResource(R.string.memorize_hint_listen))
                ListenStep(state)
            }
            GuidedStep.REPEAT -> {
                InfoNote(stringResource(R.string.memorize_hint_build))
                BuildStep(state)
            }
            GuidedStep.GAPS -> {
                InfoNote(stringResource(R.string.memorize_hint_recall))
                RecallStep(state, talkBack, meaning)
            }
            GuidedStep.TEST -> TestStep(state, actions, meaning)
        }
    }
    val next: () -> Unit = { actions.onGuidedStep(GuidedStep.entries[step.ordinal + 1]) }
    when (step) {
        GuidedStep.LISTEN -> GuidedDock(dockMax) {
            val loaded = listenLoaded(state)
            PlayRow(state, loaded, stringResource(R.string.memorize_listen_twice), actions.onListen, actions.onToggle, next)
        }
        GuidedStep.REPEAT -> GuidedDock(dockMax) {
            val loaded = state.playing == Playing.BUILD && !state.playback.ended
            if (loaded) BuildProgress(state)
            PlayRow(state, loaded, stringResource(R.string.memorize_build_start), actions.onBuild, actions.onToggle, next)
        }
        GuidedStep.GAPS -> GuidedDock(dockMax, recallDock(state, actions) { NextButton(next, filled = false) })
        GuidedStep.TEST -> when {
            state.rated != null -> Unit
            !state.testRevealed -> GuidedDock(dockMax) {
                GlassButton(onClick = actions.onRevealTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.memorize_reveal_text))
                }
            }
            else -> GuidedDock(dockMax) { RatingButtons(state, actions) }
        }
    }
}

/** "Step 2 of 4 · Repeat after it", announced on each change; the bars are decorative. */
@Composable
private fun GuidedProgress(step: GuidedStep) {
    val n = step.ordinal + 1
    val of = GuidedStep.entries.size
    val name = stringResource(step.labelRes())
    val spoken = stringResource(R.string.memorize_guided_step_spoken, n, of, name)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = HahoTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs),
    ) {
        Text(
            stringResource(R.string.memorize_guided_step, n, of, name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                contentDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
        )
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xs)) {
            GuidedStep.entries.forEach { s ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (s.ordinal <= step.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

private fun GuidedStep.labelRes() = when (this) {
    GuidedStep.LISTEN -> R.string.memorize_listen_now
    GuidedStep.REPEAT -> R.string.memorize_guided_repeat
    GuidedStep.GAPS -> R.string.memorize_guided_gaps
    GuidedStep.TEST -> R.string.memorize_guided_test
}

/** The docked sheet without a More: Guided keeps the tuning in Advanced. */
@Composable
private fun GuidedDock(maxHeight: Dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            // Big text at 200% font: at most 60% of the screen, scrolling inside.
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HahoTheme.dimens.screenGutter)
                .padding(top = HahoTheme.spacing.md, bottom = HahoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.sm),
            content = content,
        )
    }
}

/**
 * From the start, play or pause, and Next. The play button starts the step's sequence the first
 * time. At large text the play button gets its own row so the two text buttons keep their words.
 */
@Composable
private fun PlayRow(
    state: MemorizeUiState,
    loaded: Boolean,
    startLabel: String,
    onStart: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val play: @Composable () -> Unit = {
        val playing = loaded && state.playback.isPlaying
        GlassIconButton(
            onClick = if (loaded) onToggle else onStart,
            enabled = state.hasAudio,
            modifier = Modifier.size(AdwamDimens.playerMainButton),
        ) {
            Icon(
                painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                when {
                    !loaded -> startLabel
                    playing -> stringResource(R.string.player_pause)
                    else -> stringResource(R.string.player_play)
                },
                Modifier.size(HahoTheme.dimens.icon * 1.5f),
            )
        }
    }
    val restart: @Composable () -> Unit = { if (loaded) RestartButton(onStart) }
    if (LocalDensity.current.fontScale > STACK_FONT_SCALE) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { play() }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            restart()
            Spacer(Modifier.weight(1f))
            NextButton(onNext)
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { restart() }
            play()
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { NextButton(onNext) }
        }
    }
}

@Composable
private fun NextButton(onClick: () -> Unit, filled: Boolean = true) {
    val content: @Composable () -> Unit = {
        Text(stringResource(R.string.memorize_next))
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.padding(start = HahoTheme.spacing.xs).size(HahoTheme.dimens.smallIcon))
    }
    if (filled) GlassButton(onClick = onClick) { content() } else TextButton(onClick = onClick) { content() }
}

/**
 * Recite from memory first (retrieval before restudy), then reveal, mark the missed words and rate.
 * After rating: when the dua comes back, and Practise now after Not yet.
 */
@Composable
private fun TestStep(state: MemorizeUiState, actions: MemorizeActions, meaning: Boolean) {
    val rated = state.rated
    when {
        rated != null -> {
            ArabicCard(state, active = -1)
            RatedResult(state, rated, actions, practiseNow = state.ratedWith == Rating.AGAIN)
        }
        !state.testRevealed -> {
            if (meaning) {
                GlassCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(HahoTheme.spacing.lg)) {
                        DuaTranslations(state.dua, state.reading)
                    }
                }
            }
            GlassCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.memorize_test_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(HahoTheme.spacing.xl),
                )
            }
        }
        else -> {
            if (Recall.recallable(state.words).isNotEmpty()) MarkMissed(state, actions) else ArabicCard(state, active = -1)
            Text(
                stringResource(R.string.memorize_how_did_it_go),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
        }
    }
}

/** Plain-language ratings, one to one with Easy, Hard and Again, each saying what happens next. */
@Composable
private fun RatingButtons(state: MemorizeUiState, actions: MemorizeActions) {
    val current = state.review?.state
    val today = LocalDate.now()
    listOf(Rating.EASY, Rating.HARD, Rating.AGAIN).forEach { rating ->
        val days = Leitner.INTERVALS[Leitner.rate(current, rating, today).box - 1]
        val content: @Composable () -> Unit = {
            Column(Modifier.padding(vertical = HahoTheme.spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(rating.guidedRes()), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (rating == Rating.AGAIN) {
                        stringResource(R.string.memorize_practise_now)
                    } else {
                        pluralStringResource(R.plurals.memorize_next_in_days, days, days)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        val onClick = { actions.onRate(rating) }
        val m = Modifier.fillMaxWidth()
        when (rating) {
            Rating.EASY -> GlassButton(onClick = onClick, modifier = m) { content() }
            Rating.HARD -> GlassTonalButton(onClick = onClick, modifier = m) { content() }
            Rating.AGAIN -> GlassOutlinedButton(onClick = onClick, modifier = m) { content() }
        }
    }
}

private fun Rating.guidedRes() = when (this) {
    Rating.EASY -> R.string.memorize_knew_it
    Rating.HARD -> R.string.memorize_with_help
    Rating.AGAIN -> R.string.memorize_not_yet
}
