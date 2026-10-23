package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.routines.RoutineEditActions
import id.haho.muslimdaily.feature.routines.RoutineEditScreen
import id.haho.muslimdaily.feature.routines.RoutineEditState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1800dp-xhdpi")
class RoutineScreenshotTest {
    private val categories = DuaPreviewData.categories

    @Test
    fun libraryEmpty() = captureVariants("110-library-routines-empty") {
        DuaLibraryContent(categories, routines = emptyList()) {}
    }

    @Test
    fun libraryWithRoutines() = captureVariants("111-library-routines") {
        DuaLibraryContent(
            categories,
            routines = listOf(
                Routine("1", "Berangkat kerja", listOf("morning_01", "morning_02", "v1")),
                Routine("2", "Sebelum tidur", listOf("morning_01")),
            ),
        ) {}
    }

    @Test
    fun editor() = captureVariants("112-routine-editor") {
        RoutineEditScreen(
            RoutineEditState(isNew = true, name = "Berangkat kerja", items = DuaPreviewData.duas.take(3), categories = categories),
            onBack = {},
            actions = RoutineEditActions(),
        )
    }

    @Test
    fun picker() = captureVariants("113-routine-picker") {
        val event = categories.first().events.first().id
        RoutineEditScreen(
            RoutineEditState(
                isNew = true,
                name = "",
                items = DuaPreviewData.duas.take(1),
                picking = true,
                categories = categories,
                expanded = setOf(event),
                eventDuas = mapOf(event to DuaPreviewData.duas),
            ),
            onBack = {},
            actions = RoutineEditActions(),
        )
    }
}
