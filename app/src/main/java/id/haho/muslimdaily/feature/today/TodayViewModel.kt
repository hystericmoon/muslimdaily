package id.haho.muslimdaily.feature.today

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.checklist.ChecklistRepository
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.location.LocationRepository
import id.haho.muslimdaily.data.run.RunSessionRepository
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.notifications.ReminderScheduler
import id.haho.muslimdaily.widget.isWidgetPlaced
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    content: ContentRepository,
    private val checklist: ChecklistRepository,
    private val locations: LocationRepository,
    private val runSessions: RunSessionRepository,
    private val calculator: PrayerCalculator,
    private val clock: Clock,
    routines: RoutineRepository,
) : ViewModel() {
    /** The user's routines, started from Today like the adhkar. */
    val routines: StateFlow<List<Routine>> = routines.routines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ticks every 15 seconds; the screen shows minutes, so this keeps it accurate without churn. */
    private val now: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now(clock))
            delay(TICK_MS)
        }
    }

    private val travelKm = MutableStateFlow<Int?>(null)

    private val widgetNeedsExact = MutableStateFlow(false)

    private val settings = settingsRepository.settings

    private val day: Flow<LocalDate> = combine(settings, now) { s, t ->
        val zone = s.location?.timeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        t.atZone(zone).toLocalDate()
    }.distinctUntilChanged()

    private val checked: Flow<Set<String>> = day.flatMapLatest { checklist.checked(it) }

    private val progress = day.flatMapLatest { d ->
        combine(
            runSessions.progress(TodayStateBuilder.MORNING, d),
            runSessions.progress(TodayStateBuilder.EVENING, d),
        ) { m, e ->
            buildMap {
                m?.let { put(TodayStateBuilder.MORNING, RunProgress(it.done, it.total, it.completed)) }
                e?.let { put(TodayStateBuilder.EVENING, RunProgress(it.done, it.total, it.completed)) }
            }
        }
    }

    private val inputs = combine(content.rawatib(), content.fard(), checked, progress, travelKm) { r, f, c, p, km ->
        Inputs(r, f, c, p, km)
    }

    val state: StateFlow<TodayUiState> = combine(settings, now, inputs) { s, t, i ->
        TodayStateBuilder.build(t, s, i.rawatib, i.fard, i.checked, calculator, i.progress, i.travelKm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    init {
        viewModelScope.launch {
            day.collect {
                checklist.pruneBefore(it)
                runSessions.pruneBefore(it)
            }
        }
    }

    /** Called when the screen resumes: compares the cached fix with the saved location. */
    fun checkTravel() {
        viewModelScope.launch {
            val saved = settingsRepository.current().location ?: return@launch
            val km = locations.distanceFromSavedKm(saved) ?: return@launch
            travelKm.value = km.roundToInt().takeIf { km >= TRAVEL_SUGGESTION_KM }
        }
    }

    /**
     * A widget is placed but alarms are inexact (Android 12+ without the permission, the default
     * on 14+), so it can take up to 10 minutes to move to the next prayer.
     */
    val widgetAlarmNote: StateFlow<Boolean> = combine(widgetNeedsExact, settings) { needs, s -> needs && !s.widgetAlarmNoteDismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** On resume: a widget may have been placed, or exact alarms allowed, meanwhile. */
    fun checkWidgetAlarms() {
        widgetNeedsExact.value = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduler.canScheduleExact() && isWidgetPlaced(context)
    }

    fun dismissWidgetAlarms() {
        viewModelScope.launch { settingsRepository.update { widgetAlarmNoteDismissed = true } }
    }

    fun dismissTravel() {
        travelKm.value = null
    }

    fun enableTravelMode() {
        viewModelScope.launch { settingsRepository.update { travelMode = true } }
        travelKm.value = null
    }

    /** [startsNext] null: dismissed without an answer; the calendar stays as calculated. */
    fun answerHijri(check: HijriCheck, startsNext: Boolean?) {
        viewModelScope.launch {
            settingsRepository.update {
                if (startsNext != null) hijriAdjustment = (hijriAdjustment + check.delta(startsNext)).coerceIn(-1, 1)
                hijriConfirmedFor = check.key
            }
        }
    }

    fun setTravelMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { travelMode = enabled } }
    }

    fun toggle(itemKey: String, checked: Boolean) {
        val s = state.value as? TodayUiState.Ready ?: return
        viewModelScope.launch { checklist.toggle(s.data.date, itemKey, checked) }
    }

    fun updateLocationFromGps(onResult: (LocationRepository.FixResult) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { locations.updateFromGps() }.getOrDefault(LocationRepository.FixResult.Unavailable)
            if (result is LocationRepository.FixResult.Success) travelKm.value = null
            onResult(result)
        }
    }

    private data class Inputs(
        val rawatib: List<id.haho.muslimdaily.core.model.Rawatib>,
        val fard: List<id.haho.muslimdaily.core.model.Fard>,
        val checked: Set<String>,
        val progress: Map<String, RunProgress>,
        val travelKm: Int?,
    )

    private companion object {
        const val TICK_MS = 15_000L
        const val TRAVEL_SUGGESTION_KM = 30.0
    }
}
