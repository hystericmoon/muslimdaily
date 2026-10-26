package id.haho.muslimdaily.data.handover

import android.content.Context
import androidx.core.content.edit

/**
 * Moving the app to its new package, id.haho.adwam. Android treats a new applicationId as a
 * different app with empty data, so this app hands its data, as a backup file, to the new one
 * through [HandoverProvider]. Only an app signed with the same key may ask. Once the new app
 * has it, this one stops sending reminders and points people to the new app.
 *
 * Packages pair by suffix: id.haho.muslimdaily.debug (the GitHub sideload build) moves to
 * id.haho.adwam.debug, and id.haho.muslimdaily to id.haho.adwam.
 */
object Handover {
    const val LEGACY_BASE = "id.haho.muslimdaily"
    const val NEW_BASE = "id.haho.adwam"

    /** [HandoverProvider]'s path for the backup file. */
    const val PATH_BACKUP = "backup"

    /** [HandoverProvider] call: the new app has restored the data. */
    const val METHOD_HANDED_OVER = "handed_over"

    private const val PREFS = "handover"
    private const val KEY_MOVED_TO = "moved_to"

    /** The package this build takes its data from: the same build of the previous package. */
    fun legacyPackageFor(packageName: String): String? =
        if (packageName.startsWith(NEW_BASE)) LEGACY_BASE + packageName.removePrefix(NEW_BASE) else null

    /** The new app's package for this build, or null when this build is the new app. */
    fun newPackageFor(packageName: String): String? =
        if (packageName.startsWith(LEGACY_BASE)) NEW_BASE + packageName.removePrefix(LEGACY_BASE) else null

    fun authority(packageName: String) = "$packageName.handover"

    fun permission(packageName: String) = "$packageName.permission.HANDOVER"

    /**
     * The package this install handed its data to, if it has. Kept outside the settings, so it
     * never travels in a backup file to the app that received it.
     */
    fun movedTo(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MOVED_TO, null)

    internal fun markMoved(context: Context, packageName: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) { putString(KEY_MOVED_TO, packageName) }
}
