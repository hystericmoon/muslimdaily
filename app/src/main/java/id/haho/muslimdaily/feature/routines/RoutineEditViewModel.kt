package id.haho.muslimdaily.feature.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.navigation.RoutineEditRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineEditState(
    val isNew: Boolean,
    val name: String,
    /** Reading order. */
    val items: List<Dua>,
    val picking: Boolean = false,
    val categories: List<DuaCategory> = emptyList(),
    val expanded: Set<String> = emptySet(),
    val eventDuas: Map<String, List<Dua>> = emptyMap(),
    val query: String = "",
    val results: List<Dua>? = null,
    val saved: Boolean = false,
) {
    val selectedIds: Set<String> get() = items.mapTo(HashSet()) { it.id }
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty()
    val eventLabels: Map<String, String> get() = categories.flatMap { it.events }.associate { it.id to it.label }
}

/** Pure list edits, shared with the tests. */
object RoutineEdits {
    fun toggle(items: List<Dua>, dua: Dua): List<Dua> =
        if (items.any { it.id == dua.id }) items.filterNot { it.id == dua.id } else items + dua

    fun move(items: List<Dua>, id: String, delta: Int): List<Dua> {
        val i = items.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in items.indices) return items
        return items.toMutableList().apply { add(j, removeAt(i)) }
    }

    fun reorder(items: List<Dua>, ids: List<String>): List<Dua> {
        val byId = items.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val routines: RoutineRepository,
    private val content: ContentRepository,
    private val mySets: MySetRepository,
) : ViewModel() {
    private val route = savedState.toRoute<RoutineEditRoute>()
    private val _state = MutableStateFlow<RoutineEditState?>(null)
    val state: StateFlow<RoutineEditState?> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val categories = content.categories().first()
            val existing = route.id?.let { routines.routine(it) }
            val (name, items) = when {
                existing != null -> existing.name to content.duas(existing.duaIds)
                route.fromEvent != null -> {
                    // Start from what the user reads for that occasion now (their set, if any).
                    val event = content.eventOnce(route.fromEvent)
                    val duas = ActiveSet.active(ActiveSet.canonical(content.duasForEventOnce(route.fromEvent)), mySets.customizationOnce(route.fromEvent))
                    event?.label.orEmpty() to duas
                }
                else -> "" to emptyList()
            }
            _state.value = RoutineEditState(isNew = existing == null, name = name, items = items, categories = categories)
        }
    }

    fun rename(name: String) = _state.update { it?.copy(name = name.take(MAX_NAME)) }
    fun toggle(dua: Dua) = _state.update { it?.copy(items = RoutineEdits.toggle(it.items, dua)) }
    fun remove(id: String) = _state.update { s -> s?.copy(items = s.items.filterNot { it.id == id }) }
    fun move(id: String, delta: Int) = _state.update { it?.copy(items = RoutineEdits.move(it.items, id, delta)) }
    fun reorder(ids: List<String>) = _state.update { it?.copy(items = RoutineEdits.reorder(it.items, ids)) }
    fun setPicking(picking: Boolean) = _state.update { it?.copy(picking = picking, query = if (picking) it.query else "", results = if (picking) it.results else null) }

    fun toggleEvent(eventId: String) {
        val s = _state.value ?: return
        if (eventId in s.expanded) {
            _state.update { it?.copy(expanded = it.expanded - eventId) }
            return
        }
        _state.update { it?.copy(expanded = it.expanded + eventId) }
        if (eventId !in s.eventDuas) {
            viewModelScope.launch {
                val duas = content.duasForEventOnce(eventId)
                _state.update { it?.copy(eventDuas = it.eventDuas + (eventId to duas)) }
            }
        }
    }

    fun search(query: String) {
        _state.update { it?.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it?.copy(results = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val results = content.search(query)
            _state.update { it?.copy(results = results) }
        }
    }

    fun save() {
        val s = _state.value ?: return
        if (!s.canSave) return
        viewModelScope.launch {
            routines.save(route.id, s.name, s.items.map { it.id })
            _state.update { it?.copy(saved = true) }
        }
    }

    fun delete() {
        val id = route.id ?: return
        viewModelScope.launch {
            routines.delete(id)
            _state.update { it?.copy(saved = true) }
        }
    }

    private companion object {
        const val MAX_NAME = 40
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
