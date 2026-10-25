package id.haho.muslimdaily.widget

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import id.haho.muslimdaily.R

/**
 * The widget names the system font "google-sans-flex", which Robolectric doesn't have; its
 * fallback has no SemiBold, so renders would lose the next prayer's weight. Tests register that
 * name as the app's Plus Jakarta Sans instead: close to Google Sans, a little wider (so fit checks
 * stay on the safe side), and what the screenshots and the widget-picker preview have always shown.
 */
object WidgetTestFont {
    const val FAMILY = "google-sans-flex"

    fun install(context: Context) {
        val field = Typeface::class.java.getDeclaredField("sSystemFontMap").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = field.get(null) as MutableMap<String, Typeface>
        map[FAMILY] = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)!!
    }
}
