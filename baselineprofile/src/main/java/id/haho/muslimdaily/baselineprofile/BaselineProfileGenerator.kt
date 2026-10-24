package id.haho.muslimdaily.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's own profiles, from the journeys people take most:
 * `ANDROID_SERIAL=<device> ./gradlew :app:generateSideloadBaselineProfile`. Prefer an emulator
 * with a location set; labels are the app's default Indonesian. The run installs over the app,
 * and `android.injected.androidTest.leaveApksInstalledAfterRun` in gradle.properties keeps it
 * (and its data) installed afterwards: without it Gradle uninstalls the app when the run ends.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Launch to Today: the startup profile, which also orders the dex for launch. */
    @Test
    fun startup() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Hari ini")), TIMEOUT)
    }

    /** Today scrolled, the dua library, one routine, memorization and settings. */
    @Test
    fun journeys() = rule.collect(packageName = PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Hari ini")), TIMEOUT)
        scrollDown()

        tab("Doa")
        scrollDown()
        device.findObject(By.text("Bangun tidur"))?.let {
            it.click()
            device.wait(Until.hasObject(By.textStartsWith("Mulai baca")), TIMEOUT)
            scrollDown()
            device.pressBack()
        }

        tab("Hafalan")
        tab("Pengaturan")
        scrollDown()
        tab("Hari ini")
    }

    private fun MacrobenchmarkScope.tab(label: String) {
        device.findObject(By.text(label))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollDown() {
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE = "id.haho.muslimdaily.debug"
        const val TIMEOUT = 5_000L
    }
}
