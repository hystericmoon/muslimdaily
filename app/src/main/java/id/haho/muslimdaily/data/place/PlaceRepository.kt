package id.haho.muslimdaily.data.place

import id.haho.muslimdaily.data.db.SavedPlaceEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.settings.LocationSource
import id.haho.muslimdaily.data.settings.SavedLocation
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaceKind { HOME, WORK, OTHER }

/**
 * Places the user saved, for one-tap switching. Switching writes the place into the current
 * location, so prayer times, Qibla and the timetable follow it. Which place is active is not
 * stored: it is the one whose coordinates match the current location, so "Use GPS" or choosing a
 * city simply leaves every place inactive. Nothing here reads the device's position.
 */
@Singleton
class PlaceRepository @Inject constructor(
    private val db: UserDatabase,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val dao get() = db.placeDao()

    fun places(): Flow<List<SavedPlaceEntity>> = dao.observeAll()

    /**
     * Saves the current location as a place. Home and Work hold one place each: saving again
     * replaces it. Other places are many, each with the user's own name. Returns false when there
     * is no current location, or an Other place has no name.
     */
    suspend fun saveCurrent(kind: PlaceKind, label: String = ""): Boolean {
        val loc = settings.current().location ?: return false
        val name = label.trim().take(MAX_LABEL)
        if (kind == PlaceKind.OTHER && name.isEmpty()) return false
        val existing = dao.all()
        val same = if (kind == PlaceKind.OTHER) null else existing.firstOrNull { it.kind == kind.name }
        dao.upsert(
            SavedPlaceEntity(
                id = same?.id ?: UUID.randomUUID().toString(),
                kind = kind.name,
                label = if (kind == PlaceKind.OTHER) name else "",
                cityName = loc.name,
                latitude = loc.latitude,
                longitude = loc.longitude,
                timeZoneId = loc.timeZoneId,
                elevation = loc.elevation,
                position = same?.position ?: dao.nextPosition(),
                updatedAt = clock.millis(),
            ),
        )
        return true
    }

    /** Makes [id] the current location. */
    suspend fun switchTo(id: String) {
        val place = dao.place(id) ?: return
        settings.update {
            setLocation(
                SavedLocation(
                    name = place.cityName,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    timeZoneId = place.timeZoneId,
                    source = LocationSource.MANUAL,
                    updatedAtMillis = clock.millis(),
                    elevation = place.elevation,
                ),
            )
        }
    }

    suspend fun remove(id: String) = dao.delete(id)

    companion object {
        const val MAX_LABEL = 40

        /** True when [place] is where the current location points (to about a metre). */
        fun isActive(place: SavedPlaceEntity, current: SavedLocation?): Boolean =
            current != null &&
                kotlin.math.abs(current.latitude - place.latitude) < 1e-5 &&
                kotlin.math.abs(current.longitude - place.longitude) < 1e-5
    }
}
