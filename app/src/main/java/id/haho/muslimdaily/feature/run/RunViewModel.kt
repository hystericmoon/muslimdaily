package id.haho.muslimdaily.feature.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.data.run.RunSession
import id.haho.muslimdaily.data.run.RunSessionRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.navigation.RunRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class RunUiState(
    val eventLabel: String,
    val duas: List<Dua>,
    val items: List<RunItem>,
    val counts: List<Int>,
    /** Current page; equals items.size on the summary page. */
    val index: Int,
    val completed: Boolean,
    val prefs: ReadingPrefs,
    val autoAdvance: Boolean,
    val haptics: Boolean,
    /** Duas in "My hifz"; with [fromMemory] their Arabic stays hidden until the page is revealed. */
    val hifz: Set<String> = emptySet(),
    /** Every dua in "My hifz", including those saved to learn later (not hidden: not learnt yet). */
    val saved: Set<String> = emptySet(),
    val fromMemory: Boolean = false,
    /** Pages whose text was shown this session; not stored. */
    val revealed: Set<Int> = emptySet(),
    /** Pages recited from memory and answered "knew it / needed the text", with the next review date. */
    val recalled: Map<Int, LocalDate> = emptyMap(),
) {
    /** Recited from memory and checked, but not yet answered. */
    fun asksRecall(page: Int): Boolean =
        fromMemory && page in revealed && page !in recalled && duas.getOrNull(page)?.id?.let { it in hifz } == true

    fun isHidden(page: Int): Boolean =
        fromMemory && page !in revealed && duas.getOrNull(page)?.id?.let { it in hifz } == true

    val done: Int get() = items.indices.count { isDone(it) }
    fun isDone(i: Int): Boolean = counts.getOrElse(i) { 0 } >= items[i].taps
}

/** One-off feedback for the screen (haptics); never stored. */
enum class RunEffect { Tick, ItemComplete, SessionComplete }

/** Pure counting rules shared by the ViewModel and the UI tests. */
object RunLogic {
    /** Counts one tap on the current card. Returns the new state and the feedback to give. */
    fun tap(s: RunUiState): Pair<RunUiState, RunEffect?> {
        val i = s.index
        if (i !in s.items.indices || s.isDone(i)) return s to null
        val next = s.copy(counts = s.counts.toMutableList().also { it[i] = it[i] + 1 })
        if (!next.isDone(i)) return next to RunEffect.Tick
        val allDone = next.items.indices.all { next.isDone(it) }
        return if (allDone) next.copy(completed = true) to RunEffect.SessionComplete else next to RunEffect.ItemComplete
    }

    fun undo(s: RunUiState): RunUiState {
        val i = s.index
        if (i !in s.items.indices || s.counts[i] == 0) return s
        return s.copy(counts = s.counts.toMutableList().also { it[i] = it[i] - 1 })
    }

    fun goTo(s: RunUiState, index: Int): RunUiState = s.copy(index = index.coerceIn(0, s.items.size))
}

/**
 * Run mode: one card at a time through the event's active set. Counts are worship counts and are
 * stored only to resume an interrupted session today; they are never aggregated or shared.
 */
@HiltViewModel
class RunViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val content: ContentRepository,
    private val mySets: MySetRepository,
    private val sessions: RunSessionRepository,
    private val settings: SettingsRepository,
    private val routines: RoutineRepository,
    private val reviews: ReviewRepository,
    private val clock: Clock,
) : ViewModel() {
    /** An occasion id, or a routine's run key ("routine:<id>"). */
    val eventId: String = savedState.toRoute<RunRoute>().eventId
    private val routineId: String? = Routine.idFromRunKey(eventId)

    private val _state = MutableStateFlow<RunUiState?>(null)
    val state: StateFlow<RunUiState?> = _state.asStateFlow()

    private val _effects = Channel<RunEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var advanceJob: Job? = null
    private val today: LocalDate get() = LocalDate.now(clock)

    init {
        viewModelScope.launch {
            val s = settings.current()
            val label = routineId?.let { routines.routine(it)?.name } ?: content.eventOnce(eventId)?.label.orEmpty()
            val prefs = ReadingPrefs(s.translationMode, s.showTransliteration)
            // An edited routine starts afresh rather than resuming a list it no longer has.
            val routineIds = routineId?.let { routines.routine(it)?.duaIds }
            val resumed = sessions.load(eventId, today)?.takeIf { session ->
                routineIds == null || session.items.map { it.duaId } == routineIds
            }?.let { session ->
                val duas = content.duas(session.items.map { it.duaId }).filter { it.isRecitable }
                if (duas.size == session.items.size) session to duas else null
            }
            val (session, duas) = resumed ?: newSession()
            val hifz = reviews.items().first()
            _state.value = RunUiState(
                eventLabel = label,
                duas = duas,
                items = session.items,
                counts = session.counts,
                index = session.index.coerceIn(0, session.items.size),
                completed = session.completed,
                prefs = prefs,
                autoAdvance = s.autoAdvance,
                haptics = s.haptics,
                hifz = reviews.withTwins(hifz.filterNot { it.notStarted }.map { it.duaId }.toSet()),
                saved = reviews.withTwins(hifz.map { it.duaId }.toSet()),
                fromMemory = s.runFromMemory,
            )
            launch {
                settings.settings.collect { st ->
                    _state.update { it?.copy(autoAdvance = st.autoAdvance, haptics = st.haptics, fromMemory = st.runFromMemory) }
                }
            }
        }
    }

    private suspend fun newSession(): Pair<RunSession, List<Dua>> {
        val duas = if (routineId != null) {
            // The routine's own order; a dua removed from the content since is skipped.
            val ids = routines.routine(routineId)?.duaIds.orEmpty()
            val byId = content.duas(ids).filter { it.isRecitable }.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        } else {
            ActiveSet.active(ActiveSet.canonical(content.duasForEventOnce(eventId).filter { it.isRecitable }), mySets.customizationOnce(eventId))
        }
        val session = RunSession(
            eventId = eventId,
            day = today,
            items = duas.map { RunItem(it.id, it.count) },
            counts = List(duas.size) { 0 },
            index = 0,
            completed = false,
        )
        sessions.save(session)
        return session to duas
    }

    /** The whole card is the count target. */
    fun tap() {
        val s = _state.value ?: return
        val (next, effect) = RunLogic.tap(s)
        if (effect == null) return
        _state.value = next
        _effects.trySend(effect)
        if (effect == RunEffect.SessionComplete && !s.completed && routineId == null) {
            viewModelScope.launch { mySets.advanceRotation(eventId) }
        }
        if (effect != RunEffect.Tick && next.autoAdvance) scheduleAdvance(s.index)
        persist()
    }

    fun undo() {
        val s = _state.value ?: return
        advanceJob?.cancel()
        _state.value = RunLogic.undo(s)
        persist()
    }

    fun goTo(index: Int) {
        val s = _state.value ?: return
        advanceJob?.cancel()
        if (index == s.index) return
        _state.value = RunLogic.goTo(s, index)
        persist()
    }

    fun restart() {
        viewModelScope.launch {
            sessions.clear(eventId)
            val (session, duas) = newSession()
            _state.update { it?.copy(duas = duas, items = session.items, counts = session.counts, index = 0, completed = false) }
        }
    }

    /** Shows the hidden text of a dua recited from memory. Changes no count and no review. */
    fun reveal(page: Int) {
        _state.update { it?.copy(revealed = it.revealed + page) }
    }

    /**
     * After checking a dua recited from memory: knowing it counts as a review when the dua is due,
     * needing the text sends it back to box 1. Changes no count.
     */
    fun recall(page: Int, knew: Boolean) {
        val id = _state.value?.duas?.getOrNull(page)?.id ?: return
        viewModelScope.launch {
            val current = reviews.item(id).first()?.state ?: return@launch
            val rating = Leitner.ratingFromRoutine(current, knew, today)
            val next = if (rating != null) reviews.rate(id, rating, today) else current
            _state.update { it?.copy(recalled = it.recalled + (page to next.due)) }
        }
    }

    /** Saves the dua on [page] to "My hifz" to learn later. Changes no count. */
    fun addToHifz(page: Int) {
        val id = _state.value?.duas?.getOrNull(page)?.id ?: return
        viewModelScope.launch {
            reviews.add(id, today)
            val twins = reviews.withTwins(setOf(id))
            _state.update { it?.copy(saved = it.saved + twins) }
        }
    }

    fun setFromMemory(enabled: Boolean) {
        viewModelScope.launch { settings.update { runFromMemory = enabled } }
    }

    fun setAutoAdvance(enabled: Boolean) {
        viewModelScope.launch { settings.update { autoAdvance = enabled } }
    }

    private fun scheduleAdvance(from: Int) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(ADVANCE_DELAY_MS)
            val s = _state.value ?: return@launch
            if (s.index == from) goTo(from + 1)
        }
    }

    private fun persist() {
        val s = _state.value ?: return
        viewModelScope.launch {
            sessions.save(RunSession(eventId, today, s.items, s.counts, s.index, s.completed))
        }
    }

    private companion object {
        const val ADVANCE_DELAY_MS = 600L
    }
}
