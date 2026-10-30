package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.domain.myset.FormChoice
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.run.RunActions
import id.haho.muslimdaily.feature.run.RunScreen
import id.haho.muslimdaily.feature.run.RunUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h860dp-xhdpi")
class MySetScreenshotTest {
    private val duas = DuaPreviewData.duas + DuaPreviewData.dua("morning_05", 5) + DuaPreviewData.dua("morning_06", 6)

    private fun run(index: Int, counts: List<Int>) = RunUiState(
        eventLabel = "Dzikir pagi",
        duas = duas.take(3),
        items = duas.take(3).map { RunItem(it.id, it.count) },
        counts = counts,
        index = index,
        completed = false,
        prefs = ReadingPrefs(),
        autoAdvance = true,
        haptics = true,
    )

    @Test
    fun runCounting() = captureVariants("31-run-counting") { RunScreen(run(1, listOf(1, 2, 0)), RunActions()) }

    @Test
    fun runCardDone() = captureVariants("32-run-card-done") { RunScreen(run(1, listOf(1, 3, 0)), RunActions()) }

    @Test
    fun runSummary() = captureVariants("33-run-summary") { RunScreen(run(3, listOf(1, 3, 1)), RunActions()) }

    @Test
    fun runFromMemory() = captureVariants("123-run-from-memory") {
        RunScreen(run(1, listOf(1, 0, 0)).copy(hifz = setOf(duas[1].id), fromMemory = true), RunActions())
    }
}
