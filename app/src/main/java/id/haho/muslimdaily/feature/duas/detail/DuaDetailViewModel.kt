package id.haho.muslimdaily.feature.duas.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.audio.AudioEngine
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.navigation.DuaDetailRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import id.haho.muslimdaily.data.note.NoteRepository
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.routine.RoutineRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuaDetailState(
    val dua: Dua,
    val event: DuaEvent?,
    val prefs: ReadingPrefs,
    val playback: PlaybackState,
    /** The reader's own note, if any. */
    val note: String? = null,
    /** The reader's routines, for "add to routine". */
    val routines: List<Routine> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DuaDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val settings: SettingsRepository,
    private val audio: AudioEngine,
    private val notes: NoteRepository,
    private val routineRepo: RoutineRepository,
) : ViewModel() {
    val duaId: String = savedState.toRoute<DuaDetailRoute>().duaId

    private val dua: Flow<Dua?> = content.dua(duaId)

    private val event: Flow<DuaEvent?> = dua.flatMapLatest { d -> d?.let { content.event(it.event) } ?: flowOf(null) }

    val state: StateFlow<DuaDetailState?> = combine(
        dua,
        event,
        settings.settings,
        audio.state,
    ) { d, e, s, playback ->
        d?.let {
            DuaDetailState(
                dua = it,
                event = e,
                prefs = ReadingPrefs(s.translationMode, s.showTransliteration),
                playback = if (playback.duaId == it.id) playback else PlaybackState(speed = s.audioSpeed, repeat = playback.repeat),
            )
        }
    }.combine(notes.note(duaId)) { st, note -> st?.copy(note = note) }
        .combine(routineRepo.routines()) { st, routines -> st?.copy(routines = routines) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveNote(text: String) {
        viewModelScope.launch { notes.save(duaId, text) }
    }

    fun togglePlay(title: String) {
        val s = state.value ?: return
        if (audio.state.value.duaId == s.dua.id && !audio.state.value.ended) {
            audio.toggle()
        } else {
            audio.playDua(s.dua, s.playback.speed, title = title)
        }
    }

    fun playFrom(phrase: Int, title: String) {
        val s = state.value ?: return
        audio.playDua(s.dua, s.playback.speed, fromPhrase = phrase, title = title)
    }

    fun setSpeed(speed: Float) {
        audio.setSpeed(speed)
        viewModelScope.launch { settings.update { audioSpeed = speed } }
    }

    fun setRepeat(repeat: Boolean) = audio.setRepeat(repeat)

    fun setInRoutine(routineId: String, included: Boolean) {
        viewModelScope.launch { routineRepo.setIncluded(routineId, duaId, included) }
    }

    suspend fun contentVersion(): Int = content.contentVersion()

    /** The palette the share card is drawn in. */
    suspend fun palette() = settings.current().palette
    suspend fun arabicFont() = settings.current().arabicFont
}
