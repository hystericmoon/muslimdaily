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

/**
 * The island's prayer arc card and prayer alert as the overlay draws them, in light, dark and at
 * 200% text. The travelling light is off here (it is an endless animation, which a still shot cannot
 * show); it is a still edge when animations are off, and checked on a device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class IslandScreenshotTest {
    private val names = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
    private val minutes = listOf(245, 701, 903, 1084, 1147)
    private val ticks = minutes.map { (it - minutes.first()) / (minutes.last() - minutes.first()).toFloat() }

    private fun texts(
        name: String = "Maghrib",
        clock: String = "18.04",
        left: String = "7 mnt",
        next: Int = 3,
        progress: Float = (1077 - 245) / 902f,
        tomorrow: String? = null,
        times: List<String> = listOf("04.05", "11.41", "15.03", "18.04", "19.07"),
    ) = IslandTexts(
        prayerName = name,
        clock = clock,
        left = left,
        pillRight = left,
        place = "Jakarta",
        openLabel = "Buka Adwam",
        dismissLabel = "Tutup",
        description = "$name pukul $clock, $left lagi.",
        cardDescription = "$name pukul $clock, $left lagi.",
        arcNames = names,
        arcTimes = times,
        arcTicks = ticks,
        arcNext = next,
        arcProgress = progress,
        tomorrow = tomorrow,
        alertProgress = 0.3f,
        alertStart = "Terbuka 17.54",
    )

    private val config = IslandSettings(glow = false)

    private fun shot(name: String, mode: IslandMode, t: IslandTexts) = captureVariants(name) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
            IslandOverlay(t, config, gapDp = 40, mode = mode, onToggle = {}, onOpen = {})
        }
    }

    @Test fun arcCard() = shot("202-island-card", IslandMode.CARD, texts())

    @Test fun alert() = shot("203-island-big-card", IslandMode.BIG, texts())

    @Test fun arcBeforeSubuh() = shot(
        "204-island-card-before-subuh", IslandMode.CARD,
        texts(name = "Subuh", clock = "04.05", left = "55 mnt", next = 0, progress = 0f),
    )

    @Test fun arcAfterIsya() = shot(
        "205-island-card-after-isya", IslandMode.CARD,
        texts(
            name = "Subuh", clock = "04.06", left = "7 j 36 mnt", next = 0, progress = 1f, tomorrow = "besok",
            times = listOf("04.06", "11.41", "15.03", "18.04", "19.07"),
        ),
    )
}
