package id.haho.muslimdaily.screenshots

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import id.haho.muslimdaily.widget.WidgetTestFont
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Renders the prayer-times widget through Glance's RemoteViews translation (what launchers get),
 * over a wallpaper-like gradient: every size from 1×1 to 5×3, the 4×2 through the day, at 200%
 * system font, in a short landscape row, Samsung's taller 4×2, and with no location yet. Friday 23 October 2026,
 * Jakarta: Dhuhr is Jumu'ah and tomorrow is the first of the white days.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-xhdpi")
@OptIn(ExperimentalGlanceApi::class)
class WidgetScreenshotTest {
    @Before
    fun widgetFont() = WidgetTestFont.install(ApplicationProvider.getApplicationContext())

    private fun at(h: Int, m: Int): Instant = LocalDateTime.of(2026, 10, 23, h, m).atZone(ZoneId.of("Asia/Jakarta")).toInstant()

    private fun data(now: Instant, located: Boolean = true, is24: Boolean = true): WidgetData {
        val settings = TodayPreviewData.settings.let { if (located) it else it.copy(location = null) }
        return WidgetData(WidgetModels.at(now, settings, PrayerCalculator()), TimeFormatter(is24), now)
    }

    private class Fake(private val body: @androidx.compose.runtime.Composable () -> Unit) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Single
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { body() }
    }

    /**
     * [preview] renders for the widget picker instead: no wallpaper, clipped to the widget's
     * corners (Robolectric does not apply the sky's own rounding), into the app's resources.
     */
    private suspend fun render(name: String, size: DpSize, data: WidgetData, fontScale: Float = 1f, preview: Boolean = false, locale: Locale? = null) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        Settings.System.putString(base.contentResolver, Settings.System.TIME_12_24, "24")
        val config = Configuration(base.resources.configuration).apply {
            this.fontScale = fontScale
            locale?.let { setLocale(it) }
        }
        val context = base.createConfigurationContext(config)
        val views = Fake { PrayerWidgetContent(data) }.compose(context, size = size)
        val density = context.resources.displayMetrics.density
        val pad = if (preview) 0 else (16 * density).toInt()
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        val host = FrameLayout(context)
        val view = views.apply(context, host)
        host.addView(view, FrameLayout.LayoutParams(w, h))
        host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (preview) {
            val corner = 24 * density
            canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), corner, corner, Path.Direction.CW) })
        } else {
            canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), 0xFF8EC5FC.toInt(), 0xFFE0C3FC.toInt(), Shader.TileMode.CLAMP) })
        }
        canvas.translate(pad.toFloat(), pad.toFloat())
        host.draw(canvas)
        val out = if (preview) File("src/main/res/drawable-nodpi") else File("../docs/screenshots").apply { mkdirs() }
        out.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
        render("widget_preview", DpSize(340.dp, 164.dp), morning, preview = true)
        // After Isya: today's date stays, the countdown runs to tomorrow's Subuh.
        render("85-widget-2x2-2100", DpSize(164.dp, 164.dp), data(at(21, 0)))
        // A Samsung-style 5×2 in English with 12-hour times: no "11:41 …".
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            render("86-widget-5x2-en-12h-2100", DpSize(428.dp, 164.dp), data(at(21, 0), is24 = false), locale = Locale.ENGLISH)
            // Samsung's grid gives tall cells: the times use the height instead of floating in it.
            render("87-widget-5x3-tall-en-12h", DpSize(412.dp, 360.dp), data(at(8, 30), is24 = false), locale = Locale.ENGLISH)
            render("88-widget-4x3-tall-en-12h", DpSize(330.dp, 240.dp), data(at(8, 30), is24 = false), locale = Locale.ENGLISH)
            render("89-widget-1x3-tall-en-12h", DpSize(84.dp, 240.dp), data(at(8, 30), is24 = false), locale = Locale.ENGLISH)
        } finally {
            Locale.setDefault(default)
        }
        // Samsung's 4×2 is taller than Pixel's but still two rows: the scale's names and the footer fit.
        render("90-widget-4x2-samsung", DpSize(352.dp, 200.dp), morning)
    }
}
