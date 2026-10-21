package id.haho.muslimdaily.feature.duas.customize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.CustomEntry
import id.haho.muslimdaily.domain.myset.FormChoice
import id.haho.muslimdaily.domain.myset.ResolvedItem
import id.haho.muslimdaily.navigation.CustomizeRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomizeUiState(
    val event: DuaEvent?,
    /** Every item in stored order; hidden items keep their slot so a restore puts them back in place. */
    val all: List<ResolvedItem>,
    /** Enabled items in the user's order. */
    val shown: List<ResolvedItem>,
    /** Hidden items, restorable. */
    val hidden: List<ResolvedItem>,
    val choices: Map<String, FormChoice>,
    val isCustomized: Boolean,
)

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val mySets: MySetRepository,
) : ViewModel() {
    val eventId: String = savedState.toRoute<CustomizeRoute>().eventId

    val state: StateFlow<CustomizeUiState?> = combine(
        content.event(eventId),
        content.duasForEvent(eventId),
        mySets.customization(eventId),
    ) { event, duas, custom ->
        val items = ActiveSet.items(ActiveSet.canonical(duas), custom.copy(useMySet = true))
        CustomizeUiState(
            event = event,
            all = items,
            shown = items.filter { it.enabled },
            hidden = items.filterNot { it.enabled },
            choices = custom.choices,
            isCustomized = custom.entries.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun save(entries: List<CustomEntry>) {
        viewModelScope.launch { mySets.saveItems(eventId, entries) }
    }

    fun setEnabled(key: String, enabled: Boolean) {
        val s = state.value ?: return
        save(CustomizeRules.setEnabled(s.all, key, enabled))
    }

    /** Persists a new order of the shown items (after a drag or a move action). */
    fun reorder(keys: List<String>) {
        val s = state.value ?: return
        save(CustomizeRules.reorder(s.all, keys))
    }

    fun move(key: String, delta: Int) {
        val s = state.value ?: return
        CustomizeRules.move(s.all, key, delta)?.let(::save)
    }

    fun choose(group: String, choice: FormChoice) {
        viewModelScope.launch { mySets.setChoice(eventId, group, choice) }
    }

    fun resetToFullSet() {
        viewModelScope.launch { mySets.reset(eventId) }
    }
}

/** Pure editing rules for a customised set; hidden items keep their slots. */
object CustomizeRules {
    fun setEnabled(all: List<ResolvedItem>, key: String, enabled: Boolean): List<CustomEntry> =
        all.mapIndexed { i, r -> CustomEntry(r.item.key, if (r.item.key == key) enabled else r.enabled, i) }

    /** Applies a new order of the visible items, leaving hidden items in their slots. */
    fun reorder(all: List<ResolvedItem>, shownKeys: List<String>): List<CustomEntry> {
        val queue = ArrayDeque(shownKeys)
        return all.mapIndexed { i, r ->
            if (r.enabled) CustomEntry(queue.removeFirstOrNull() ?: r.item.key, true, i) else CustomEntry(r.item.key, false, i)
        }
    }

    fun move(all: List<ResolvedItem>, key: String, delta: Int): List<CustomEntry>? {
        val keys = all.filter { it.enabled }.map { it.item.key }.toMutableList()
        val from = keys.indexOf(key)
        if (from < 0) return null
        val to = (from + delta).coerceIn(0, keys.lastIndex)
        if (from == to) return null
        keys.add(to, keys.removeAt(from))
        return reorder(all, keys)
    }
}
