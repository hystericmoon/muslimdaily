package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.routines.RoutineEditActions
import id.haho.muslimdaily.feature.routines.RoutineEditScreen
import id.haho.muslimdaily.feature.routines.RoutineEditState
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
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

    /** The docked bars, as they sit on the bottom navigation. */
    @Test
    @Config(qualifiers = "id-w400dp-h300dp-xhdpi")
    fun dockedBars() = captureVariants("114-docked-bars") {
        androidx.compose.foundation.layout.Column {
            id.haho.muslimdaily.feature.duas.event.EventActionBar(count = 8, onStart = {}, onCustomize = {})
            androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(24.dp))
            id.haho.muslimdaily.feature.duas.components.PlayerBar(
                isPlaying = true, speed = 1f, repeat = false, enabled = true,
                onToggle = {}, onSpeed = {}, onRepeat = {},
            )
        }
    }
}

