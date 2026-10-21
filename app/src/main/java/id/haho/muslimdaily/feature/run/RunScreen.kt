package id.haho.muslimdaily.feature.run

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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import kotlinx.coroutines.launch

data class RunActions(
    val onTap: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onPage: (Int) -> Unit = {},
    val onRestart: () -> Unit = {},
    val onAutoAdvance: (Boolean) -> Unit = {},
    val onClose: () -> Unit = {},
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
                onClose = onClose,
            ),
        )
    }
}

@Composable
fun RunScreen(state: RunUiState, actions: RunActions) {
    val pageCount = state.items.size + 1
    val pager = rememberPagerState(initialPage = state.index) { pageCount }
    val motion = AppTheme.motion

    // ViewModel → pager (taps, auto-advance) with spring motion; pager → ViewModel on swipe.
    LaunchedEffect(state.index) {
        if (pager.currentPage != state.index) pager.animateScrollToPage(state.index, animationSpec = motion.cardAdvance())
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { actions.onPage(it) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        RunTopBar(state, pager, actions)
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppTheme.dimens.screenGutter),
            pageSpacing = AppTheme.spacing.md,
            key = { if (it < state.items.size) state.items[it].duaId + it else "summary" },
        ) { page ->
            if (page < state.items.size) {
                RunCard(state, page, actions)
            } else {
                SummaryPage(state, actions)
            }
        }
        RunBottomBar(state, pager, actions)
    }
}

@Composable
private fun RunTopBar(state: RunUiState, pager: PagerState, actions: RunActions) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.xs),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
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
    val tapLabel = stringResource(if (item.target != null) R.string.run_tap_count else R.string.run_tap_read)

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = AppTheme.spacing.sm)
            .clickable(enabled = isCurrent && !done, onClickLabel = tapLabel, role = Role.Button, onClick = actions.onTap)
            .semantics { stateDescription = stateText },
    ) {
        Column(Modifier.fillMaxSize().padding(AppTheme.spacing.xl)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                ArabicText(dua.arabic, style = AppTheme.arabic.large(dua.isQuran))
                DuaTranslations(dua, state.prefs)
            }
            CountArea(done = done, countText = countText, isCurrent = isCurrent, count = count)
        }
    }
}

/** Count display with a spring "bump" on each tap; a gold disc with a dark check when complete. */
@Composable
private fun CountArea(done: Boolean, countText: String?, isCurrent: Boolean, count: Int) {
    val scale = remember { Animatable(1f) }
    val motion = AppTheme.motion
    LaunchedEffect(count) {
        if (count > 0 && !motion.reduced) {
            scale.snapTo(1.15f)
            scale.animateTo(1f, motion.countTap())
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(top = AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        if (done) {
            Surface(
                shape = CircleShape,
                color = AppTheme.colors.completeFill,
                contentColor = AppTheme.colors.onCompleteFill,
                modifier = Modifier.size(AppTheme.dimens.completeDisc),
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.padding(AppTheme.spacing.md))
            }
            Text(stringResource(R.string.run_done), style = MaterialTheme.typography.titleMedium)
        } else if (countText != null) {
            Text(
                countText,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .then(if (isCurrent) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
            )
            Text(stringResource(R.string.run_tap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Spacer(Modifier.height(AppTheme.spacing.sm))
            Text(stringResource(R.string.run_tap_read_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RunBottomBar(state: RunUiState, pager: PagerState, actions: RunActions) {
    val scope = rememberCoroutineScope()
    val onCard = pager.currentPage < state.items.size
    Row(
        Modifier.fillMaxWidth().heightIn(min = AppTheme.dimens.largeTouchTarget).padding(horizontal = AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            enabled = pager.currentPage > 0,
            modifier = Modifier.size(AppTheme.dimens.largeTouchTarget),
        ) {
            Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.run_previous))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (onCard && state.counts.getOrElse(pager.currentPage) { 0 } > 0) {
                TextButton(onClick = actions.onUndo) {
                    Icon(painterResource(R.drawable.ic_remove), null, Modifier.size(AppTheme.dimens.smallIcon))
                    Text(stringResource(R.string.run_undo), Modifier.padding(start = AppTheme.spacing.xs), maxLines = 1)
                }
            }
        }
        IconButton(
            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            enabled = pager.currentPage < state.items.size,
            modifier = Modifier.size(AppTheme.dimens.largeTouchTarget),
        ) {
            Icon(painterResource(R.drawable.ic_skip_next), stringResource(R.string.run_next))
        }
    }
}

@Composable
private fun SummaryPage(state: RunUiState, actions: RunActions) {
    val all = state.done == state.items.size
    Column(
        Modifier.fillMaxSize().padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg, Alignment.CenterVertically),
    ) {
        Surface(
            shape = CircleShape,
            color = if (all) AppTheme.colors.completeFill else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (all) AppTheme.colors.onCompleteFill else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.completeDisc * 1.5f),
        ) {
            Icon(painterResource(if (all) R.drawable.ic_done_all else R.drawable.ic_format_list_numbered), null, Modifier.padding(AppTheme.spacing.lg))
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
        Button(onClick = actions.onClose) { Text(stringResource(R.string.action_done)) }
        if (!all) {
            val firstOpen = state.items.indices.firstOrNull { !state.isDone(it) }
            if (firstOpen != null) {
                OutlinedButton(onClick = { actions.onPage(firstOpen) }) { Text(stringResource(R.string.run_go_to_open)) }
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
