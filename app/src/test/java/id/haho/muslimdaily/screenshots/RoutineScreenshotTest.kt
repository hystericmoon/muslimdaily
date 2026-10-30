package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.routine.Routine
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.duas.library.LibraryTab
import id.haho.muslimdaily.feature.routines.RoutineEditActions
import id.haho.muslimdaily.feature.routines.RoutineEditScreen
import id.haho.muslimdaily.feature.routines.RoutineEditState
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.feature.today.TodayScreen
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

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
                Routine("3", "Dzikir pagi", DuaPreviewData.duas.map { it.id }),
            ),
            previews = DuaPreviewData.duas.associateBy { it.id },
        ) {}
    }

    /** The library tab: one category open, listing its occasions. */
    @Test
    fun libraryAllOpen() = captureVariants("132-library-all-open") {
        DuaLibraryContent(
            categories,
            routines = listOf(Routine("1", "Berangkat kerja", listOf("morning_01"))),
            tab = LibraryTab.ALL,
            expanded = setOf(categories.first().id),
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

    @Test
    fun pickerSearch() = captureVariants("124-routine-picker-search") {
        RoutineEditScreen(
            RoutineEditState(
                isNew = true,
                name = "",
                items = DuaPreviewData.duas.take(1),
                picking = true,
                categories = categories,
                query = "tidur",
                results = DuaPreviewData.duas,
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

    @Test
    @Config(qualifiers = "id-w400dp-h900dp-xhdpi")
    fun whatsNew() = captureVariants("115-whats-new") {
        id.haho.muslimdaily.feature.whatsnew.WhatsNewDialog(
            id.haho.muslimdaily.feature.whatsnew.WhatsNew.since("0.5.2", "0.6.0", firstRun = false),
        ) {}
    }

    @Test
    fun duaWithNote() = captureVariants("116-dua-note") {
        id.haho.muslimdaily.feature.duas.detail.DuaDetailContent(
            id.haho.muslimdaily.feature.duas.detail.DuaDetailState(
                DuaPreviewData.duas.first(), null,
                id.haho.muslimdaily.feature.duas.components.ReadingPrefs(),
                id.haho.muslimdaily.audio.PlaybackState(), inMySet = true,
                note = "Dibaca bersama anak-anak setiap pagi sebelum berangkat sekolah.",
            ),
            id.haho.muslimdaily.feature.duas.detail.DuaDetailActions(),
        )
    }

    // Today's timed sections, each shown only while its window runs (Jakarta, Wed 21 Oct 2026).

    @Test
    @Config(qualifiers = "id-w400dp-h2400dp-xhdpi")
    fun todayDhuha() = captureVariants("118-today-dhuha") {
        TodayScreen(TodayPreviewData.ready(at = LocalDateTime.of(2026, 10, 21, 8, 30), checked = setOf("fard:FAJR")), TodayActions())
    }

    @Test
    @Config(qualifiers = "id-w400dp-h2400dp-xhdpi")
    fun todayWitr() = captureVariants("119-today-witr") {
        TodayScreen(
            TodayPreviewData.ready(at = LocalDateTime.of(2026, 10, 21, 20, 30), checked = setOf("fard:ISHA"), progress = emptyMap()),
            TodayActions(),
            initiallyExpanded = setOf("ISHA"),
        )
    }

    /** No window running: only the next prayer and the schedule. */
    @Test
    @Config(qualifiers = "id-w400dp-h2400dp-xhdpi")
    fun todayQuiet() = captureVariants("120-today-quiet") {
        TodayScreen(TodayPreviewData.ready(at = LocalDateTime.of(2026, 10, 21, 13, 0), checked = emptySet(), progress = emptyMap()), TodayActions())
    }
}
