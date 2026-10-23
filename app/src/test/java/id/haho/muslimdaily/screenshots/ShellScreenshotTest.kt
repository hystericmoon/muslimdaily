package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.EmptyState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h860dp-xhdpi")
class ShellScreenshotTest {
    @Test
    fun emptyState() = captureVariants("00-empty-state") {
        EmptyState(R.drawable.ic_location_off, "Lokasi belum diatur", "Atur lokasi untuk menampilkan jadwal shalat.")
    }
}
