package id.haho.muslimdaily.data.handover

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.data.backup.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The new app's side of [Handover]: on first launch, take the data from the app's previous
 * package if it is installed. The bridge release of the old app hands over directly; an older
 * one can't, so the user moves it with a backup file instead.
 */
@Singleton
class LegacyImport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupRepository,
) {
    enum class Offer {
        /** Nothing to offer: no previous app, or the user already moved or skipped. */
        NONE,

        /** The previous app can hand its data over. */
        DIRECT,

        /** The previous app is installed but too old to hand over: move it with a backup file. */
        MANUAL,
    }

    val legacyPackage: String? = Handover.legacyPackageFor(context.packageName)

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun offer(): Offer {
        val legacy = legacyPackage ?: return Offer.NONE
        if (prefs.getString(KEY_RESULT, null) != null) return Offer.NONE
        if (!isInstalled(legacy)) return Offer.NONE
        val provider = context.packageManager.resolveContentProvider(Handover.authority(legacy), 0)
        return if (provider != null) Offer.DIRECT else Offer.MANUAL
    }

    /** Restores the previous app's data here, then tells it to stand down. */
    suspend fun run() = withContext(Dispatchers.IO) {
        val legacy = requireNotNull(legacyPackage)
        val uri = Uri.parse("content://${Handover.authority(legacy)}/${Handover.PATH_BACKUP}")
        val text = requireNotNull(context.contentResolver.openInputStream(uri)) { "No data from $legacy" }
            .bufferedReader().use { it.readText() }
        backup.restore(text)
        context.contentResolver.call(Uri.parse("content://${Handover.authority(legacy)}"), Handover.METHOD_HANDED_OVER, null, null)
        record(DONE)
    }

    /** The user chose to start fresh, or has seen the backup instructions: don't offer again. */
    fun dismiss() = record(SKIPPED)

    private fun record(result: String) = prefs.edit(commit = true) { putString(KEY_RESULT, result) }

    private fun isInstalled(pkg: String) = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val PREFS = "handover"
        const val KEY_RESULT = "legacy_import"
        const val DONE = "done"
        const val SKIPPED = "skipped"
    }
}
