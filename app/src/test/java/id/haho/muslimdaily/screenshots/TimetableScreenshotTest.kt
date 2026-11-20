package id.haho.muslimdaily.screenshots

import android.content.Context
import android.graphics.Canvas
import android.provider.Settings
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import id.haho.design.theme.HahoPalette
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AdwamScreen
import id.haho.muslimdaily.data.settings.CalcMethod
import id.haho.muslimdaily.data.settings.CalculationSettings
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.Timetable
import id.haho.muslimdaily.feature.timetable.TimetableContent
import id.haho.muslimdaily.feature.timetable.TimetableMode
import id.haho.muslimdaily.feature.timetable.TimetableShare
import id.haho.muslimdaily.feature.timetable.TimetableUiState
import id.haho.muslimdaily.feature.timetable.toSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h2400dp-xhdpi")
class TimetableScreenshotTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val calculator = PrayerCalculator()
    private val schedule = { d: LocalDate -> calculator.schedule(d, -6.2146, 106.8451, zone, CalculationSettings()) }

    private fun month(today: LocalDate) = TimetableUiState.Ready(
        TimetableMode.MONTH, YearMonth.from(today), zone, "Jakarta", R.string.method_kemenag,
        Timetable.month(YearMonth.from(today), 0, schedule), today,
    )

    private fun ramadan(today: LocalDate) = TimetableUiState.Ready(
        TimetableMode.RAMADAN, YearMonth.from(today), zone, "Jakarta", R.string.method_kemenag,
        checkNotNull(Timetable.ramadan(today, 0, schedule)), today,
    )

    private fun shot(name: String, s: TimetableUiState.Ready) = captureVariants(name) {
        AdwamScreen(title = stringResource(R.string.timetable_title), onBack = {}) { TimetableContent(s, {}, {}, {}) }
    }

    @Test fun month() = shot("199-timetable-month", month(LocalDate.of(2026, 11, 19)))

    /** Ramadan 1448 starts around 8 February 2027. */
    @Test fun ramadan() = shot("200-timetable-ramadan", ramadan(LocalDate.of(2027, 2, 12)))

    /** The shared image: what a reader forwards, drawn at the width the share uses. */
    @Test fun sharedImage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
        val sheet = ramadan(LocalDate.of(2027, 2, 12)).toSheet(context)
        val height = 190 + 64 + sheet.rows.size * 60 + 96
        val bmp = createBitmap(1080, height)
        TimetableShare.render(context, sheet, HahoPalette.FOREST, 1080, height, Canvas(bmp))
        assertTrue(sheet.rows.size in 29..30)
        bmp.captureRoboImage("201-timetable-share.png")
    }
}
