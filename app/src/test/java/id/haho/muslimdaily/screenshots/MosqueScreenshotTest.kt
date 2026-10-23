package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.model.Prayer
import id.haho.muslimdaily.data.mosque.Mosque
import id.haho.muslimdaily.domain.prayer.NextPrayer
import id.haho.muslimdaily.feature.mosque.MosqueActions
import id.haho.muslimdaily.feature.mosque.MosqueHeader
import id.haho.muslimdaily.feature.mosque.MosqueUiState
import id.haho.muslimdaily.feature.mosque.NearbyMosqueContent
import id.haho.muslimdaily.feature.mosque.Origin
import androidx.compose.ui.res.stringResource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h860dp-xhdpi")
class MosqueScreenshotTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = LocalDateTime.of(2026, 10, 21, 11, 31).atZone(zone).toInstant()
    private val header = MosqueHeader(NextPrayer(Prayer.DHUHR, LocalDateTime.of(2026, 10, 21, 11, 41).atZone(zone).toInstant(), null), zone, now)
    private val list = listOf(
        Mosque("Masjid Al-Ikhlas", -6.2141, 106.8455, 120),
        Mosque("", -6.2150, 106.8440, 260),
        Mosque("Musholla Nurul Huda", -6.2160, 106.8470, 540),
        Mosque("Masjid Jami' Baitul Mukarram Kebon Kacang Tanah Abang", -6.2100, 106.8400, 1280),
        Mosque("Masjid Raya", -6.2000, 106.8300, 2350),
    )

    private fun shot(name: String, state: MosqueUiState) = captureVariants(name) {
        AppScreen(title = stringResource(R.string.mosque_title), onBack = {}) {
            NearbyMosqueContent(state, header, canOpenMaps = true, actions = MosqueActions())
        }
    }

    @Test fun precise() = shot("90-mosque-precise", MosqueUiState.Ready(Origin.Precise(-6.2146, 106.8451, 12), list))

    @Test fun approximate() = shot("91-mosque-approximate", MosqueUiState.Ready(Origin.Approximate(-6.2146, 106.8451), list))

    @Test fun saved() = shot("92-mosque-saved-empty", MosqueUiState.Ready(Origin.Saved(-6.2146, 106.8451, "Jakarta"), emptyList()))

    @Test fun permission() = shot("93-mosque-permission", MosqueUiState.NeedsPermission("Jakarta"))
}
