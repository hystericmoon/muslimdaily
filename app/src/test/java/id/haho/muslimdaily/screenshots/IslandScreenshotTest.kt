package id.haho.muslimdaily.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.settings.IslandSettings
import id.haho.muslimdaily.island.IslandMode
import id.haho.muslimdaily.island.IslandOverlay
import id.haho.muslimdaily.island.IslandTexts
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The island's card and prayer alert (the big card) as the overlay draws them, with the swipe handle. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h300dp-xhdpi")
class IslandScreenshotTest {
    private val texts = IslandTexts(
        prayerName = "Maghrib",
        clock = "18.04",
        left = "7 mnt",
        pillRight = "7 mnt",
        place = "Jakarta",
        openLabel = "Buka Adwam",
        dismissLabel = "Tutup",
        description = "Maghrib pukul 18.04, 7 mnt lagi. Ketuk untuk detail.",
        cardDescription = "Maghrib pukul 18.04, 7 mnt lagi. Ketuk untuk membuka Adwam, geser untuk menutup.",
    )

    private fun shot(name: String, mode: IslandMode) = captureLight(name) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
            IslandOverlay(texts, IslandSettings(), gapDp = 40, mode = mode, onToggle = {}, onOpen = {})
        }
    }

    @Test fun card() = shot("202-island-card", IslandMode.CARD)

    @Test fun alert() = shot("203-island-big-card", IslandMode.BIG)
}
