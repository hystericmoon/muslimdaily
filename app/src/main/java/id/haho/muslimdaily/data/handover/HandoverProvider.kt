package id.haho.muslimdaily.data.handover

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.haho.muslimdaily.data.backup.BackupRepository
import id.haho.muslimdaily.notifications.ReminderScheduler
import id.haho.muslimdaily.widget.GlanceRefresher
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

/**
 * Gives this app's data to the new Adwam app (see [Handover]). Guarded by a signature permission,
 * so only an app signed with the same key can read it:
 *  - `content://<package>.handover/backup` streams the same backup file the user can export;
 *  - [Handover.METHOD_HANDED_OVER], called by the new app once it has restored that file, makes
 *    this app stand down: no more reminders, and the widget and app point to the new one.
 */
class HandoverProvider : ContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun backup(): BackupRepository
        fun scheduler(): ReminderScheduler
        fun refresher(): GlanceRefresher
    }

    private val deps by lazy { EntryPointAccessors.fromApplication(requireNotNull(context).applicationContext, Deps::class.java) }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        requireCaller()
        require(mode == "r" && uri.lastPathSegment == Handover.PATH_BACKUP) { "Unknown handover file: $uri" }
        val bytes = runBlocking { deps.backup().export() }.toByteArray()
        val (read, write) = ParcelFileDescriptor.createPipe()
        thread(name = "handover") {
            ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(bytes) }
        }
        return read
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        requireCaller()
        require(method == Handover.METHOD_HANDED_OVER) { "Unknown handover call: $method" }
        val context = requireNotNull(context)
        val caller = requireNotNull(callingPackage) { "Unknown caller" }
        require(caller == Handover.newPackageFor(context.packageName)) { "$caller is not the new app" }
        Handover.markMoved(context, caller)
        runBlocking {
            deps.scheduler().rescheduleAll()
            deps.refresher().refresh()
        }
        return Bundle.EMPTY
    }

    /** The manifest already demands the permission; checked again so a mistake there can't open this up. */
    private fun requireCaller() {
        val context = requireNotNull(context)
        if (context.checkCallingPermission(Handover.permission(context.packageName)) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Handover needs ${Handover.permission(context.packageName)}")
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
