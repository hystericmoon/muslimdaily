package id.haho.muslimdaily.feature.mosque

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.data.location.LocationRepository
import id.haho.muslimdaily.data.mosque.Mosque
import id.haho.muslimdaily.data.mosque.MosqueRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.NextPrayer
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Where distances are measured from, and how much to trust them. */
sealed interface Origin {
    val latitude: Double
    val longitude: Double

    data class Precise(override val latitude: Double, override val longitude: Double, val accuracyMeters: Int?) : Origin
    data class Approximate(override val latitude: Double, override val longitude: Double) : Origin
    data class Saved(override val latitude: Double, override val longitude: Double, val name: String) : Origin
}

sealed interface MosqueUiState {
    data object Locating : MosqueUiState

    /** No location permission yet. [savedName] offers the saved prayer location as a fallback. */
    data class NeedsPermission(val savedName: String?) : MosqueUiState

    /** Permission granted but no fix arrived, and there is no saved location to fall back on. */
    data object Unavailable : MosqueUiState

    data class Ready(val origin: Origin, val mosques: List<Mosque>) : MosqueUiState
}

data class MosqueHeader(val next: NextPrayer, val zone: ZoneId, val now: Instant)

@HiltViewModel
class NearbyMosqueViewModel @Inject constructor(
    private val location: LocationRepository,
    private val mosques: MosqueRepository,
    private val settings: SettingsRepository,
    private val calculator: PrayerCalculator,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<MosqueUiState>(MosqueUiState.Locating)
    val state: StateFlow<MosqueUiState> = _state.asStateFlow()

    private val _header = MutableStateFlow<MosqueHeader?>(null)
    val header: StateFlow<MosqueHeader?> = _header.asStateFlow()

    init {
        viewModelScope.launch {
            val now = Instant.now(clock)
            PrayerSnapshots.at(now, settings.current(), calculator)?.let { _header.value = MosqueHeader(it.next, it.zone, now) }
        }
        if (location.hasPermission()) locate() else viewModelScope.launch { askPermission() }
    }

    private suspend fun askPermission() {
        _state.value = MosqueUiState.NeedsPermission(settings.current().location?.name)
    }

    fun hasPrecisePermission(): Boolean = location.hasPrecisePermission()

    /** After the permission dialog: locate if anything was granted, otherwise stay on the ask. */
    fun onPermissionResult() {
        if (location.hasPermission()) locate() else viewModelScope.launch { askPermission() }
    }

    fun locate() {
        viewModelScope.launch {
            _state.value = MosqueUiState.Locating
            val fix = location.currentFix()
            val origin = when {
                fix == null -> savedOrigin()
                fix.precise -> Origin.Precise(fix.latitude, fix.longitude, fix.accuracyMeters?.toInt())
                else -> Origin.Approximate(fix.latitude, fix.longitude)
            }
            show(origin)
        }
    }

    fun useSaved() {
        viewModelScope.launch { show(savedOrigin()) }
    }

    private suspend fun savedOrigin(): Origin? =
        settings.current().location?.let { Origin.Saved(it.latitude, it.longitude, it.name) }

    private suspend fun show(origin: Origin?) {
        _state.value = if (origin == null) {
            MosqueUiState.Unavailable
        } else {
            MosqueUiState.Ready(origin, mosques.nearest(origin.latitude, origin.longitude))
        }
    }
}
