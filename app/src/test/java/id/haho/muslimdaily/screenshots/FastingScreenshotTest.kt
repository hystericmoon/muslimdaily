package id.haho.muslimdaily.screenshots

import androidx.compose.ui.res.stringResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.domain.fasting.FastingCalendar
import id.haho.muslimdaily.feature.fasting.FastingContent
import id.haho.muslimdaily.feature.fasting.FastingUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h2000dp-xhdpi")
class FastingScreenshotTest {
    private fun state(today: LocalDate, selected: LocalDate = today) = FastingUiState(
        today = FastingCalendar.day(today, 0),
        tomorrow = FastingCalendar.day(today.plusDays(1), 0),
        month = FastingCalendar.month(today, 0),
        selected = FastingCalendar.day(selected, 0),
        upcoming = FastingCalendar.upcoming(today, 0, includeWeekly = false, limit = 6),
    )

    private fun shot(name: String, s: FastingUiState) = captureVariants(name) {
        AdwamScreen(title = stringResource(R.string.fasting_title), onBack = {}) { FastingContent(s, {}, {}, {}) }
    }

    @Test fun ordinaryMonth() = shot("96-fasting-month", state(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 25)))

    /** Zulhijah 1448: Arafah, Idul Adha and the tasyriq days. */
    @Test fun dhulHijjah() = shot("97-fasting-dhul-hijjah", state(LocalDate.of(2027, 5, 15)))
}
