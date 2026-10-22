package id.haho.muslimdaily.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A published build on GitHub Releases that this device can install. */
data class AvailableUpdate(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

/** Numeric dotted version ("v0.2.0", "0.2.0-beta" → [0, 2, 0]) compared part by part. */
internal object Versions {
    fun parse(raw: String): List<Int>? {
        val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        if (core.isEmpty()) return null
        return core.split('.').map { it.toIntOrNull() ?: return null }
    }

    /** True when [candidate] is strictly newer than [current]. Unparseable versions are never newer. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return true
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

@Serializable
internal data class ReleaseJson(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetJson> = emptyList(),
)

@Serializable
internal data class AssetJson(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
    @SerialName("content_type") val contentType: String? = null,
)

private val ReleaseFormat = Json { ignoreUnknownKeys = true }

/** Reads GitHub's "latest release" response; null when it holds nothing installable or newer. */
internal fun parseLatestRelease(json: String, currentVersion: String): AvailableUpdate? {
    val release = runCatching { ReleaseFormat.decodeFromString<ReleaseJson>(json) }.getOrNull() ?: return null
    if (release.draft || release.prerelease) return null
    if (!Versions.isNewer(release.tag, currentVersion)) return null
    // Releases also carry the Wear OS tile ("muslimdaily-wear-…apk"); the phone must never take it.
    val apk = release.assets.firstOrNull {
        it.name.endsWith(".apk", ignoreCase = true) && !it.name.contains("-wear-", ignoreCase = true)
    } ?: return null
    if (!apk.url.startsWith("https://")) return null
    return AvailableUpdate(
        version = release.tag.removePrefix("v"),
        notes = release.body.orEmpty().trim(),
        apkUrl = apk.url,
        sizeBytes = apk.size,
    )
}
