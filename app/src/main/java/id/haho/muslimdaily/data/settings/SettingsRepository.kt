package id.haho.muslimdaily.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.core.designsystem.theme.ThemeMode
import id.haho.muslimdaily.core.model.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(block: SettingsEditor.() -> Unit) {
        store.edit { SettingsEditor(it).block() }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        val calc = CalculationSettings(
            method = enumOr(K.calcMethod, d.calculation.method),
            customFajrAngle = this[K.customFajr] ?: d.calculation.customFajrAngle,
            customIshaAngle = this[K.customIsha] ?: d.calculation.customIshaAngle,
            asr = enumOr(K.asr, d.calculation.asr),
            offsets = PrayerOffsets(
                fajr = this[K.offset("fajr")] ?: 0,
                sunrise = this[K.offset("sunrise")] ?: 0,
                dhuhr = this[K.offset("dhuhr")] ?: 0,
                asr = this[K.offset("asr")] ?: 0,
                maghrib = this[K.offset("maghrib")] ?: 0,
                isha = this[K.offset("isha")] ?: 0,
            ),
            sunriseRiseMinutes = this[K.sunriseRise] ?: d.calculation.sunriseRiseMinutes,
            zawalMinutes = this[K.zawal] ?: d.calculation.zawalMinutes,
        )
        val location = this[K.locName]?.let { name ->
            SavedLocation(
                name = name,
                latitude = this[K.locLat] ?: return@let null,
                longitude = this[K.locLon] ?: return@let null,
                timeZoneId = this[K.locTz] ?: return@let null,
                source = enumOr(K.locSource, LocationSource.MANUAL),
                updatedAtMillis = this[K.locUpdated] ?: 0L,
            )
        }
        val r = d.reminders
        val reminders = ReminderSettings(
            adhan = Prayer.entries.associateWith { enumOr(K.adhan(it), AdhanMode.OFF) },
            morningAdhkar = this[K.remMorning] ?: r.morningAdhkar,
            morningOffsetMinutes = this[K.remMorningOffset] ?: r.morningOffsetMinutes,
            eveningAdhkar = this[K.remEvening] ?: r.eveningAdhkar,
            eveningOffsetMinutes = this[K.remEveningOffset] ?: r.eveningOffsetMinutes,
            beforeSleep = this[K.remSleep] ?: r.beforeSleep,
            beforeSleepTime = this[K.remSleepTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: r.beforeSleepTime,
            friday = this[K.remFriday] ?: r.friday,
            fridayTime = this[K.remFridayTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: r.fridayTime,
            reviewQueue = this[K.remReview] ?: r.reviewQueue,
            reviewTime = this[K.remReviewTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: r.reviewTime,
            mosqueBefore = this[K.remMosque] ?: r.mosqueBefore,
            mosqueBeforeMinutes = this[K.remMosqueMinutes] ?: r.mosqueBeforeMinutes,
            nextPrayerOngoing = this[K.remOngoing] ?: r.nextPrayerOngoing,
            fasting = this[K.remFasting] ?: r.fasting,
            fastingTime = this[K.remFastingTime]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: r.fastingTime,
            fastingWeekly = this[K.remFastingWeekly] ?: r.fastingWeekly,
        )
        val m = d.memorize
        return AppSettings(
            themeMode = enumOr(K.theme, d.themeMode),
            dynamicColor = this[K.dynamicColor] ?: d.dynamicColor,
            palette = enumOr(K.palette, d.palette),
            glassEffect = this[K.glassEffect] ?: d.glassEffect,
            translationMode = enumOr(K.translation, d.translationMode),
            showTransliteration = this[K.transliteration] ?: d.showTransliteration,
            arabicScale = this[K.arabicScale] ?: d.arabicScale,
            translationScale = this[K.translationScale] ?: d.translationScale,
            calculation = calc,
            iqamahOffsets = Prayer.entries.mapNotNull { p -> this[K.iqamah(p)]?.let { p to it } }.toMap(),
            hijriAdjustment = this[K.hijri] ?: d.hijriAdjustment,
            location = location,
            travelMode = this[K.travel] ?: d.travelMode,
            dhuhrQabliyahRakah = this[K.dhuhrQabliyah] ?: d.dhuhrQabliyahRakah,
            keepChecklistHistory = this[K.checklistHistory] ?: d.keepChecklistHistory,
            audioSpeed = this[K.audioSpeed] ?: d.audioSpeed,
            reciterId = this[K.reciter] ?: d.reciterId,
            autoAdvance = this[K.autoAdvance] ?: d.autoAdvance,
            haptics = this[K.haptics] ?: d.haptics,
            memorize = MemorizeSettings(
                loopsPerChunk = this[K.memLoops] ?: m.loopsPerChunk,
                pauseMode = enumOr(K.memPauseMode, m.pauseMode),
                manualPauseSeconds = this[K.memPauseSeconds] ?: m.manualPauseSeconds,
                speed = this[K.memSpeed] ?: m.speed,
            ),
            reminders = reminders,
        )
    }

    private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
}

/** Typed writes against the preferences file. */
class SettingsEditor internal constructor(private val p: MutablePreferences) {
    var themeMode: ThemeMode by enumKey(K.theme, ThemeMode.SYSTEM)
    var dynamicColor: Boolean by key(K.dynamicColor, false)
    var palette: AppPalette by enumKey(K.palette, AppPalette.entries.first())
    var glassEffect: Boolean by key(K.glassEffect, true)
    var translationMode: TranslationMode by enumKey(K.translation, TranslationMode.INDONESIAN)
    var showTransliteration: Boolean by key(K.transliteration, false)
    var arabicScale: Float by key(K.arabicScale, 1f)
    var translationScale: Float by key(K.translationScale, 1f)
    var calcMethod: CalcMethod by enumKey(K.calcMethod, CalcMethod.KEMENAG)
    var customFajrAngle: Double by key(K.customFajr, 20.0)
    var customIshaAngle: Double by key(K.customIsha, 18.0)
    var asr: AsrMethod by enumKey(K.asr, AsrMethod.STANDARD)
    var sunriseRiseMinutes: Int by key(K.sunriseRise, 15)
    var zawalMinutes: Int by key(K.zawal, 10)
    var hijriAdjustment: Int by key(K.hijri, 0)
    var travelMode: Boolean by key(K.travel, false)
    var dhuhrQabliyahRakah: Int by key(K.dhuhrQabliyah, 4)
    var keepChecklistHistory: Boolean by key(K.checklistHistory, false)
    var audioSpeed: Float by key(K.audioSpeed, 1f)
    var reciterId: String by key(K.reciter, "default")
    var autoAdvance: Boolean by key(K.autoAdvance, true)
    var haptics: Boolean by key(K.haptics, true)
    var memLoops: Int by key(K.memLoops, 3)
    var memPauseMode: PauseMode by enumKey(K.memPauseMode, PauseMode.AUTO)
    var memPauseSeconds: Int by key(K.memPauseSeconds, 3)
    var memSpeed: Float by key(K.memSpeed, 0.85f)
    var remMorning: Boolean by key(K.remMorning, false)
    var remMorningOffset: Int by key(K.remMorningOffset, 15)
    var remEvening: Boolean by key(K.remEvening, false)
    var remEveningOffset: Int by key(K.remEveningOffset, 15)
    var remSleep: Boolean by key(K.remSleep, false)
    var remFriday: Boolean by key(K.remFriday, false)
    var remReview: Boolean by key(K.remReview, false)
    var remMosque: Boolean by key(K.remMosque, false)
    var remMosqueMinutes: Int by key(K.remMosqueMinutes, 10)
    var remOngoing: Boolean by key(K.remOngoing, false)
    var remFasting: Boolean by key(K.remFasting, false)
    var remFastingWeekly: Boolean by key(K.remFastingWeekly, true)
    fun setFastingTime(time: LocalTime) { p[K.remFastingTime] = time.toSecondOfDay() }

    fun setOffset(slot: String, minutes: Int) { p[K.offset(slot)] = minutes }
    fun setIqamah(prayer: Prayer, minutes: Int?) {
        if (minutes == null || minutes <= 0) p.remove(K.iqamah(prayer)) else p[K.iqamah(prayer)] = minutes
    }
    fun setAdhan(prayer: Prayer, mode: AdhanMode) { p[K.adhan(prayer)] = mode.name }
    fun setSleepTime(time: LocalTime) { p[K.remSleepTime] = time.toSecondOfDay() }
    fun setFridayTime(time: LocalTime) { p[K.remFridayTime] = time.toSecondOfDay() }
    fun setReviewTime(time: LocalTime) { p[K.remReviewTime] = time.toSecondOfDay() }

    fun setLocation(location: SavedLocation) {
        p[K.locName] = location.name
        p[K.locLat] = location.latitude
        p[K.locLon] = location.longitude
        p[K.locTz] = location.timeZoneId
        p[K.locSource] = location.source.name
        p[K.locUpdated] = location.updatedAtMillis
    }

    private fun <T> key(key: Preferences.Key<T>, default: T) = object : kotlin.properties.ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = p[key] ?: default
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) { p[key] = value }
    }

    private fun <E : Enum<E>> enumKey(key: Preferences.Key<String>, default: E) =
        object : kotlin.properties.ReadWriteProperty<Any?, E> {
            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): E =
                p[key]?.let { name -> default.declaringJavaClass.enumConstants?.firstOrNull { it.name == name } } ?: default
            override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: E) { p[key] = value.name }
        }
}

internal object K {
    val theme = stringPreferencesKey("theme")
    val dynamicColor = booleanPreferencesKey("dynamic_color")
    val palette = stringPreferencesKey("palette")
    val glassEffect = booleanPreferencesKey("glass_effect")
    val translation = stringPreferencesKey("translation_mode")
    val transliteration = booleanPreferencesKey("transliteration")
    val arabicScale = floatPreferencesKey("arabic_scale")
    val translationScale = floatPreferencesKey("translation_scale")
    val calcMethod = stringPreferencesKey("calc_method")
    val customFajr = doublePreferencesKey("custom_fajr_angle")
    val customIsha = doublePreferencesKey("custom_isha_angle")
    val asr = stringPreferencesKey("asr_method")
    val sunriseRise = intPreferencesKey("sunrise_rise_minutes")
    val zawal = intPreferencesKey("zawal_minutes")
    fun offset(slot: String) = intPreferencesKey("offset_$slot")
    fun iqamah(prayer: Prayer) = intPreferencesKey("iqamah_${prayer.name}")
    fun adhan(prayer: Prayer) = stringPreferencesKey("adhan_${prayer.name}")
    val hijri = intPreferencesKey("hijri_adjust")
    val locName = stringPreferencesKey("loc_name")
    val locLat = doublePreferencesKey("loc_lat")
    val locLon = doublePreferencesKey("loc_lon")
    val locTz = stringPreferencesKey("loc_tz")
    val locSource = stringPreferencesKey("loc_source")
    val locUpdated = longPreferencesKey("loc_updated")
    val travel = booleanPreferencesKey("travel_mode")
    val dhuhrQabliyah = intPreferencesKey("dhuhr_qabliyah_rakah")
    val checklistHistory = booleanPreferencesKey("checklist_history")
    val audioSpeed = floatPreferencesKey("audio_speed")
    val reciter = stringPreferencesKey("reciter")
    val autoAdvance = booleanPreferencesKey("auto_advance")
    val haptics = booleanPreferencesKey("haptics")
    val memLoops = intPreferencesKey("mem_loops")
    val memPauseMode = stringPreferencesKey("mem_pause_mode")
    val memPauseSeconds = intPreferencesKey("mem_pause_seconds")
    val memSpeed = floatPreferencesKey("mem_speed")
    val remMorning = booleanPreferencesKey("rem_morning")
    val remMorningOffset = intPreferencesKey("rem_morning_offset")
    val remEvening = booleanPreferencesKey("rem_evening")
    val remEveningOffset = intPreferencesKey("rem_evening_offset")
    val remSleep = booleanPreferencesKey("rem_sleep")
    val remSleepTime = intPreferencesKey("rem_sleep_time")
    val remFriday = booleanPreferencesKey("rem_friday")
    val remFridayTime = intPreferencesKey("rem_friday_time")
    val remReview = booleanPreferencesKey("rem_review")
    val remReviewTime = intPreferencesKey("rem_review_time")
    val remMosque = booleanPreferencesKey("rem_mosque")
    val remMosqueMinutes = intPreferencesKey("rem_mosque_minutes")
    val remOngoing = booleanPreferencesKey("rem_next_prayer_ongoing")
    val remFasting = booleanPreferencesKey("rem_fasting")
    val remFastingTime = intPreferencesKey("rem_fasting_time")
    val remFastingWeekly = booleanPreferencesKey("rem_fasting_weekly")
}
