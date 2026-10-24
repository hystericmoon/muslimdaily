package id.haho.muslimdaily.core.diagnostics

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import id.haho.muslimdaily.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Keeps the last few crashes on the device. The app has no analytics, so without this a crash on
 * a tester's phone leaves no trace. Nothing is sent: the user can attach the files to a report.
 *
 * A report holds the app version, device model, Android version, thread and stack trace; no
 * settings, location or content.
 */
object CrashLog {
    private const val DIR = "crashes"
    private const val KEEP = 5
    private const val PREFS = "crash_log"
    private const val SEEN = "seen"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val now = Instant.now()
        val text = buildString {
            appendLine("Adwam ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Time: $now")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace)
        }
        File(dir(context), "crash-${now.toEpochMilli()}.txt").writeText(text)
        files(context).drop(KEEP).forEach { it.delete() }
    }

    /** Newest first. */
    fun files(context: Context): List<File> =
        dir(context).listFiles { f -> f.name.startsWith("crash-") }.orEmpty().sortedByDescending { it.name }

    /** A crash the user hasn't been asked about yet. */
    fun unseen(context: Context): File? {
        val latest = files(context).firstOrNull() ?: return null
        val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SEEN, null)
        return latest.takeIf { it.name != seen }
    }

    fun markSeen(context: Context) {
        val latest = files(context).firstOrNull() ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(SEEN, latest.name) }
    }

    fun clear(context: Context) {
        files(context).forEach { it.delete() }
    }
}
