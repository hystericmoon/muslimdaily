package id.haho.muslimdaily.update

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val update: AvailableUpdate) : UpdateStatus
    /** [progress] is 0..1, or negative when the size is unknown. */
    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateStatus
    data class Ready(val update: AvailableUpdate, val apk: File) : UpdateStatus
    data class Failed(val update: AvailableUpdate?) : UpdateStatus
}

data class UpdateUiState(
    val enabled: Boolean = false,
    val autoCheck: Boolean = true,
    val lastCheckMillis: Long = 0L,
    val status: UpdateStatus = UpdateStatus.Idle,
    /** Show the "update available" dialog (from the automatic daily check). */
    val prompt: Boolean = false,
)

/** Shared between the start-up prompt and the Settings → Pembaruan aplikasi screen (activity scope). */
@HiltViewModel
class UpdateViewModel @Inject constructor(private val repository: UpdateRepository) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateUiState(
            enabled = repository.enabled,
            autoCheck = repository.autoCheck,
            lastCheckMillis = repository.lastCheckMillis,
        ),
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Silent daily check on start; only surfaces a dialog when an update exists. */
    fun autoCheck(nowMillis: Long = System.currentTimeMillis()) {
        if (!repository.isAutoCheckDue(nowMillis) || job?.isActive == true) return
        job = viewModelScope.launch {
            val found = runCatching { repository.check(nowMillis) }.getOrNull()
            _state.update {
                it.copy(
                    lastCheckMillis = repository.lastCheckMillis,
                    status = found?.let(UpdateStatus::Available) ?: it.status,
                    prompt = found != null,
                )
            }
        }
    }

    fun checkNow() {
        if (!repository.enabled || job?.isActive == true) return
        _state.update { it.copy(status = UpdateStatus.Checking) }
        job = viewModelScope.launch {
            val result = runCatching { repository.check() }
            _state.update {
                it.copy(
                    lastCheckMillis = repository.lastCheckMillis,
                    status = result.fold(
                        onSuccess = { found -> found?.let(UpdateStatus::Available) ?: UpdateStatus.UpToDate },
                        onFailure = { UpdateStatus.Failed(null) },
                    ),
                )
            }
        }
    }

    fun download() {
        val update = when (val s = _state.value.status) {
            is UpdateStatus.Available -> s.update
            is UpdateStatus.Failed -> s.update ?: return
            else -> return
        }
        if (job?.isActive == true) return
        _state.update { it.copy(status = UpdateStatus.Downloading(update, 0f)) }
        job = viewModelScope.launch {
            val result = runCatching {
                repository.download(update) { p ->
                    _state.update { s -> s.copy(status = UpdateStatus.Downloading(update, p)) }
                }
            }
            _state.update {
                it.copy(
                    status = result.fold(
                        onSuccess = { apk -> UpdateStatus.Ready(update, apk) },
                        onFailure = { UpdateStatus.Failed(update) },
                    ),
                )
            }
        }
    }

    /** The intent to start: the system installer, or first the "install unknown apps" setting. */
    fun installIntent(): Intent? {
        val ready = _state.value.status as? UpdateStatus.Ready ?: return null
        return if (repository.canInstall()) repository.installIntent(ready.apk) else repository.installPermissionIntent()
    }

    fun dismissPrompt() = _state.update { it.copy(prompt = false) }

    fun setAutoCheck(enabled: Boolean) {
        repository.autoCheck = enabled
        _state.update { it.copy(autoCheck = enabled) }
    }
}
