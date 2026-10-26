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

/**
 * Dotted versions with an optional pre-release suffix, ordered like SemVer:
 * 0.6.0-beta.1 < 0.6.0-beta.2 < 0.6.0 < 0.6.1.
 */
internal object Versions {
    fun parse(raw: String): List<Int>? {
        val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        if (core.isEmpty()) return null
        return core.split('.').map { it.toIntOrNull() ?: return null }
    }

    /** "beta.2" → ["beta", "2"]; empty for a final release. */
    private fun preRelease(raw: String): List<String> =
        raw.trim().substringBefore('+').substringAfter('-', "").split('.').filter { it.isNotEmpty() }

    fun isPreRelease(raw: String): Boolean = preRelease(raw).isNotEmpty()

    /** True when [candidate] is strictly newer than [current]. Unparseable versions are never newer. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return true
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        // Same numbers: a final release beats its pre-releases; pre-releases compare by part.
        val pa = preRelease(candidate)
        val pb = preRelease(current)
        if (pa.isEmpty() || pb.isEmpty()) return pa.isEmpty() && pb.isNotEmpty()
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrNull(i) ?: return false
            val y = pb.getOrNull(i) ?: return true
            if (x == y) continue
            val nx = x.toIntOrNull()
            val ny = y.toIntOrNull()
            return when {
                nx != null && ny != null -> nx > ny
                nx != null -> false
                ny != null -> true
                else -> x > y
            }
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
    if (release.prerelease) return null
    return installable(release, currentVersion)
}

/**
 * Beta channel: from GitHub's release list, the newest installable release, pre-releases
 * included. A tester on a beta still moves to the final release once it is out.
 */
internal fun parseNewestRelease(json: String, currentVersion: String): AvailableUpdate? {
    val releases = runCatching { ReleaseFormat.decodeFromString<List<ReleaseJson>>(json) }.getOrNull() ?: return null
    return releases.mapNotNull { installable(it, currentVersion) }
        .reduceOrNull { best, next -> if (Versions.isNewer(next.version, best.version)) next else best }
}

private fun installable(release: ReleaseJson, currentVersion: String): AvailableUpdate? {
    if (release.draft) return null
    if (!Versions.isNewer(release.tag, currentVersion)) return null
    // Releases up to 0.9.x also carried a Wear OS APK ("…-wear-…apk"); the phone must never take it.
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
