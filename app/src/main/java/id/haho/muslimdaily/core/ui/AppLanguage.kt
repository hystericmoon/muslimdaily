package id.haho.muslimdaily.core.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * The app's language: Indonesian unless the user picks English. Not the device language: many
 * Indonesian phones run in English, and their users expect this app in Indonesian (the Hisn
 * al-Muslim translations are Indonesian too).
 *
 * Android 13+ keeps the choice as the per-app language (also visible in system settings);
 * older versions apply it to the process's resources at start and to each activity.
 */
object AppLanguage {
    const val INDONESIAN = "id"
    const val ENGLISH = "en"
    private const val PREFS = "app_language"
    private const val KEY = "tag"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tags = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (tags != null && !tags.isEmpty) return if (tags[0].language == ENGLISH) ENGLISH else INDONESIAN
        }
        return prefs(context).getString(KEY, null) ?: INDONESIAN
    }

    fun set(activity: Activity, tag: String) {
        prefs(activity).edit { putString(KEY, tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activity with the new configuration.
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            applyToProcess(activity.applicationContext, tag)
            activity.recreate()
        }
    }

    /** Called once from Application.onCreate. */
    fun onAppStart(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            // First start on a non-Indonesian device: pin Indonesian rather than follow it.
            if (manager.applicationLocales.isEmpty && prefs(context).getString(KEY, null) == null) {
                prefs(context).edit { putString(KEY, INDONESIAN) }
                manager.applicationLocales = LocaleList.forLanguageTags(INDONESIAN)
            }
        } else {
            applyToProcess(context, current(context))
        }
    }

    /** Before Android 13: the activity's base context in the chosen language. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = Locale.forLanguageTag(current(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    @Suppress("DEPRECATION") // the only process-wide switch before per-app languages
    private fun applyToProcess(context: Context, tag: String) {
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration).apply { setLocale(locale) }
        res.updateConfiguration(config, res.displayMetrics)
    }
}
