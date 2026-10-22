package id.haho.muslimdaily.data.location

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.core.arabic.ArabicNormalizer
import id.haho.muslimdaily.data.db.CityEntity
import id.haho.muslimdaily.data.db.CityFtsEntity
import id.haho.muslimdaily.data.db.ContentDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos

data class City(
    val id: Long,
    val name: String,
    val region: String,
    val countryCode: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
)

/**
 * Offline city search for the manual location picker, backed by GeoNames (CC BY 4.0) data in
 * assets/cities.tsv. Imported into Room the first time the picker is opened.
 */
@Singleton
class CityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ContentDatabase,
) {
    private val mutex = Mutex()

    private suspend fun ensureLoaded() = mutex.withLock {
        val dao = db.cityDao()
        if (dao.count() > 0) return@withLock
        withContext(Dispatchers.IO) {
            val cities = mutableListOf<CityEntity>()
            context.assets.open("cities.tsv").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val f = line.split('\t')
                    if (f.size >= 9) {
                        cities += CityEntity(
                            id = f[0].toLong(), name = f[1], asciiName = f[2], country = f[3], region = f[4],
                            latitude = f[5].toDouble(), longitude = f[6].toDouble(), timeZone = f[7],
                            population = f[8].toLongOrNull() ?: 0,
                        )
                    }
                }
            }
            db.runInTransactionSuspending {
                cities.chunked(2000).forEach { chunk ->
                    dao.insertCities(chunk)
                    dao.insertFts(
                        chunk.map {
                            CityFtsEntity(
                                cityId = it.id,
                                name = ArabicNormalizer.normalize("${it.name} ${it.asciiName}"),
                                region = ArabicNormalizer.normalize(it.region),
                            )
                        },
                    )
                }
            }
        }
    }

    suspend fun search(query: String, preferredCountry: String = "ID"): List<City> {
        ensureLoaded()
        val fts = ArabicNormalizer.toFtsQuery(query) ?: return emptyList()
        return db.cityDao().search(fts, preferredCountry).map { it.toCity() }
    }

    suspend fun popular(country: String = "ID", limit: Int = 30): List<City> {
        ensureLoaded()
        return db.cityDao().topInCountry(country, limit).map { it.toCity() }
    }

    /** The populated place closest to a coordinate, used to name a GPS fix. */
    suspend fun nearest(latitude: Double, longitude: Double): City? {
        ensureLoaded()
        val dLat = 0.5
        val dLon = 0.5 / cos(Math.toRadians(latitude)).coerceAtLeast(0.1)
        val candidates = db.cityDao().near(latitude - dLat, latitude + dLat, longitude - dLon, longitude + dLon)
        return candidates.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) }?.toCity()
    }

    private fun CityEntity.toCity() = City(
        id = id,
        name = name,
        region = region,
        countryCode = country,
        countryName = Locale.Builder().setRegion(country).build().getDisplayCountry(Locale.forLanguageTag("id")),
        latitude = latitude,
        longitude = longitude,
        timeZone = timeZone,
    )
}

private suspend fun ContentDatabase.runInTransactionSuspending(block: suspend () -> Unit) {
    withTransaction { block() }
}

/** Great-circle distance in kilometres. */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2).let { it * it }
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
}
