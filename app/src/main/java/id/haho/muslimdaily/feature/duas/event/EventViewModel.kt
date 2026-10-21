package id.haho.muslimdaily.feature.duas.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val useMySet: Boolean,
    val customized: Boolean,
    /** Items in reading order. In My set, disabled items are left out. */
    val items: List<ResolvedItem>,
    val hiddenCount: Int,
    val prefs: ReadingPrefs,
)

@HiltViewModel
class EventViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val mySets: MySetRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val eventId: String = savedState.toRoute<EventRoute>().eventId

    val state: StateFlow<EventUiState?> = combine(
        content.event(eventId),
        content.duasForEvent(eventId),
        mySets.customization(eventId),
        settings.settings,
    ) { event, duas, custom, s ->
        build(event, ActiveSet.canonical(duas), custom, ReadingPrefs(s.translationMode, s.showTransliteration))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(event: DuaEvent?, canonical: List<id.haho.muslimdaily.domain.myset.SetItem>, custom: Customization, prefs: ReadingPrefs): EventUiState {
        val all = if (custom.useMySet) ActiveSet.items(canonical, custom) else ActiveSet.items(canonical, custom.copy(entries = emptyList()))
        val shown = if (custom.useMySet) all.filter { it.enabled } else all
        return EventUiState(
            event = event,
            useMySet = custom.useMySet,
            customized = custom.entries.isNotEmpty(),
            items = shown,
            hiddenCount = all.size - shown.size,
            prefs = prefs,
        )
    }

    fun setUseMySet(useMySet: Boolean) {
        viewModelScope.launch { mySets.setUseMySet(eventId, useMySet) }
    }

    fun chooseForm(group: String, duaId: String) {
        viewModelScope.launch { mySets.setChoice(eventId, group, FormChoice.Fixed(duaId)) }
    }
}
