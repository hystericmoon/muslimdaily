package id.haho.muslimdaily.feature.memorize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import id.haho.design.component.InfoNote
import id.haho.design.theme.HahoTheme
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.memorize.MemorizePlan
import id.haho.muslimdaily.audio.SequenceItem
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.PhraseFill
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.feature.duas.components.PhraseLines
import id.haho.muslimdaily.feature.duas.components.PlayerControls
import id.haho.muslimdaily.feature.duas.components.SPEEDS
import id.haho.muslimdaily.feature.settings.ReadingSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import id.haho.muslimdaily.navigation.ListenOnlyRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListenOnlyState(
    val dua: Dua,
    val playback: PlaybackState,
    val settings: AppSettings = AppSettings(),
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
)

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

    /** The queue that was started, so previous and next can jump between its clips. */
    private var queue: List<SequenceItem> = emptyList()

    val state: StateFlow<ListenOnlyState?> = combine(content.dua(duaId), audio.state, settings.settings) { d, p, s ->
        d?.let {
            val playback = if (p.duaId == it.id) p else PlaybackState()
            val clips = clipIndexes()
            val at = playback.sequenceIndex
            ListenOnlyState(
                it, playback, s,
                canPrevious = clips.isNotEmpty() && at > clips.first(),
                canNext = clips.any { i -> i > at },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun clipIndexes() = queue.indices.filter { queue[it] is SequenceItem.Clip }

    fun start() {
        viewModelScope.launch {
            val dua = content.duaOnce(duaId) ?: return@launch
            val m = settings.current().memorize
            val label = content.eventOnce(dua.event)?.label.orEmpty()
            queue = MemorizePlan.listen(dua, label) + MemorizePlan.build(dua, m, label)
            audio.playSequence(dua, queue, m.speed)
        }
    }

    fun toggle() = audio.toggle()

    /** Previous (-1) restarts the clip being played, or the one before it; next (+1) starts the next clip. A pause counts as part of the clip before it. */
    fun skip(delta: Int) {
        val at = audio.state.value.sequenceIndex
        val clips = clipIndexes()
        val target = if (delta > 0) clips.firstOrNull { it > at } else clips.lastOrNull { it < at } ?: clips.firstOrNull()
        target?.let(audio::skipTo)
    }

    /** Loops the whole queue. */
    fun setRepeat(on: Boolean) = audio.setRepeat(on)

    fun setSpeed(speed: Float) {
        val s = speed.coerceIn(0.5f, 1f)
        viewModelScope.launch { settings.update { memSpeed = s } }
        audio.setSpeed(s)
    }

    fun updateReading(block: SettingsEditor.() -> Unit) {
        viewModelScope.launch { settings.update(block) }
    }

    override fun onCleared() {
        audio.stop()
        audio.setRepeat(false)
    }
}

@Composable
fun ListenOnlyRoute(onClose: () -> Unit, viewModel: ListenOnlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    ListenOnlyContent(
        s,
        onStart = viewModel::start,
        onToggle = viewModel::toggle,
        onClose = onClose,
        onSkip = viewModel::skip,
        onRepeat = viewModel::setRepeat,
        onSpeed = viewModel::setSpeed,
        onUpdateReading = viewModel::updateReading,
    )
}

private val LISTEN_ONLY_SPEEDS = SPEEDS.filter { it <= 1f }

@Composable
fun ListenOnlyContent(
    s: ListenOnlyState,
    onStart: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onSkip: (Int) -> Unit = {},
    onRepeat: (Boolean) -> Unit = {},
    onSpeed: (Float) -> Unit = {},
    onUpdateReading: (SettingsEditor.() -> Unit) -> Unit = {},
) {
    var readingSheet by rememberSaveable { mutableStateOf(false) }
    if (readingSheet) ReadingSheet(s.settings, onUpdateReading) { readingSheet = false }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(HahoTheme.dimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(HahoTheme.spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close)) }
            Text(
                stringResource(R.string.memorize_listen_only),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // The same Reading display as every other dua screen: layout, progress line, translation, text size.
            IconButton(onClick = { readingSheet = true }) {
                Icon(painterResource(R.drawable.ic_text_fields), stringResource(R.string.dua_reading_settings))
            }
        }
        // A long dua scrolls in the space between the title and the controls. Before, it took all the height,
        // the controls column was squeezed to nothing and the play button drew as a clipped pill over the note.
        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            val fill = if (s.playback.isPlaying && s.settings.hideFillWhilePlaying) PhraseFill.NONE else s.settings.phraseFill
            // The bar below skips parts of the queue, not phrases, so focus mode shows the phrase alone, no Back and Next.
            PhraseLines(
                s.dua.arabic, s.dua.phrases, s.playback.activePhrase, AdwamTheme.arabic.large(s.dua.isQuran),
                layout = s.settings.phraseLayout, fill = fill, focusNav = false,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            val loaded = s.playback.sequenceIndex >= 0 && !s.playback.ended
            // Whose turn it is, spoken to TalkBack as it changes. Nothing before the start: the play button is the cue.
            val status = when {
                s.playback.ended -> stringResource(R.string.memorize_listen_done)
                s.playback.inPause -> stringResource(R.string.memorize_your_turn)
                loaded -> stringResource(R.string.memorize_listen_now)
                else -> null
            }
            if (status != null) {
                Text(
                    status,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = HahoTheme.spacing.sm).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PlayerControls(
                isPlaying = s.playback.isPlaying,
                speed = s.settings.memorize.speed,
                repeat = s.playback.repeat,
                enabled = s.dua.audio != null,
                onToggle = { if (loaded) onToggle() else onStart() },
                onSpeed = onSpeed,
                onRepeat = onRepeat,
                onPreviousPhrase = { onSkip(-1) },
                onNextPhrase = { onSkip(1) },
                canPrevious = s.canPrevious,
                canNext = s.canNext,
                phraseCount = s.dua.phrases.size,
                activePhrase = s.playback.activePhrase,
                speeds = LISTEN_ONLY_SPEEDS,
                showTrack = s.settings.showPhraseTrack,
                trackSeekable = false,
                previousLabel = stringResource(R.string.player_previous_part),
                nextLabel = stringResource(R.string.player_next_part),
            )
        }
        InfoNote(stringResource(R.string.memorize_listen_only_note), icon = R.drawable.ic_mobile_off)
    }
}
