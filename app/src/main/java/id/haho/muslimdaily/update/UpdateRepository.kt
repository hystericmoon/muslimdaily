package id.haho.muslimdaily.update

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Sideload updater: checks GitHub Releases for a newer APK, downloads it to the cache and hands it
 * to the system installer. Only active when [BuildConfig.SELF_UPDATE] is true (debug / sideload
 * builds); store builds have no INTERNET permission and never call this.
 */
@Singleton
class UpdateRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    val enabled: Boolean get() = BuildConfig.SELF_UPDATE && BuildConfig.UPDATE_REPO.isNotBlank()

    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO, value) }

    /** Beta: pre-releases too. Testers opt in; everyone else stays on final releases. */
    var betaChannel: Boolean
        get() = prefs.getBoolean(KEY_BETA, Versions.isPreRelease(BuildConfig.VERSION_NAME))
        set(value) = prefs.edit { putBoolean(KEY_BETA, value) }

    var lastCheckMillis: Long
        get() = prefs.getLong(KEY_LAST, 0L)
        private set(value) = prefs.edit { putLong(KEY_LAST, value) }

    fun isAutoCheckDue(nowMillis: Long): Boolean =
        enabled && autoCheck && nowMillis - lastCheckMillis >= AUTO_INTERVAL_MS

    /** Returns the newer release, or null when up to date. Throws [IOException] when offline. */
    suspend fun check(nowMillis: Long = System.currentTimeMillis()): AvailableUpdate? = withContext(Dispatchers.IO) {
        val beta = betaChannel
        val url = URL(
            "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/" +
                if (beta) "releases?per_page=$BETA_PAGE" else "releases/latest",
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MuslimDaily/${BuildConfig.VERSION_NAME}")
        }
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    lastCheckMillis = nowMillis
                    if (beta) parseNewestRelease(body, BuildConfig.VERSION_NAME) else parseLatestRelease(body, BuildConfig.VERSION_NAME)
                }
                // No release published yet.
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    lastCheckMillis = nowMillis
                    null
                }
                else -> throw IOException("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK, reporting progress 0..1 (or -1 when the size is unknown). */
    suspend fun download(update: AvailableUpdate, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "muslimdaily-${update.version}.apk")
        val partial = File(dir, target.name + ".part")
        val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "MuslimDaily/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
            conn.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        onProgress(if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else -1f)
                    }
                }
            }
            if (total > 0 && partial.length() != total) throw IOException("Incomplete download")
            if (!partial.renameTo(target)) throw IOException("Could not save update")
            target
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens the "install unknown apps" setting for this app. */
    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * The system installer verifies the signature: an APK signed with a different key cannot
     * replace this app, so a tampered download is rejected there.
     */
    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private companion object {
        const val KEY_AUTO = "auto_check"
        const val KEY_BETA = "beta_channel"
        const val BETA_PAGE = 15
        const val KEY_LAST = "last_check"
        const val TIMEOUT_MS = 15_000
        const val AUTO_INTERVAL_MS = 20L * 60 * 60 * 1000
    }
}
