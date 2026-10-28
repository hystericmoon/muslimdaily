package id.haho.muslimdaily.screenshots

import androidx.compose.ui.res.stringResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.feature.qibla.QiblaContent
import id.haho.muslimdaily.feature.qibla.QiblaUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h860dp-xhdpi")
class QiblaScreenshotTest {
    private fun shot(name: String, state: QiblaUiState) = captureVariants(name) {
        AdwamScreen(title = stringResource(R.string.qibla_title), onBack = {}) { QiblaContent(state) {} }
    }

    @Test fun turning() = shot("94-qibla-turn", QiblaUiState.Ready("Jakarta", 295.15, 250.0, reliable = false))

    @Test fun aligned() = shot("95-qibla-aligned", QiblaUiState.Ready("Jakarta", 295.15, 294.0, reliable = true))
}
