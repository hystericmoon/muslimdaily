package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.audio.AudioEngine
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.memorize.MemorizePlan
import id.haho.muslimdaily.feature.duas.components.HighlightedArabic
import id.haho.muslimdaily.navigation.ListenOnlyRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListenOnlyState(val dua: Dua, val playback: PlaybackState)

/**
 * Hands-free memorisation: Listen ×2 followed by the whole Build sequence in one queue, played by
 * the media session so it continues with the screen off and responds to lock-screen and headphone
 * controls.
 */
@HiltViewModel
class ListenOnlyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val settings: SettingsRepository,
    private val audio: AudioEngine,
) : ViewModel() {
    private val duaId = savedState.toRoute<ListenOnlyRoute>().duaId

    val state: StateFlow<ListenOnlyState?> = combine(content.dua(duaId), audio.state) { d, p ->
        d?.let { ListenOnlyState(it, if (p.duaId == it.id) p else PlaybackState()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun start() {
        viewModelScope.launch {
            val dua = content.duaOnce(duaId) ?: return@launch
            val m = settings.current().memorize
            val label = content.eventOnce(dua.event)?.label.orEmpty()
            val items = MemorizePlan.listen(dua, label) + MemorizePlan.build(dua, m, label)
            audio.playSequence(dua, items, m.speed)
        }
    }

    fun toggle() = audio.toggle()

    override fun onCleared() {
        audio.stop()
    }
}

@Composable
fun ListenOnlyRoute(onClose: () -> Unit, viewModel: ListenOnlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(AppTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close)) }
            Text(stringResource(R.string.memorize_listen_only), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        HighlightedArabic(s.dua.arabic, s.dua.phrases, s.playback.activePhrase, AppTheme.arabic.large(s.dua.isQuran))
        Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val loaded = s.playback.sequenceIndex >= 0 && !s.playback.ended
            FilledIconButton(
                onClick = { if (loaded) viewModel.toggle() else viewModel.start() },
                enabled = s.dua.audio != null,
                modifier = Modifier.size(AppTheme.dimens.playerMainButton * 1.5f),
            ) {
                Icon(
                    painterResource(if (s.playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                    stringResource(if (s.playback.isPlaying) R.string.player_pause else R.string.player_play),
                    Modifier.size(AppTheme.dimens.icon * 2),
                )
            }
            Text(
                when {
                    s.playback.ended -> stringResource(R.string.memorize_listen_done)
                    s.playback.inPause -> stringResource(R.string.memorize_your_turn)
                    loaded -> stringResource(R.string.memorize_listen_now)
                    else -> stringResource(R.string.memorize_listen_only_start)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = AppTheme.spacing.lg).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        InfoNote(stringResource(R.string.memorize_listen_only_note), icon = R.drawable.ic_mobile_off)
    }
}
