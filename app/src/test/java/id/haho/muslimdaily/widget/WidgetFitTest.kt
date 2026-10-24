package id.haho.muslimdaily.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.text.Layout
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

/**
 * Every widget size, in Indonesian and English, 24- and 12-hour, through the day (after Isya the
 * date reads "Besok · …") and at larger system fonts: no time, prayer name or countdown may end
 * in "…", run past its view or the widget, or touch another text. Dates and places may shorten
 * with "…" by design.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
@OptIn(ExperimentalGlanceApi::class)
class WidgetFitTest {
    private val default = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(default)

    private class Fake(private val data: WidgetData) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Single
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { PrayerWidgetContent(data) }
    }

    private data class Case(val locale: Locale, val is24: Boolean, val hour: Int, val minute: Int, val size: DpSize, val fontScale: Float) {
        override fun toString() = "${locale.language} ${if (is24) "24h" else "12h"} %02d:%02d ${size.width.value.toInt()}×${size.height.value.toInt()}dp font×$fontScale".format(hour, minute)
    }

    // The launcher grid's sizes, plus larger cells (tablets, Samsung's 5-column grid) where the type scales up.
    private val widths = listOf(76, 110, 164, 195, 252, 290, 340, 400, 428, 520)
    private val heights = listOf(56, 76, 150, 164, 215, 230, 250, 252, 330)
    private val times = listOf(4 to 45, 8 to 30, 11 to 35, 17 to 30, 19 to 5, 21 to 0, 23 to 50)
    private val indonesian = Locale.forLanguageTag("id-ID")

    @Test fun indonesian24Hour() = everyTextFits(indonesian, is24 = true)

    @Test fun indonesian12Hour() = everyTextFits(indonesian, is24 = false)

    @Test fun english24Hour() = everyTextFits(Locale.ENGLISH, is24 = true)

    @Test fun english12Hour() = everyTextFits(Locale.ENGLISH, is24 = false)

    // Each locale renders ~1,900 widgets: more than runTest's default minute on a busy machine.
    private fun everyTextFits(locale: Locale, is24: Boolean) = runTest(timeout = 5.minutes) {
        val problems = mutableListOf<String>()
        for ((h, m) in times) for (w in widths) for (ht in heights) {
            for (fontScale in listOf(1f, 1.3f, 2f)) problems += check(Case(locale, is24, h, m, DpSize(w.dp, ht.dp), fontScale))
        }
        assertTrue("${problems.size} problems:\n" + problems.distinct().take(60).joinToString("\n"), problems.isEmpty())
    }

    private suspend fun check(case: Case): List<String> {
        Locale.setDefault(case.locale)
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply {
            setLocale(case.locale)
            fontScale = case.fontScale
        }
        val context = base.createConfigurationContext(config)
        val now = LocalDateTime.of(2026, 10, 23, case.hour, case.minute).atZone(ZoneId.of("Asia/Jakarta")).toInstant()
        val data = WidgetData(WidgetModels.at(now, TodayPreviewData.settings, PrayerCalculator()), TimeFormatter(case.is24), now)
        val model = data.model!!
        val text = WidgetText(context, model, data.time)
        // What must always show in full: the times, the prayer names and the countdown.
        val whole = (model.slots.map { text.time(it.time) } + model.slots.map { clockDigits(text.time(it.time)) } + model.slots.map { text.name(it.slot) }).toSet()

        val views = Fake(data).compose(context, size = case.size)
        val density = context.resources.displayMetrics.density
        val w = (case.size.width.value * density).toInt()
        val h = (case.size.height.value * density).toInt()
        val host = FrameLayout(context)
        host.addView(views.apply(context, host), FrameLayout.LayoutParams(w, h))
        host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w, h)

        val texts = mutableListOf<Pair<TextView, Rect>>()
        collect(host, texts)
        val problems = mutableListOf<String>()
        for ((view, rect) in texts) {
            val s = view.text.toString()
            val mustShow = view is Chronometer || s in whole || whole.any { it.isNotEmpty() && s.startsWith(it) }
            if (!mustShow) continue
            val ellipsized = view.layout?.let { l -> (0 until l.lineCount).any { l.getEllipsisCount(it) > 0 } } == true
            val needed = Layout.getDesiredWidth(view.text, view.paint)
            val room = view.width - view.totalPaddingLeft - view.totalPaddingRight
            when {
                ellipsized -> problems += "$case: \"$s\" ends in …"
                needed > room + 1 -> problems += "$case: \"$s\" needs ${(needed / density).toInt()}dp, has ${(room / density).toInt()}dp"
                rect.left < 0 || rect.top < 0 || rect.right > w || rect.bottom > h -> problems += "$case: \"$s\" runs past the widget"
            }
        }
        // Texts on the same line must not touch: "5:23 AM11:41" reads as one number.
        val shown = texts.filter { (v, _) -> v.text.isNotBlank() }
        for (i in shown.indices) for (j in i + 1 until shown.size) {
            val (a, ra) = shown[i]
            val (b, rb) = shown[j]
            val ia = inked(a, ra)
            val ib = inked(b, rb)
            if (Rect.intersects(ia, ib)) problems += "$case: \"${a.text}\" touches \"${b.text}\""
        }
        return problems
    }

    /** Visible TextViews with their bounds in the widget, clipped by every parent as the launcher would. */
    private fun collect(view: View, out: MutableList<Pair<TextView, Rect>>, clip: Rect = Rect(0, 0, view.width, view.height), x: Int = 0, y: Int = 0) {
        if (view.visibility != View.VISIBLE) return
        val bounds = Rect(x, y, x + view.width, y + view.height)
        if (view is TextView) out += view to bounds
        if (view is ViewGroup) {
            val inner = Rect(bounds).apply { intersect(clip) }
            for (i in 0 until view.childCount) {
                val c = view.getChildAt(i)
                collect(c, out, inner, x + c.left - view.scrollX, y + c.top - view.scrollY)
            }
        }
    }

    /** Where the glyphs of [v] actually are, from its gravity and measured text, with 1dp to spare. */
    private fun inked(v: TextView, r: Rect): Rect {
        // An ellipsized line is as wide as what is drawn, not the whole text.
        val width = (v.layout?.takeIf { it.lineCount > 0 }?.getLineWidth(0) ?: Layout.getDesiredWidth(v.text, v.paint)).toInt()
        val room = r.width() - v.totalPaddingLeft - v.totalPaddingRight
        val start = r.left + v.totalPaddingLeft + when (v.gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK) {
            android.view.Gravity.CENTER_HORIZONTAL -> (room - width) / 2
            android.view.Gravity.RIGHT, android.view.Gravity.END -> room - width
            else -> 0
        }
        val spare = (v.resources.displayMetrics.density / 2).toInt()
        return Rect(start - spare, r.top + v.totalPaddingTop, start + width + spare, r.bottom - v.totalPaddingBottom)
    }
}
