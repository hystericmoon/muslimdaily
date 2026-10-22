package id.haho.muslimdaily.feature.mosque

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import id.haho.muslimdaily.data.mosque.Mosque
import java.util.Locale

/**
 * Hands navigation to whatever maps app the user has. Google Maps gets walking directions
 * straight away; any other app (OsmAnd, Organic Maps, …) gets a pin it can route to.
 */
object MapsIntents {

    fun directions(context: Context, mosque: Mosque, label: String): Intent {
        val coords = "%.6f,%.6f".format(Locale.ROOT, mosque.latitude, mosque.longitude)
        val google = Intent(Intent.ACTION_VIEW, "google.navigation:q=$coords&mode=w".toUri())
        if (google.resolveActivity(context.packageManager) != null) return google
        return Intent(Intent.ACTION_VIEW, "geo:$coords?q=$coords(${Uri.encode(label)})".toUri())
    }

    /** A map search for mosques around a point, for places the offline list doesn't have. */
    fun search(latitude: Double, longitude: Double, query: String): Intent {
        val coords = "%.6f,%.6f".format(Locale.ROOT, latitude, longitude)
        return Intent(Intent.ACTION_VIEW, "geo:$coords?q=${Uri.encode(query)}".toUri())
    }

    fun canHandle(context: Context, intent: Intent): Boolean = intent.resolveActivity(context.packageManager) != null
}
