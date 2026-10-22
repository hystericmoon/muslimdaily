package id.haho.muslimdaily.feature.duas.detail

import id.haho.muslimdaily.core.designsystem.glass.GlassOutlinedButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.feature.duas.components.gradeLabel
import id.haho.muslimdaily.feature.duas.components.sourceLabel
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.ui.reportError
import id.haho.muslimdaily.feature.duas.components.CountBadge
import id.haho.muslimdaily.feature.duas.components.DuaTranslations
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.feature.duas.components.PlayerBar
import id.haho.muslimdaily.feature.duas.components.SourceChip
import kotlinx.coroutines.launch

data class DuaDetailActions(
    val onTogglePlay: () -> Unit = {},
    val onSpeed: (Float) -> Unit = {},
    val onRepeat: (Boolean) -> Unit = {},
    val onMemorize: () -> Unit = {},
    val onSetInMySet: (Boolean) -> Unit = {},
)

@Composable
fun DuaDetailRoute(
    onBack: () -> Unit,
    onMemorize: (String) -> Unit,
    viewModel: DuaDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = state
    val title = s?.event?.label.orEmpty()
    AppScreen(
        title = title,
        onBack = onBack,
        actions = {
            if (s != null) {
                val source = buildString {
                    append(s.dua.sources.joinToString("; ") { sourceLabel(it) })
                    if (s.dua.grading != null) append(" · ").append(gradeLabel(s.dua))
                }
                IconButton(onClick = { scope.launch { DuaShareCard.share(context, s.dua, title, source, viewModel.palette()) } }) {
                    Icon(painterResource(R.drawable.ic_share), stringResource(R.string.share_dua))
                }
            }
            IconButton(onClick = { scope.launch { reportError(context, viewModel.duaId, viewModel.contentVersion()) } }) {
                Icon(painterResource(R.drawable.ic_flag), stringResource(R.string.settings_report))
            }
        },
        bottomBar = {
            if (s != null) {
                PlayerBar(
                    isPlaying = s.playback.isPlaying,
                    speed = s.playback.speed,
                    repeat = s.playback.repeat,
                    enabled = s.dua.audio != null,
                    onToggle = { viewModel.togglePlay(title) },
                    onSpeed = viewModel::setSpeed,
                    onRepeat = viewModel::setRepeat,
                    modifier = Modifier
                        .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm)
                        .navigationBarsPadding(),
                )
            }
        },
    ) {
        if (s != null) {
            DuaDetailContent(
                state = s,
                actions = DuaDetailActions(
                    onMemorize = { onMemorize(s.dua.id) },
                    onSetInMySet = viewModel::setInMySet,
                ),
            )
        }
    }
}

@Composable
fun DuaDetailContent(state: DuaDetailState, actions: DuaDetailActions) {
    val dua = state.dua
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.dimens.screenGutter)
            .padding(bottom = AppTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        GlassCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(AppTheme.spacing.xl)) {
                HighlightedArabic(
                    text = dua.arabic,
                    phrases = dua.phrases,
                    activePhrase = state.playback.activePhrase,
                    style = AppTheme.arabic.large(dua.isQuran),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            dua.count?.let { CountBadge(it, Modifier.align(Alignment.CenterVertically)) }
            SourceChip(dua)
        }
        DuaTranslations(dua, state.prefs)
        dua.virtue?.let { virtue ->
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(
                    stringResource(R.string.dua_virtue),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(virtue.text, style = AppTheme.reading.bodyLarge)
                if (virtue.source.isNotBlank()) {
                    Text(virtue.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (dua.audio == null) InfoNote(stringResource(R.string.dua_no_audio), icon = R.drawable.ic_volume_off)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            GlassOutlinedButton(onClick = actions.onMemorize) {
                Icon(painterResource(R.drawable.ic_record_voice_over), null, Modifier.size(AppTheme.dimens.smallIcon))
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(R.string.dua_memorize))
            }
            GlassOutlinedButton(onClick = { actions.onSetInMySet(!state.inMySet) }) {
                Icon(
                    painterResource(if (state.inMySet) R.drawable.ic_bookmark_added_fill else R.drawable.ic_bookmark_add),
                    null,
                    Modifier.size(AppTheme.dimens.smallIcon),
                )
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(stringResource(if (state.inMySet) R.string.dua_remove_from_set else R.string.dua_add_to_set))
            }
        }
        InfoNote(
            stringResource(if (state.inMySet) R.string.dua_in_set_note else R.string.dua_not_in_set_note),
            Modifier.fillMaxWidth(),
        )
    }
}
