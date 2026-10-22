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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
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

    /** The next prayer and how long until it, kept current while the screen is shown. */
    val header: StateFlow<MosqueHeader?> = combine(ticker(), settings.settings) { now, s ->
        PrayerSnapshots.at(now, s, calculator)?.let { MosqueHeader(it.next, it.zone, now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun ticker() = flow {
        while (true) {
            emit(Instant.now(clock))
            delay(TICK_MS)
        }
    }

    private var fixedAt: Instant? = null

    init {
        if (location.hasPermission()) locate() else viewModelScope.launch { askPermission() }
    }

    /**
     * Back on screen (from settings, or reopened from a later pre-prayer notification): pick up a
     * permission granted meanwhile, and take a fresh fix if the last one is old.
     */
    fun onResume() {
        val stale = fixedAt?.let { Duration.between(it, Instant.now(clock)) > REFIX_AFTER } ?: false
        when (val s = _state.value) {
            is MosqueUiState.NeedsPermission -> if (location.hasPermission()) locate()
            is MosqueUiState.Ready -> if (stale && s.origin !is Origin.Saved) locate()
            else -> Unit
        }
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
            fixedAt = Instant.now(clock)
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

private const val TICK_MS = 30_000L
private val REFIX_AFTER: Duration = Duration.ofMinutes(2)
