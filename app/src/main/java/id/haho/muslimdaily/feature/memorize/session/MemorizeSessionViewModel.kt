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
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.data.settings.PauseMode
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.memorize.FadeLadder
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.MemorizePlan
import id.haho.muslimdaily.domain.memorize.Rating
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.domain.memorize.Word
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.navigation.MemorizeSessionRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    val fadeOrder: FadeOrder = FadeOrder.RANDOM,
    val firstLetters: Boolean = false,
    /** The word shown last by "reveal as you go", announced to TalkBack. */
    val lastRevealed: Word? = null,
    /** How the last recital moved the hints, until the next one or a manual change. */
    val attempt: FadeLadder.Outcome? = null,
    val reading: ReadingPrefs = ReadingPrefs(),
    /** 1-based place of this dua among its event's duas, and how many there are (e.g. 3 of 19). */
    val position: Int = 0,
    val positionOf: Int = 0,
    /** Words the learner marked in Check as missed or wrong. */
    val missedWords: Set<Int> = emptySet(),
    /** Recall hides only the missed words instead of the fade level's words. */
    val practising: Boolean = false,
    /** After a recital of the missed words: how many still had to be revealed. */
    val practiceLeft: Int? = null,
) {
    val hasAudio: Boolean get() = dua.audio != null
    val segmented: Boolean get() = dua.isSegmented
    val stage: Int get() = FadeLadder.stageOf(fade, firstLetters)
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

    // A review starts with recall (retrieval before restudy): the whole text hidden behind the cue card.
    private val local = MutableStateFlow(
        if (route.fromReview) Local(step = Step.RECALL, fade = FadeLevel.FULL) else Local(),
    )

    private data class Local(
        val step: Step = Step.LISTEN,
        val playing: Playing = Playing.NONE,
        val fade: FadeLevel = FadeLevel.LIGHT,
        val order: FadeOrder = FadeOrder.RANDOM,
        val firstLetters: Boolean = false,
        val revealed: List<Int> = emptyList(),
        val attempt: FadeLadder.Outcome? = null,
        val rated: LeitnerState? = null,
        val nextDueId: String? = null,
        val ab: IntRange? = null,
        val missed: Set<Int> = emptySet(),
        val practice: Set<Int>? = null,
        val practiceLeft: Int? = null,
    )

    private val today: LocalDate get() = LocalDate.now(clock)

    /** The ids of the dua's event, in reading order, for the "3 of 19" cue. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val siblings = content.dua(duaId)
        .map { it?.event }
        .distinctUntilChanged()
        .flatMapLatest { event -> if (event == null) flowOf(emptyList()) else content.duasForEvent(event).map { list -> list.filter { it.isRecitable }.map { it.id } } }

    private data class Base(val dua: Dua?, val review: ReviewItem?, val settings: AppSettings, val siblings: List<String>)

    private val base = combine(content.dua(duaId), reviews.item(duaId), settingsRepository.settings, siblings, ::Base)

    val state: StateFlow<MemorizeUiState?> = combine(base, local, audio.state) { (dua, review, settings, siblings), l, playback ->
        dua ?: return@combine null
        val memorize = settings.memorize
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
            hiddenWords = (l.practice ?: Recall.hiddenWords(words, l.fade, dua.id, l.order)) - l.revealed.toSet(),
            fadeOrder = l.order,
            firstLetters = l.firstLetters,
            lastRevealed = l.revealed.lastOrNull()?.let { words.getOrNull(it) },
            attempt = l.attempt,
            // Transliteration would give the Arabic away on the cue card.
            reading = ReadingPrefs(settings.translationMode, transliteration = false),
            review = review,
            rated = l.rated,
            fromReview = route.fromReview,
            nextDueId = l.nextDueId,
            ab = l.ab ?: (0..(dua.phrases.lastIndex.coerceAtLeast(0))),
            position = siblings.indexOf(dua.id) + 1,
            positionOf = siblings.size,
            missedWords = l.missed,
            practising = l.practice != null,
            practiceLeft = l.practiceLeft,
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
        local.value = local.value.copy(fade = level, revealed = emptyList(), attempt = null, practice = null, practiceLeft = null)
    }

    fun setFadeOrder(order: FadeOrder) {
        local.value = local.value.copy(order = order, revealed = emptyList(), attempt = null, practice = null, practiceLeft = null)
    }

    fun setFirstLetters(on: Boolean) {
        local.value = local.value.copy(firstLetters = on, attempt = null, practice = null, practiceLeft = null)
    }

    /** The learner finished reciting: the words they had to reveal decide how far the hints fade. */
    fun finishRecital() {
        val s = state.value ?: return
        if (local.value.practice != null) return finishPractice()
        val revealed = local.value.revealed.size
        val outcome = FadeLadder.outcome(s.stage, revealed, s.hiddenWords.size + revealed)
        val stage = FadeLadder.STAGES[FadeLadder.next(s.stage, outcome)]
        audio.stop()
        local.value = local.value.copy(
            fade = stage.fade,
            firstLetters = stage.firstLetters,
            revealed = emptyList(),
            attempt = outcome,
            playing = Playing.NONE,
        )
    }

    /** A recital of the missed words: the ones revealed stay missed and hidden; none left ends the practice. */
    private fun finishPractice() {
        val left = local.value.revealed.toSet()
        audio.stop()
        local.value = local.value.copy(
            practice = left.ifEmpty { null },
            missed = left,
            practiceLeft = left.size,
            revealed = emptyList(),
            attempt = null,
            playing = Playing.NONE,
        )
    }

    fun toggleMissed(index: Int) {
        val missed = local.value.missed
        local.value = local.value.copy(missed = if (index in missed) missed - index else missed + index)
    }

    /** Back to Recall with only the missed words hidden. */
    fun practiseMissed() {
        val missed = local.value.missed
        if (missed.isEmpty()) return
        audio.stop()
        local.value = local.value.copy(
            step = Step.RECALL,
            playing = Playing.NONE,
            practice = missed,
            practiceLeft = null,
            revealed = emptyList(),
            attempt = null,
        )
    }

    fun revealNext() {
        val s = state.value ?: return
        val next = Recall.nextHidden(s.words, s.hiddenWords) ?: return
        local.value = local.value.copy(revealed = local.value.revealed + next.index)
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
