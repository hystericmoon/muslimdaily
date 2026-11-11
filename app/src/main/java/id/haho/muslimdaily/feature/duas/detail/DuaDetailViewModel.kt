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
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
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
import id.haho.muslimdaily.data.review.ReviewRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.mapLatest
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
    /** All settings, for the reading sheet. */
    val settings: AppSettings = AppSettings(),
    /** Whether the dua is in the Memorize tab (My hifz). */
    val inHifz: Boolean = false,
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
    private val reviews: ReviewRepository,
    private val clock: Clock,
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
                prefs = ReadingPrefs(s.translationMode, s.showTransliteration, s.phraseLayout, s.phraseFill, s.hideFillWhilePlaying),
                settings = s,
                playback = if (playback.duaId == it.id) playback else PlaybackState(speed = s.audioSpeed, repeat = playback.repeat),
            )
        }
    }.combine(notes.note(duaId)) { st, note -> st?.copy(note = note) }
        .combine(routineRepo.routines()) { st, routines -> st?.copy(routines = routines) }
        .combine(reviews.items().mapLatest { items -> reviews.withTwins(items.map { it.duaId }.toSet()) }) { st, hifz ->
            st?.copy(inHifz = st.dua.id in hifz)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Changes from the reading sheet: the same settings as Display settings. */
    fun updateSettings(block: SettingsEditor.() -> Unit) {
        viewModelScope.launch { settings.update(block) }
    }

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

    /** Previous (-1) or next (+1) phrase: restarts the recitation there, staying in the dua. */
    fun skipPhrase(delta: Int, title: String) {
        val s = state.value ?: return
        val last = s.dua.phrases.lastIndex
        if (last < 0) return
        val current = s.playback.activePhrase.coerceAtLeast(0)
        playFrom((current + delta).coerceIn(0, last), title)
    }

    /** Adds the dua to the Memorize tab. It never starts a session. */
    fun addToHifz() {
        viewModelScope.launch { reviews.add(duaId, LocalDate.now(clock)) }
    }

    /** Takes the dua out of the Memorize tab, with its review schedule (the page asks first). */
    fun removeFromHifz() {
        viewModelScope.launch { reviews.remove(duaId) }
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
