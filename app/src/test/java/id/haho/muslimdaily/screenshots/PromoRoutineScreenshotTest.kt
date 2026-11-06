package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.routines.RoutineEditActions
import id.haho.muslimdaily.feature.routines.RoutineEditScreen
import id.haho.muslimdaily.feature.routines.RoutineEditState
import id.haho.muslimdaily.feature.run.RunActions
import id.haho.muslimdaily.feature.run.RunScreen
import id.haho.muslimdaily.feature.run.RunUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Routines (Rangkaian) for the promo, light theme only. The preview duas carry the neutral
 * typography sample, with the placeholder translation and transliteration blanked so no test
 * wording shows. `marketing/tools/prep_screens.py` turns the Arabic sample into empty boxes.
 * Indonesian: 170-174, English: the same names with "-en".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1800dp-xhdpi")
class PromoRoutineScreenshotTest {
    private fun clean(d: Dua) = d.copy(translationId = "", translationEn = "", transliteration = "")
    private val duas = (DuaPreviewData.duas + DuaPreviewData.dua("morning_05", 5) + DuaPreviewData.dua("morning_06", 6)).map(::clean)

    private val categoriesId = DuaPreviewData.categories.drop(1)
    private val categoriesEn = listOf(
        DuaCategory("adhkar", "Morning and evening adhkar", 3, listOf(DuaEvent("morning", "adhkar", "Morning adhkar", 1, 8), DuaEvent("evening", "adhkar", "Evening adhkar", 2, 6))),
    )

    private fun routines(names: List<String>) = listOf(
        Routine("1", names[0], listOf("morning_01", "morning_02", "v1")),
        Routine("2", names[1], listOf("morning_01")),
        Routine("3", names[2], duas.take(4).map { it.id }),
    )

    private val namesId = listOf("Berangkat kerja", "Sebelum tidur", "Dzikir pagi")
    private val namesEn = listOf("Leaving for work", "Before sleep", "Morning adhkar")

    private fun run(label: String, index: Int, counts: List<Int>) = RunUiState(
        eventLabel = label,
        duas = duas.take(3),
        items = duas.take(3).map { RunItem(it.id, it.count) },
        counts = counts,
        index = index,
        completed = false,
        prefs = ReadingPrefs(),
        autoAdvance = true,
        haptics = true,
    )

    private fun libraryShot(name: String, names: List<String>, cats: List<DuaCategory>) = captureLight(name) {
        DuaLibraryContent(cats, routines = routines(names), previews = duas.associateBy { it.id }) {}
    }

    private fun editorShot(name: String, routine: String, cats: List<DuaCategory>) = captureLight(name) {
        RoutineEditScreen(RoutineEditState(isNew = true, name = routine, items = duas.take(3), categories = cats), onBack = {}, actions = RoutineEditActions())
    }

    private fun pickerShot(name: String, cats: List<DuaCategory>) = captureLight(name) {
        RoutineEditScreen(
            RoutineEditState(isNew = true, name = "", items = duas.take(1), picking = true, categories = cats, expanded = setOf("morning"), eventDuas = mapOf("morning" to duas.take(4))),
            onBack = {},
            actions = RoutineEditActions(),
        )
    }

    @Test fun libraryId() = libraryShot("170-promo-routines-library", namesId, categoriesId)
    @Test fun editorId() = editorShot("171-promo-routines-editor", namesId[0], categoriesId)
    @Test fun pickerId() = pickerShot("172-promo-routines-picker", categoriesId)
    @Test @Config(qualifiers = "id-w400dp-h860dp-xhdpi")
    fun runId() = captureLight("173-promo-routines-run") { RunScreen(run("Dzikir pagi", 1, listOf(1, 2, 0)), RunActions()) }
    @Test @Config(qualifiers = "id-w400dp-h860dp-xhdpi")
    fun summaryId() = captureLight("174-promo-routines-summary") { RunScreen(run("Dzikir pagi", 3, listOf(1, 3, 1)), RunActions()) }

    @Test @Config(qualifiers = "en-w400dp-h1800dp-xhdpi")
    fun libraryEn() = libraryShot("170-promo-routines-library-en", namesEn, categoriesEn)
    @Test @Config(qualifiers = "en-w400dp-h1800dp-xhdpi")
    fun editorEn() = editorShot("171-promo-routines-editor-en", namesEn[0], categoriesEn)
    @Test @Config(qualifiers = "en-w400dp-h1800dp-xhdpi")
    fun pickerEn() = pickerShot("172-promo-routines-picker-en", categoriesEn)
    @Test @Config(qualifiers = "en-w400dp-h860dp-xhdpi")
    fun runEn() = captureLight("173-promo-routines-run-en") { RunScreen(run("Morning adhkar", 1, listOf(1, 2, 0)), RunActions()) }
    @Test @Config(qualifiers = "en-w400dp-h860dp-xhdpi")
    fun summaryEn() = captureLight("174-promo-routines-summary-en") { RunScreen(run("Morning adhkar", 3, listOf(1, 3, 1)), RunActions()) }
}
