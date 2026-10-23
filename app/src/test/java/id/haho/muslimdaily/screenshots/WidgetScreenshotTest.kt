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
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.feature.today.TodayPreviewData
import id.haho.muslimdaily.widget.PrayerWidgetContent
import id.haho.muslimdaily.widget.WidgetData
import id.haho.muslimdaily.widget.WidgetModels
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Renders the prayer-times widget through Glance's RemoteViews translation (what launchers get),
 * over a wallpaper-like gradient: every size from 1×1 to 5×3, the 4×2 through the day, at 200%
 * system font, in a short landscape row, and with no location yet. Friday 23 October 2026,
 * Jakarta: Dhuhr is Jumu'ah and tomorrow is the first of the white days.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-xhdpi")
@OptIn(ExperimentalGlanceApi::class)
class WidgetScreenshotTest {
    private fun at(h: Int, m: Int): Instant = LocalDateTime.of(2026, 10, 23, h, m).atZone(ZoneId.of("Asia/Jakarta")).toInstant()

    private fun data(now: Instant, located: Boolean = true): WidgetData {
        val settings = TodayPreviewData.settings.let { if (located) it else it.copy(location = null) }
        return WidgetData(WidgetModels.at(now, settings, PrayerCalculator()), TimeFormatter(true), now)
    }

    private class Fake(private val body: @androidx.compose.runtime.Composable () -> Unit) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Single
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { body() }
    }

    private suspend fun render(name: String, size: DpSize, data: WidgetData, fontScale: Float = 1f) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(base.contentResolver, Settings.System.TIME_12_24, "24")
        val config = Configuration(base.resources.configuration).apply { this.fontScale = fontScale }
        val context = base.createConfigurationContext(config)
        val views = Fake { PrayerWidgetContent(data) }.compose(context, size = size)
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
        canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), 0xFF8EC5FC.toInt(), 0xFFE0C3FC.toInt(), Shader.TileMode.CLAMP) })
        canvas.translate(pad.toFloat(), pad.toFloat())
        host.draw(canvas)
        File("../docs/screenshots").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val widths = listOf(76, 164, 252, 340, 428)
    private val heights = listOf(76, 164, 252)

    @Test
    fun widgets() = runTest {
        val morning = data(at(8, 30))
        heights.forEachIndexed { r, h ->
            widths.forEachIndexed { c, w -> render("80-widget-${c + 1}x${r + 1}", DpSize(w.dp, h.dp), morning) }
        }
        for ((label, t) in listOf("0445" to at(4, 45), "1135" to at(11, 35), "1730" to at(17, 30), "1820" to at(18, 20), "2100" to at(21, 0))) {
            render("81-widget-4x2-$label", DpSize(340.dp, 164.dp), data(t))
        }
        render("82-widget-4x2-font200", DpSize(340.dp, 164.dp), morning, fontScale = 2f)
        render("83-widget-4x1-landscape", DpSize(340.dp, 56.dp), morning)
        render("84-widget-no-location", DpSize(164.dp, 164.dp), data(at(8, 30), located = false))
    }
}
