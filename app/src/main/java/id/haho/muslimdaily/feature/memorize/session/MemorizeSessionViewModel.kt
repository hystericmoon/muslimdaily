package id.haho.muslimdaily.feature.memorize.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.audio.AudioEngine
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PauseMode
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.MemorizePlan
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.domain.memorize.Word
import id.haho.muslimdaily.navigation.MemorizeSessionRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class Step { LISTEN, BUILD, RECALL, CHECK }

/** Which sequence is loaded, so the screen can describe progress. */
enum class Playing { NONE, LISTEN, BUILD, AB, RECALL }

data class MemorizeUiState(
    val dua: Dua,
    val eventLabel: String,
    val step: Step,
    val playing: Playing,
    val playback: PlaybackState,
    val settings: MemorizeSettings,
    val fade: FadeLevel,
    val words: List<Word>,
    val hiddenWords: Set<Int>,
    val review: ReviewItem?,
    val rated: LeitnerState?,
    val fromReview: Boolean,
    val nextDueId: String?,
    val ab: IntRange,
) {
    val hasAudio: Boolean get() = dua.audio != null
    val segmented: Boolean get() = dua.phrases.isNotEmpty() && dua.phrases.all { it.segment != null }
}

@HiltViewModel
class MemorizeSessionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val reviews: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    private val audio: AudioEngine,
    private val clock: Clock,
) : ViewModel() {
    private val route = savedState.toRoute<MemorizeSessionRoute>()
    val duaId = route.duaId

    private val local = MutableStateFlow(Local())

    private data class Local(
        val step: Step = Step.LISTEN,
        val playing: Playing = Playing.NONE,
        val fade: FadeLevel = FadeLevel.LIGHT,
        val rated: LeitnerState? = null,
        val nextDueId: String? = null,
        val ab: IntRange? = null,
    )

    private val today: LocalDate get() = LocalDate.now(clock)

    private val base = combine(content.dua(duaId), reviews.item(duaId), settingsRepository.settings.map { it.memorize }) { d, r, m ->
        Triple(d, r, m)
    }

    val state: StateFlow<MemorizeUiState?> = combine(base, local, audio.state) { (dua, review, memorize), l, playback ->
        dua ?: return@combine null
        val words = Recall.words(dua)
        val label = content.eventOnce(dua.event)?.label.orEmpty()
        MemorizeUiState(
            dua = dua,
            eventLabel = label,
            step = l.step,
            playing = if (playback.duaId == dua.id) l.playing else Playing.NONE,
            playback = if (playback.duaId == dua.id) playback else PlaybackState(),
            settings = memorize,
            fade = l.fade,
            words = words,
            hiddenWords = Recall.hiddenWords(words, l.fade, dua.id),
            review = review,
            rated = l.rated,
            fromReview = route.fromReview,
            nextDueId = l.nextDueId,
            ab = l.ab ?: (0..(dua.phrases.lastIndex.coerceAtLeast(0))),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setStep(step: Step) {
        audio.stop()
        local.value = local.value.copy(step = step, playing = Playing.NONE)
    }

    private fun play(kind: Playing, build: (MemorizeUiState) -> List<id.haho.muslimdaily.audio.SequenceItem>) {
        val s = state.value ?: return
        val items = build(s)
        if (items.isEmpty()) return
        local.value = local.value.copy(playing = kind)
        audio.playSequence(s.dua, items, s.settings.speed)
    }

    fun playListen() = play(Playing.LISTEN) { MemorizePlan.listen(it.dua, it.eventLabel) }
    fun playBuild() = play(Playing.BUILD) { MemorizePlan.build(it.dua, it.settings, it.eventLabel) }
    fun playAb() = play(Playing.AB) { MemorizePlan.abLoop(it.dua, it.ab.first, it.ab.last, it.settings, it.eventLabel) }
    fun playRecallWithGaps() = play(Playing.RECALL) {
        MemorizePlan.recallWithGaps(it.dua, Recall.hiddenPhrases(it.words, it.hiddenWords), it.settings, it.eventLabel)
    }

    fun toggle() = audio.toggle()

    fun setAb(range: IntRange) {
        local.value = local.value.copy(ab = range)
    }

    fun setFade(level: FadeLevel) {
        local.value = local.value.copy(fade = level)
    }

    fun rate(rating: Rating) {
        viewModelScope.launch {
            audio.stop()
            val next = reviews.rate(duaId, rating, today)
            val nextDue = if (route.fromReview) reviews.due(today).firstOrNull { it != duaId } else null
            local.value = local.value.copy(rated = next, nextDueId = nextDue)
        }
    }

    fun updateSettings(loops: Int? = null, pauseMode: PauseMode? = null, pauseSeconds: Int? = null, speed: Float? = null) {
        viewModelScope.launch {
            settingsRepository.update {
                loops?.let { memLoops = it.coerceIn(1, 10) }
                pauseMode?.let { memPauseMode = it }
                pauseSeconds?.let { memPauseSeconds = it.coerceIn(1, 30) }
                speed?.let { memSpeed = it.coerceIn(0.5f, 1f) }
            }
            speed?.let { audio.setSpeed(it.coerceIn(0.5f, 1f)) }
        }
    }

    override fun onCleared() {
        audio.stop()
    }
}
