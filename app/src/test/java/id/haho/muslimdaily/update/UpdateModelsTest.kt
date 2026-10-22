package id.haho.muslimdaily.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelsTest {

    @Test fun comparesNumerically() {
        assertTrue(Versions.isNewer("v0.10.0", "0.9.9"))
        assertTrue(Versions.isNewer("0.2.0", "0.1.1"))
        assertTrue(Versions.isNewer("1.0", "0.9.9"))
        assertFalse(Versions.isNewer("v0.2.0", "0.2.0"))
        assertFalse(Versions.isNewer("0.2", "0.2.0"))
        assertFalse(Versions.isNewer("0.1.9", "0.2.0"))
    }

    @Test fun ignoresSuffixesAndRejectsGarbage() {
        assertEquals(listOf(0, 3, 0), Versions.parse("v0.3.0-beta.1"))
        assertNull(Versions.parse("latest"))
        assertFalse(Versions.isNewer("nightly", "0.1.0"))
    }

    private fun release(tag: String, assets: String, extra: String = "") =
        """{"tag_name":"$tag","body":"Catatan","html_url":"x",$extra"assets":[$assets]}"""

    private val apk = """{"name":"muslimdaily-0.3.0.apk","browser_download_url":"https://github.com/o/r/releases/download/v0.3.0/a.apk","size":123}"""

    @Test fun parsesNewerRelease() {
        val u = parseLatestRelease(release("v0.3.0", apk), "0.2.0")!!
        assertEquals("0.3.0", u.version)
        assertEquals(123L, u.sizeBytes)
        assertEquals("Catatan", u.notes)
    }

    @Test fun skipsSameOlderPrereleaseOrWithoutApk() {
        assertNull(parseLatestRelease(release("v0.2.0", apk), "0.2.0"))
        assertNull(parseLatestRelease(release("v0.1.0", apk), "0.2.0"))
        assertNull(parseLatestRelease(release("v0.3.0", apk, "\"prerelease\":true,"), "0.2.0"))
        assertNull(parseLatestRelease(release("v0.3.0", """{"name":"notes.txt","browser_download_url":"https://x/notes.txt"}"""), "0.2.0"))
        assertNull(parseLatestRelease(release("v0.3.0", """{"name":"a.apk","browser_download_url":"http://x/a.apk"}"""), "0.2.0"))
        assertNull(parseLatestRelease("not json", "0.2.0"))
    }
}
