package id.haho.muslimdaily.feature.duas.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.routine.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import java.time.Clock
import java.time.LocalDate
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.myset.MySetRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.domain.myset.FormChoice
import id.haho.muslimdaily.domain.myset.ResolvedItem
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.navigation.EventRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val event: DuaEvent?,
    /** Items in reading order: the full set, in the forms the reader picked. */
    val items: List<ResolvedItem>,
    val prefs: ReadingPrefs,
    /** Duas in "My hifz" (with their twins). */
    val hifz: Set<String> = emptySet(),
    /** The reader's routines, for the bookmark on each dua. */
    val routines: List<Routine> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val mySets: MySetRepository,
    settings: SettingsRepository,
    private val reviews: ReviewRepository,
    private val clock: Clock,
    private val routines: RoutineRepository,
) : ViewModel() {
    val eventId: String = savedState.toRoute<EventRoute>().eventId

    val state: StateFlow<EventUiState?> = combine(
        content.event(eventId),
        content.duasForEvent(eventId),
        mySets.customization(eventId),
        settings.settings,
        reviews.items().mapLatest { items -> reviews.withTwins(items.map { it.duaId }.toSet()) },
    ) { event, duas, custom, s, hifz ->
        build(event, ActiveSet.canonical(duas), custom, ReadingPrefs(s.translationMode, s.showTransliteration)).copy(hifz = hifz)
    }.combine(routines.routines()) { st, list -> st.copy(routines = list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(event: DuaEvent?, canonical: List<id.haho.muslimdaily.domain.myset.SetItem>, custom: Customization, prefs: ReadingPrefs) =
        // Only the form choices count: an occasion always reads its full set (routines are the
        // reader's own lists).
        EventUiState(event = event, items = ActiveSet.items(canonical, custom.copy(useMySet = false, entries = emptyList())), prefs = prefs)


    /** Saves a dua to "My hifz" to learn later, as Run mode does. */
    fun addToHifz(duaId: String) {
        viewModelScope.launch { reviews.add(duaId, LocalDate.now(clock)) }
    }

    /** Takes a dua out of "My hifz", with its review schedule (the page asks first). */
    fun removeFromHifz(duaId: String) {
        viewModelScope.launch { reviews.remove(duaId) }
    }

    /** The first bookmark, with no routine yet: makes one named [name] that holds the dua. */
    fun startRoutine(name: String, duaId: String) {
        viewModelScope.launch { routines.save(null, name, listOf(duaId)) }
    }

    fun setInRoutine(routineId: String, duaId: String, included: Boolean) {
        viewModelScope.launch { routines.setIncluded(routineId, duaId, included) }
    }

    fun chooseForm(group: String, duaId: String) {
        viewModelScope.launch { mySets.setChoice(eventId, group, FormChoice.Fixed(duaId)) }
    }
}
