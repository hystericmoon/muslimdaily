package id.haho.muslimdaily.screenshots

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.core.designsystem.theme.ThemeMode
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.widget.DayScheduleContent
import id.haho.muslimdaily.widget.NextPrayerContent
import id.haho.muslimdaily.widget.WidgetColors
import id.haho.muslimdaily.widget.WidgetData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Renders the home-screen widgets through Glance's RemoteViews translation (what launchers get),
 * over a wallpaper-like gradient, in light and dark and a few palettes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-xhdpi")
@OptIn(ExperimentalGlanceApi::class)
class WidgetScreenshotTest {
    private val now = LocalDateTime.of(2026, 10, 21, 10, 0).atZone(ZoneId.of("Asia/Jakarta")).toInstant()

    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 10, 21, h, m).atZone(ZoneId.of("Asia/Jakarta")).toInstant()

    private fun data(palette: AppPalette, mode: ThemeMode, now: java.time.Instant = this.now): WidgetData {
        val settings = TodayPreviewData.settings.copy(palette = palette, themeMode = mode)
        return WidgetData(PrayerSnapshots.at(now, settings, PrayerCalculator()), "10 Jumadilawal 1448 H", WidgetColors.of(settings), TimeFormatter(true), now)
    }

    private class Fake(private val body: @androidx.compose.runtime.Composable () -> Unit) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Single
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { body() }
    }

    private suspend fun render(name: String, size: DpSize, dark: Boolean, body: @androidx.compose.runtime.Composable () -> Unit) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(base.contentResolver, Settings.System.TIME_12_24, "24")
        val config = Configuration(base.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context = base.createConfigurationContext(config)
        val views = Fake(body).compose(context, size = size)
        val density = context.resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        val host = FrameLayout(context)
        val view = views.apply(context, host)
        host.addView(view, FrameLayout.LayoutParams(w, h))
        host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val wallpaper = if (dark) intArrayOf(0xFF0B1F2A.toInt(), 0xFF3A2245.toInt()) else intArrayOf(0xFF8EC5FC.toInt(), 0xFFE0C3FC.toInt())
        canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), wallpaper[0], wallpaper[1], Shader.TileMode.CLAMP) })
        canvas.translate(pad.toFloat(), pad.toFloat())
        host.draw(canvas)
        File("../docs/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun widgets() = runTest {
        // A day of skies: before dawn, dawn, morning, noon, Ashar, dusk, night.
        val times = listOf("0330" to at(3, 30), "0440" to at(4, 40), "0900" to at(9, 0), "1300" to at(13, 0), "1630" to at(16, 30), "1810" to at(18, 10), "2100" to at(21, 0))
        for ((label, t) in times) {
            val d = data(AppPalette.TEAL, ThemeMode.SYSTEM, t)
            render("80-widget-next-$label", DpSize(170.dp, 170.dp), false) { NextPrayerContent(d) }
            render("82-widget-day-$label", DpSize(340.dp, 200.dp), false) { DayScheduleContent(d) }
        }
        val d = data(AppPalette.TEAL, ThemeMode.SYSTEM, at(10, 0))
        render("81-widget-next-compact", DpSize(250.dp, 70.dp), false) { NextPrayerContent(d) }
        render("83-widget-day-short", DpSize(340.dp, 140.dp), false) { DayScheduleContent(d) }
        val none = data(AppPalette.ROSE, ThemeMode.LIGHT).let { WidgetData(null, it.hijri, it.colors, it.time, it.now) }
        render("84-widget-no-location", DpSize(170.dp, 170.dp), false) { NextPrayerContent(none) }
    }
}
