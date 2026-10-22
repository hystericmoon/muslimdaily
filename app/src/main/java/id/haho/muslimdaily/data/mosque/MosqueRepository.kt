package id.haho.muslimdaily.data.mosque

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.location.distanceKm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos

/** A mosque or mushalla from OpenStreetMap. [name] is empty when OSM has none. */
data class Mosque(val name: String, val latitude: Double, val longitude: Double, val distanceMeters: Int)

/**
 * Offline list of places of worship in Indonesia (OpenStreetMap, ODbL), bundled as
 * assets/mosques.bin by tools/osm/build_mosques.py. Looked up on the device; the user's location
 * never leaves the phone. OSM coverage is incomplete, so the screen also offers a map search.
 */
@Singleton
class MosqueRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private class Index(val lat: IntArray, val lon: IntArray, val names: Array<String>)

    private val mutex = Mutex()
    private var index: Index? = null

    private suspend fun index(): Index = mutex.withLock {
        index ?: withContext(Dispatchers.IO) { load() }.also { index = it }
    }

    private fun load(): Index = DataInputStream(context.assets.open(ASSET).buffered()).use { input ->
        val magic = ByteArray(4).also { input.readFully(it) }
        require(String(magic, Charsets.US_ASCII) == "MSQ1") { "Unknown mosque data format" }
        val count = input.readInt()
        val lat = IntArray(count)
        val lon = IntArray(count)
        val names = Array(count) { "" }
        for (i in 0 until count) {
            lat[i] = input.readInt()
            lon[i] = input.readInt()
            val len = input.readUnsignedShort()
            if (len > 0) names[i] = String(ByteArray(len).also { input.readFully(it) }, Charsets.UTF_8)
        }
        Index(lat, lon, names)
    }

    /** Up to [limit] places within [radiusKm], nearest first. Data is sorted by latitude. */
    suspend fun nearest(latitude: Double, longitude: Double, limit: Int = 20, radiusKm: Double = 5.0): List<Mosque> {
        val idx = index()
        return withContext(Dispatchers.Default) {
            val dLat = radiusKm / 111.0
            val dLon = radiusKm / (111.0 * cos(Math.toRadians(latitude)).coerceAtLeast(0.01))
            val from = lowerBound(idx.lat, ((latitude - dLat) * 1e6).toInt())
            val to = lowerBound(idx.lat, ((latitude + dLat) * 1e6).toInt() + 1)
            val minLon = ((longitude - dLon) * 1e6).toInt()
            val maxLon = ((longitude + dLon) * 1e6).toInt()
            buildList {
                for (i in from until to) {
                    if (idx.lon[i] !in minLon..maxLon) continue
                    val la = idx.lat[i] / 1e6
                    val lo = idx.lon[i] / 1e6
                    val meters = (distanceKm(latitude, longitude, la, lo) * 1000).toInt()
                    if (meters <= radiusKm * 1000) add(Mosque(idx.names[i], la, lo, meters))
                }
            }.sortedBy { it.distanceMeters }.take(limit)
        }
    }

    private fun lowerBound(a: IntArray, key: Int): Int {
        var lo = 0
        var hi = a.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (a[mid] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private companion object {
        const val ASSET = "mosques.bin"
    }
}
