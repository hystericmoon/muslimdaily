package id.haho.muslimdaily.core.model

import kotlinx.serialization.Serializable

/** The five daily prayers plus Jumu'ah, which replaces Dhuhr on Friday for those who attend. */
@Serializable
enum class Prayer { FAJR, DHUHR, ASR, MAGHRIB, ISHA, JUMUAH }

/** Times shown in the prayer list. SUNRISE is informational only; there is deliberately no Imsak. */
enum class PrayerTimeSlot { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

val FIVE_PRAYERS = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

@Serializable
enum class Position { BEFORE, AFTER }

@Serializable
enum class Tier { MUAKKADAH, GHAIRU_MUAKKADAH }

@Serializable
enum class ContentStatus {
    @kotlinx.serialization.SerialName("draft") DRAFT,
    @kotlinx.serialization.SerialName("review") REVIEW,
    @kotlinx.serialization.SerialName("published") PUBLISHED,
}

@Serializable
data class SourceRef(val book: String = "", val no: String = "")
