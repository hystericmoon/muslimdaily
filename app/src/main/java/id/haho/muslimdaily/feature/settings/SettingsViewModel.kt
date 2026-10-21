package id.haho.muslimdaily.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared by the settings screens: reads [AppSettings] and applies typed edits. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val content: ContentRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _contentVersion = MutableStateFlow<Int?>(null)
    val contentVersion: StateFlow<Int?> = _contentVersion.asStateFlow()

    init {
        viewModelScope.launch { _contentVersion.value = content.contentVersion() }
    }

    fun update(block: SettingsEditor.() -> Unit) {
        viewModelScope.launch { repository.update(block) }
    }
}
