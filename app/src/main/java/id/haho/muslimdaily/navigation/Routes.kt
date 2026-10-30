package id.haho.muslimdaily.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import id.haho.muslimdaily.R
import kotlinx.serialization.Serializable

// Tab graphs
@Serializable data object TodayGraph
@Serializable data object DuasGraph
@Serializable data object MemorizeGraph
@Serializable data object SettingsGraph

// Today
@Serializable data object TodayRoute
@Serializable data object MosquesRoute
@Serializable data object QiblaRoute
@Serializable data object FastingRoute
// The location and prayer-calculation screens opened from Today stay in Today's tab, so the
// bottom bar (and TalkBack) doesn't jump to Settings.
@Serializable data object TodayLocationRoute
@Serializable data object TodayPrayerSettingsRoute

// Duas
@Serializable data object DuaLibraryRoute
@Serializable data object SearchRoute
@Serializable data class EventRoute(val eventId: String)
@Serializable data class DuaDetailRoute(val duaId: String)
@Serializable data class RunRoute(val eventId: String)
/** [id] null creates a routine; [fromEvent] pre-fills it with that occasion's duas. */
@Serializable data class RoutineEditRoute(val id: String? = null, val fromEvent: String? = null, val withDua: String? = null)

// Memorize
@Serializable data object MemorizeHomeRoute
@Serializable data object MemorizePickerRoute
@Serializable data class MemorizeSessionRoute(val duaId: String, val fromReview: Boolean = false)
@Serializable data class ListenOnlyRoute(val duaId: String)

// Settings
@Serializable data object SettingsHomeRoute
@Serializable data object PrayerSettingsRoute
@Serializable data object LocationRoute
@Serializable data object RemindersRoute
@Serializable data object DisplaySettingsRoute
@Serializable data object AudioSettingsRoute
@Serializable data object MethodologyRoute
@Serializable data object MySetsDataRoute
@Serializable data object AboutRoute
@Serializable data object DiagnosticsRoute

@Serializable data object UpdatesRoute

enum class TopLevelDestination(
    val graph: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    TODAY(TodayGraph, R.string.nav_today, R.drawable.ic_wb_twilight, R.drawable.ic_wb_twilight_fill),
    DUAS(DuasGraph, R.string.nav_duas, R.drawable.ic_menu_book, R.drawable.ic_menu_book_fill),
    MEMORIZE(MemorizeGraph, R.string.nav_memorize, R.drawable.ic_record_voice_over, R.drawable.ic_record_voice_over_fill),
    SETTINGS(SettingsGraph, R.string.nav_settings, R.drawable.ic_settings, R.drawable.ic_settings_fill),
}
