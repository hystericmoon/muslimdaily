package id.haho.muslimdaily.widget

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.handover.Handover
import id.haho.muslimdaily.feature.fasting.labelRes
import id.haho.muslimdaily.domain.prayer.SlotTime
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.min

/**
 * The prayer-times widget: one widget for every size from 1×1 to 5×3 (and larger), a smoked-glass
 * card over a sky that follows the time of day. It shows the next prayer, the time until it and
 * how far the day has gone and, as it grows, the date and Hijri date, the place, the day's times,
 * what matters now and a sunnah fast tomorrow.
 */
open class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Handover.movedTo(context)?.let { moved ->
            provideContent { MovedWidgetContent(moved) }
            return
        }
        val initial = loadWidgetData(context)
        // Keeps the countdown's minute tick going while a widget is placed.
        WidgetTicker.schedule(context)
        provideContent { PrayerWidgetContent(rememberWidgetData(context, initial)) }
    }
}

/** The widget in the picker; the class keeps the name of the next-prayer widget it replaced. */
class NextPrayerWidget : PrayerTimesWidget()

class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}

/** The former day-schedule widget: hidden from the picker, kept so widgets already placed keep working. */
class DayScheduleWidget : PrayerTimesWidget()

class DayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayScheduleWidget()
}

/** The sky's corners: the launcher's own widget radius on Android 12+, else 24dp. */
private fun corner(context: Context): Dp =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.resources.getDimension(android.R.dimen.system_app_widget_background_radius) / context.resources.displayMetrics.density).dp
    } else {
        24.dp
    }

private val DividerColor = Color(0x1AFFFFFF)
private val White = ColorProvider(Color.White)
private val MutedTint = ColorProvider(Color(Muted))

/**
 * The sky is drawn at up to 1.5x and stretched by the launcher: it is a soft gradient, so on a
 * 2.75x phone it looks the same as 2x (faint stars included) at about half the bitmap memory.
 */
private fun scale(context: Context) = min(context.resources.displayMetrics.density, 1.5f)

/** The words the widget shows, in the app's language. */
internal class WidgetText(private val context: Context, private val model: WidgetModel, private val clock: TimeFormatter, now: Instant = Instant.now(), private val precise: Boolean = true) {
    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)
    private fun day(pattern: String): String = DateTimeFormatter.ofPattern(pattern, AppLocale).format(model.today)
    private val month = context.resources.getStringArray(R.array.hijri_months)[(model.todayHijri.month - 1).coerceIn(0, 11)]

    fun name(slot: PrayerTimeSlot): String =
        if (slot == PrayerTimeSlot.DHUHR && model.friday) s(R.string.prayer_jumuah) else s(slot.labelRes())
    fun time(t: Instant): String = clock.format(t, model.zone)

    val nextName = s(model.next.prayer.labelRes())
    /** Where there is room: on Fridays "Shalat Jumat", which can't be read as the day. */
    val nextNameLong = if (model.nextSlot == PrayerTimeSlot.DHUHR && model.friday) s(R.string.widget_prayer_jumuah) else nextName
    val nextTime = time(model.next.time)
    val dateTiny = day("EEE, d")
    val dateShort = day("EEE, d MMM")
    val dateMedium = day("EEE, d MMMM")
    val dateLong = day("EEEE, d MMMM")
    val hijriShort = s(R.string.widget_hijri_day, model.todayHijri.day, month)
    val hijriLong = s(R.string.hijri_date, model.todayHijri.day, month, model.todayHijri.year)
    val place = model.locationName
    val nextLabel = s(R.string.widget_next).uppercase(AppLocale)
    val arrivesLabel = s(R.string.widget_arrives_in).uppercase(AppLocale)

    /**
     * Time until the next prayer in whole minutes, rounded up (it reads "1 menit" until the
     * prayer comes in). Rounded to ten minutes and marked "±" when the widget can't redraw each
     * minute (see [WidgetTicker]).
     */
    private val minutes: Long = Duration.between(now, model.next.time).let { d -> (d.seconds + 59) / 60 }.coerceAtLeast(0).let { m ->
        if (precise) m else ((m + 5) / 10 * 10).coerceAtLeast(10)
    }
    private fun span(hm: Int, h: Int, m: Int): String {
        val text = when {
            minutes >= 60 && minutes % 60 == 0L -> s(h, minutes / 60)
            minutes >= 60 -> s(hm, minutes / 60, minutes % 60)
            else -> s(m, minutes)
        }
        return if (precise) text else s(R.string.widget_about, text)
    }
    val countdown = span(R.string.widget_in_hm, R.string.widget_in_h, R.string.widget_in_m)
    val countdownShort = span(R.string.widget_in_hm_short, R.string.widget_in_h_short, R.string.widget_in_m_short)

    val now: String = model.now.let { line ->
        val until = line.until?.let { time(it) }
        when (line.kind) {
            NowKind.FAJR_ENDS -> s(R.string.widget_now_fajr_ends, until.orEmpty())
            NowKind.PROHIBITED -> if (until != null) s(R.string.widget_now_prohibited_until, until) else s(R.string.widget_now_prohibited)
            NowKind.DUHA -> s(R.string.widget_now_duha, until.orEmpty())
            NowKind.DHUHR -> s(R.string.widget_now_prayer, s(R.string.prayer_dhuhr))
            NowKind.EVENING_ADHKAR -> s(R.string.window_evening_adhkar)
            NowKind.MAGHRIB -> s(R.string.widget_now_prayer, s(R.string.prayer_maghrib))
            NowKind.WITR -> s(R.string.window_witr)
        }
    }

    @DrawableRes
    val nowIcon: Int = when (model.now.kind) {
        NowKind.FAJR_ENDS, NowKind.MAGHRIB -> R.drawable.ic_wb_twilight
        NowKind.PROHIBITED -> R.drawable.ic_block
        NowKind.DUHA, NowKind.DHUHR -> R.drawable.ic_wb_sunny
        NowKind.EVENING_ADHKAR -> R.drawable.ic_menu_book
        NowKind.WITR -> R.drawable.ic_nightlight
    }

    val fast: String? = model.fastTomorrow?.let { s(R.string.widget_fast_tomorrow, s(it.labelRes())) }

    /** "Jakarta / Sen, 26 Okt": the place muted, the date white. */
    fun crumb(date: String): CharSequence = SpannableString("$place / $date").apply {
        setSpan(ForegroundColorSpan(Muted), 0, place.length + 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    /** TalkBack hears the widget as one sentence. */
    val description: String = buildString {
        append(dateLong).append(", ").append(hijriLong).append(", ").append(place).append(". ")
        append(s(R.string.widget_a11y_next, nextNameLong, nextTime, countdown)).append(". ")
        append(this@WidgetText.now).append(". ")
        model.prayers.joinTo(this, ", ") { "${name(it.slot)} ${time(it.time)}" }
        append('.')
        fast?.let { append(' ').append(it).append('.') }
    }
}

@Composable
internal fun PrayerWidgetContent(data: WidgetData) {
    val model = data.model ?: return NoLocation()
    val context = LocalContext.current
    val size = LocalSize.current
    val grid = WidgetGrid.of(size.width.value, size.height.value, context.resources.configuration.fontScale)
    // Built on every composition (cheap), so a language switch shows at the next update.
    val t = WidgetText(context, model, data.time, data.now, data.precise)
    val inset = Space.Inset
    Box(GlanceModifier.fillMaxSize().cornerRadius(corner(context)).openToday(context).semantics { contentDescription = t.description }) {
        SkyBackground(model.sky, inset)
        val l = Layout(model, t, grid.scale, grid.tight, size.width, size.height, inset, TextFit(context), context.resources.configuration.fontScale)
        when (grid.rows) {
            1 -> when (grid.columns) {
                1 -> l.OneByOne()
                2 -> l.TwoByOne()
                else -> l.WideOne(grid.columns)
            }
            else -> when (grid.columns) {
                1 -> l.Narrow(tall = grid.rows > 2)
                2 -> l.Square(tall = grid.rows > 2)
                3 -> l.Medium(tall = grid.rows > 2)
                else -> l.Wide(grid.columns, tall = grid.rows > 2)
            }
        }
    }
}

@Composable
private fun SkyBackground(look: SkyLook, inset: Dp) {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val corner = corner(context)
    val bitmap = remember(size, look, inset) {
        SkyPainter.paint(
            (size.width.value * d).toInt().coerceAtLeast(1),
            (size.height.value * d).toInt().coerceAtLeast(1),
            d,
            look,
            corner.value * d,
            cardInsetPx = inset.value * d,
        )
    }
    Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
}

/**
 * The widget's spacing, shared by every layout so edges and gaps line up between sizes. The card
 * sits [Inset] inside the sky, so the sky shows as a hairline rim rather than a frame; text sits
 * [Pad] inside the card ([PadSmall] on one-column widgets).
 */
private object Space {
    val Inset = 1.5.dp
    val Pad = 14.dp
    val PadSmall = 8.dp
    val PadShort = 10.dp
    val XS = 2.dp
    val S = 4.dp
    val M = 8.dp
    val L = 12.dp
    val Bar = 5.dp
    val BarThin = 4.dp
}

/**
 * The widget's layouts, one per size class, from the same few parts: a header (place and date, the
 * Hijri date as a chip), the next prayer (a mono label, its name in the accent, its time large),
 * the time until it (smaller, so it never competes with the time), and the bar of the day with
 * the prayer times as its scale. [k] scales the type for big widgets; each layout shrinks it
 * further, by [fit], when its texts would not fit.
 */
private class Layout(
    val model: WidgetModel,
    val t: WidgetText,
    val k: Float,
    val tight: Boolean,
    val width: Dp,
    val height: Dp,
    val inset: Dp,
    val fit: TextFit,
    private val fontScale: Float,
) {
    val next = model.nextSlot

    /** A line of [sp] text, in dp, for deciding what fits the height (labels use no font padding). */
    fun line(sp: Float): Dp = (sp * fontScale.coerceAtLeast(1f) * 1.2f).dp

    fun inner(pad: Dp) = width - (inset + pad) * 2
    fun innerHeight(pad: Dp) = height - (inset + pad) * 2

    /** The factor (at most 1) the type must shrink by for [need] to fit the card's height. */
    fun fitHeight(pad: Dp, need: Dp): Float = (innerHeight(pad) / need).coerceIn(0.5f, 1f)

    // ---- 1 row ----

    @Composable
    fun OneByOne() {
        val pad = Space.PadSmall
        val inner = inner(pad)
        val s = k * fit.factor(inner, FitLine(0.dp, t.nextName to 11f * k), FitLine(0.dp, *clockParts(t.nextTime, 22f * k)))
        val showBar = innerHeight(pad) >= line(11f * s) + line(22f * s) + Space.M + Space.BarThin
        Column(GlanceModifier.fillMaxSize().padding(inset + pad), verticalAlignment = Alignment.CenterVertically) {
            Label(t.nextName, 11f * s, semibold = true, color = Accent)
            Spacer(GlanceModifier.height(Space.XS))
            Label(clockText(t.nextTime), 22f * s, medium = true)
            if (showBar && !tight) {
                Spacer(GlanceModifier.height(Space.M))
                Bar(inner, Space.BarThin)
            }
        }
    }

    @Composable
    fun TwoByOne() {
        val pad = Space.PadShort
        val inner = inner(pad)
        val s = k * fit.factor(
            inner,
            FitLine(Space.M, t.nextName to 11f * k, t.countdownShort to 10f * k),
            FitLine(0.dp, *clockParts(t.nextTime, 28f * k)),
        )
        val base = line(11f * s) + Space.XS + line(28f * s)
        val showDate = innerHeight(pad) >= line(11f * s) + Space.XS + line(20f * s) + Space.S + line(10f * s) + Space.S + Space.BarThin
        val full = base + (if (showDate) Space.S + line(10f * s) else 0.dp) + Space.M + Space.BarThin
        val showBar = innerHeight(pad) >= full * 0.72f
        // With the date and bar both kept, the type shrinks to the height (large system fonts).
        val hv = (innerHeight(pad) / full).coerceIn(0.72f, 1f)
        Column(GlanceModifier.fillMaxSize().padding(horizontal = inset + Space.L, vertical = inset + pad), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(t.nextName, 11f * s * hv, semibold = true, color = Accent)
                Spacer(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(Space.M))
                Label(t.countdownShort, 10f * s * hv, color = Muted)
            }
            if (!tight) {
                Spacer(GlanceModifier.height(Space.XS))
                Label(clockText(t.nextTime), 28f * s * hv, medium = true)
                if (showDate) {
                    Spacer(GlanceModifier.height(Space.S))
                    Label("${t.dateShort} · ${t.hijriShort}", 10f * s * hv, GlanceModifier.fillMaxWidth().wrapContentHeight(), color = Muted, fill = true)
                }
                if (showBar) {
                    Spacer(GlanceModifier.height(Space.M))
                    Bar(inner(Space.L), Space.BarThin)
                }
            }
        }
    }

    /** One row, 3 to 5 columns: next prayer and time, the time until it, and (4+) the dates. */
    @Composable
    fun WideOne(columns: Int) {
        val pad = Space.Pad
        val inner = inner(pad)
        val dates = columns >= 4
        val chipW = fit.monoWidth(t.hijriShort, 10f * k) + 12f
        val dateW = fit.width(if (tight) t.dateTiny else t.dateShort, 11f * k)
        val datesWidth = when {
            !dates -> 0.dp
            tight -> (chipW + dateW).dp + Space.S + Space.L
            else -> maxOf(chipW, dateW).dp + Space.L
        }
        val vertical = if (tight) Space.S else Space.PadShort
        val timeSp = if (tight) 22f else 28f
        val tall = (innerHeight(vertical) / ((if (tight) 0.dp else line(11f * k) + Space.XS) + line(timeSp * k))).coerceAtMost(1f)
        val s = k * tall * fit.factor(
            inner - datesWidth,
            FitLine(Space.L * 2 + if (tight) 10.dp else 0.dp, *(if (tight) arrayOf(t.nextName to 11f * k) else emptyArray()), *clockParts(t.nextTime, timeSp * k), (if (tight) t.countdownShort else t.countdown) to 14f * k),
            FitLine(Space.L * 2, *clockParts(t.nextTime, timeSp * k), mono = listOf(t.arrivesLabel to 9f * k)),
        )
        Row(GlanceModifier.fillMaxSize().padding(horizontal = inset + pad, vertical = inset + vertical), verticalAlignment = Alignment.CenterVertically) {
            if (tight) {
                // Too short for two lines: the name stays, beside the time.
                Label(t.nextName, 11f * s, semibold = true, color = Accent)
                Spacer(GlanceModifier.width(Space.S + Space.XS))
                Label(clockText(t.nextTime), timeSp * s, medium = true)
            } else {
                Column {
                    Label(t.nextName, 11f * s, semibold = true, color = Accent)
                    Spacer(GlanceModifier.height(Space.XS))
                    Label(clockText(t.nextTime), timeSp * s, medium = true)
                }
            }
            Spacer(GlanceModifier.width(Space.L * 2))
            Column {
                if (!tight) {
                    Label(t.arrivesLabel, 9f * s, mono = true, color = Muted)
                    Spacer(GlanceModifier.height(Space.S))
                }
                Label(if (tight) t.countdownShort else t.countdown, 14f * s, medium = true)
            }
            Spacer(GlanceModifier.defaultWeight())
            if (dates) {
                Spacer(GlanceModifier.width(Space.L))
                if (tight) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(t.dateTiny, 11f * k)
                        Spacer(GlanceModifier.width(Space.S))
                        Chip(t.hijriShort, 10f * k)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Chip(t.hijriShort, 10f * k)
                        Spacer(GlanceModifier.height(Space.S))
                        Label(t.dateShort, 11f * k)
                    }
                }
            }
        }
    }

    // ---- 2 and 3 rows ----

    /**
     * One column: the date (tall), the next prayer, its time with the hours stacked over the
     * minutes (so a narrow, tall widget gets large figures instead of empty space), the time until
     * it and the bar.
     */
    @Composable
    fun Narrow(tall: Boolean) {
        val pad = Space.PadSmall
        val inner = inner(pad)
        val digits = clockDigits(t.nextTime)
        val parts = digits.split(':', '.')
        val marker = t.nextTime.removePrefix(digits).trim()
        val head = if (tall) line(11f * k) + line(10f * k) * 3 + Space.M else 0.dp
        val rest = line(12f * k) + Space.XS + Space.S + line(11f * k) + Space.M + Space.BarThin + (if (marker.isNotEmpty()) line(10f * k) else 0.dp)
        // Two lines of figures share what the height leaves, at most 44sp each.
        val figSp = ((innerHeight(pad) - head - rest).value / 2 / 1.2f / fontScale.coerceAtLeast(1f)).coerceIn(18f, 44f)
        val f = k * fit.factor(inner, *parts.map { FitLine(0.dp, it to figSp * k) }.toTypedArray())
        val s = k * fit.factor(inner, FitLine(0.dp, t.nextName to 12f * k), FitLine(0.dp, t.countdownShort to 11f * k))
        Column(GlanceModifier.fillMaxSize().padding(inset + pad)) {
            if (tall) {
                Label(t.dateTiny, 11f * s, GlanceModifier.fillMaxWidth().wrapContentHeight(), fill = true)
                val word = t.hijriShort.split(' ').maxBy { fit.width(it, 10f * s) }
                Label(t.hijriShort, 10f * s * fit.factor(inner, FitLine(0.dp, word to 10f * s)), GlanceModifier.fillMaxWidth().wrapContentHeight(), color = Muted, maxLines = 3, fill = true)
                Spacer(GlanceModifier.height(Space.M))
            }
            Label(t.nextName, 12f * s, semibold = true, color = Accent)
            Spacer(GlanceModifier.height(Space.XS))
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                parts.forEach { Label(it, figSp * f, medium = true) }
                if (marker.isNotEmpty()) Label(marker, 10f * s, color = Muted)
            }
            Spacer(GlanceModifier.height(Space.S))
            Label(t.countdownShort, 11f * s, color = Muted)
            Spacer(GlanceModifier.height(Space.M))
            Bar(inner, Space.BarThin)
        }
    }

    /**
     * Two columns: the date and Hijri chip, the next prayer, the time until it, the bar. Spare
     * height goes to the time (up to half again as large), then to one gap under the header.
     */
    @Composable
    fun Square(tall: Boolean) {
        val pad = Space.L
        val inner = inner(pad)
        val header = line(12f * k) + Space.S + line(10f * k) + 6.dp + Space.L
        val rest = line(9.5f * k) + Space.XS + Space.S + (if (tall) line(9.5f * k) + Space.XS + line(18f * k) else line(12f * k)) + Space.M + Space.Bar + (if (tall) Space.M + line(11f * k) else 0.dp)
        val grow = ((innerHeight(pad) - header - rest) / line(34f * k)).coerceIn(0.5f, 1.5f)
        val timeSp = 34f * grow
        val s = k * fit.factor(
            inner,
            FitLine(Space.S + Space.XS, t.nextName to 13f * k, mono = listOf(t.nextLabel to 9.5f * k)),
            FitLine(Space.S, t.countdown to 12f * k, mono = listOf(t.arrivesLabel to 9.5f * k)),
        )
        val ts = k * fit.factor(inner, FitLine(0.dp, *clockParts(t.nextTime, timeSp * k))) * minOf(1f, s / k)
        val cdGrow = if (tall) fit.factor(inner, FitLine(0.dp, t.countdown to 12f * k * 1.5f)) * 1.5f else 1f
        // Glance drops children past ten per column, so the parts are nested.
        Column(GlanceModifier.fillMaxSize().padding(inset + pad)) {
            Column {
                Label(t.dateShort, 12f * s, GlanceModifier.fillMaxWidth().wrapContentHeight(), medium = true, fill = true)
                Spacer(GlanceModifier.height(Space.S))
                Chip(t.hijriShort, 10f * k * fit.factor(inner, FitLine(16.dp, mono = listOf(t.hijriShort to 10f * k))))
            }
            Spacer(GlanceModifier.defaultWeight())
            Column {
                NextLabel(9.5f * s, 13f * s, long = false)
                Spacer(GlanceModifier.height(Space.XS))
                Label(clockText(t.nextTime), timeSp * ts, medium = true)
                Spacer(GlanceModifier.height(Space.S))
                if (tall) {
                    // Tall: the countdown gets its own line and a larger size, still under the time's.
                    Label(t.arrivesLabel, 9.5f * s, mono = true, color = Muted)
                    Spacer(GlanceModifier.height(Space.XS))
                    Label(t.countdown, 12f * s * cdGrow, medium = true)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(t.arrivesLabel, 9.5f * s, mono = true, color = Muted)
                        Spacer(GlanceModifier.width(Space.S))
                        Label(t.countdown, 12f * s, medium = true)
                    }
                }
            }
            Spacer(GlanceModifier.height(Space.M))
            Bar(inner, Space.Bar)
            if (tall) Footer(11f * k, inner)
        }
    }

    /** Three columns: header, the next prayer beside the time until it, the bar and its scale. */
    @Composable
    fun Medium(tall: Boolean) {
        val pad = Space.Pad
        val inner = inner(pad)
        Card(inner, pad, crumb = t.dateShort, chip = t.hijriShort, baseTimeSp = if (tall) 44f else 34f, baseCdSp = 13f, tall = tall)
    }

    /** Four and five columns: the reference layout. */
    @Composable
    fun Wide(columns: Int, tall: Boolean) {
        val pad = Space.Pad
        val inner = inner(pad)
        val five = columns >= 5
        Card(
            inner, pad,
            crumb = t.crumb(if (five) t.dateMedium else t.dateShort),
            chip = if (five || tall) t.hijriLong else t.hijriShort,
            baseTimeSp = if (tall) 52f else 40f,
            baseCdSp = if (tall) 17f else 15f,
            tall = tall,
            long = true,
        )
    }

    @Composable
    fun Card(inner: Dp, pad: Dp, crumb: CharSequence, chip: String, baseTimeSp: Float, baseCdSp: Float, tall: Boolean, long: Boolean = false) {
        val name = if (long) t.nextNameLong else t.nextName
        // Tall cells: spare height goes to the time and the countdown (up to 2x, capped by width), not to gaps.
        val fixed = line(12f * k) + Space.M * 3 + 1.dp + line(9.5f * k) + Space.S + Space.Bar + Space.S + line(9f * k) * 2 + Space.M + line(11f * k)
        val cardH = height - inset * 2 - Space.L - Space.Pad
        val grow = if (tall) ((cardH - fixed - (Space.L + Space.S) * 2 - 4.dp) / line(baseTimeSp * k)).coerceIn(1f, 2f) else 1f
        val timeSp = baseTimeSp * grow
        val cdSp = baseCdSp * (1f + (grow - 1f) / 2)
        // The chip keeps its room; the crumb shortens before it.
        val chipSp = 10f * k * fit.factor(inner, FitLine(64.dp, mono = listOf(chip to 10f * k)))
        // Shrinks to the height too (large system fonts on a short cell), keeping the header, the
        // next prayer and the bar; the scale and footer drop out before anything shrinks for them.
        val v = fitHeight(Space.L, maxOf(line(12f * k), line(10f * k) + 6.dp) + Space.M + 1.dp + Space.M + line(9.5f * k) + Space.S + line(baseTimeSp * k) + Space.M + Space.Bar + Space.XS)
        val s0 = k * v * fit.factor(inner, FitLine(Space.L * 2 + Space.S + Space.XS, name to 13f * k, mono = listOf(t.nextLabel to 9.5f * k, t.arrivesLabel to 9.5f * k)))
        val st0 = k * v * fit.factor(inner, FitLine(Space.L * 2, *clockParts(t.nextTime, timeSp * k), t.countdown to cdSp * k))
        // The two columns side by side: the wider of label and time on the left, of label and
        // countdown on the right. Both shrink together if that pair does not fit.
        val left = maxOf(fit.monoWidth(t.nextLabel, 9.5f * s0) + 6f + fit.width(name, 13f * s0), clockParts(t.nextTime, timeSp * st0).sumOf { (x, sp) -> fit.width(x, sp).toDouble() }.toFloat())
        val right = maxOf(fit.monoWidth(t.arrivesLabel, 9.5f * s0), fit.width(t.countdown, cdSp * st0))
        val both = ((inner.value * 0.96f - Space.L.value * 2) / (left + right)).coerceAtMost(1f)
        val s = s0 * both
        val st = st0 * both
        // The card's height inside its padding (more below the scale than above the header).
        val avail = height - inset * 2 - Space.L - Space.Pad
        val withNames = Scale(inner, names = true).let { if (it.small) Scale(inner, names = false) else it }
        val headerH = maxOf(line(12f * k * v), line(chipSp * v) + 6.dp)
        val bodyH = line(9.5f * s) + Space.S + line(timeSp * st)
        // The gaps as drawn: fixed on tall cards, at least M where they flex.
        val gap = if (tall) Space.L + Space.S else Space.M
        val base = headerH + Space.M + 1.dp + gap + bodyH + gap + Space.Bar + Space.S
        // Decided by the room there is, not the size class (Samsung's 2-row cells are taller than
        // Pixel's): names over the scale's times where they fit, then the footer under them.
        val scale = if (avail >= base + withNames.height) withNames else Scale(inner, names = false)
        val footer = avail >= base + withNames.height + Space.M + maxOf(line(11f * k), 12.dp) + 2.dp
        // Tall cards keep one rhythm from header to footer and centre the group in the card.
        Column(
            GlanceModifier.fillMaxSize().padding(start = inset + pad, end = inset + pad, top = inset + Space.L, bottom = inset + Space.Pad),
            verticalAlignment = if (tall) Alignment.CenterVertically else Alignment.Top,
        ) {
            Column(GlanceModifier.fillMaxWidth()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Label(crumb, 12f * k * v, GlanceModifier.defaultWeight().wrapContentHeight(), fill = true)
                    Spacer(GlanceModifier.width(Space.M))
                    Chip(chip, chipSp * v)
                }
                Spacer(GlanceModifier.height(Space.M))
                Box(GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
            }
            Spacer(if (tall) GlanceModifier.height(Space.L + Space.S) else GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    NextLabel(9.5f * s, 13f * s, long)
                    Spacer(GlanceModifier.height(Space.S))
                    Label(clockText(t.nextTime), timeSp * st, medium = true)
                }
                Spacer(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(Space.L))
                Column(horizontalAlignment = Alignment.End) {
                    Label(t.arrivesLabel, 9.5f * s, mono = true, color = Muted)
                    Spacer(GlanceModifier.height(Space.S))
                    Label(t.countdown, cdSp * st, medium = true, modifier = GlanceModifier.padding(bottom = 2.dp))
                }
            }
            // Tall: the next prayer and the bar stay together in the middle, the footer at the bottom.
            Spacer(if (tall) GlanceModifier.height(Space.L + Space.S) else GlanceModifier.defaultWeight())
            Column(GlanceModifier.fillMaxWidth()) {
                Bar(inner, Space.Bar, scale.stops)
                // Flexible gaps (short cards) can close to nothing; fixed ones (tall cards) cannot.
                val minGap = if (tall) gap else 0.dp
                if (!scale.small && avail >= headerH + Space.M + 1.dp + minGap * 2 + bodyH + Space.Bar + Space.S + scale.height) {
                    Spacer(GlanceModifier.height(Space.S))
                    scale.Draw()
                }
            }
            if (footer) Footer(11f * k, inner)
        }
    }

    // ---- parts ----

    /** "BERIKUTNYA  Ashar": the label muted and mono, the prayer in the accent. */
    @Composable
    fun NextLabel(labelSp: Float, nameSp: Float, long: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(t.nextLabel, labelSp, mono = true, color = Muted)
            Spacer(GlanceModifier.width(Space.S + Space.XS))
            Label(if (long) t.nextNameLong else t.nextName, nameSp, semibold = true, color = Accent)
        }
    }

    /** The bar of the day (or night): accent up to now, dashed after. [stops] in dp from the start. */
    @Composable
    fun Bar(barWidth: Dp, barHeight: Dp, stops: List<Dp>? = null) {
        val context = LocalContext.current
        val d = context.resources.displayMetrics.density
        val n = model.track.stops.size
        val at = stops ?: List(n) { i -> barWidth * (i.toFloat() / (n - 1)) }
        val bitmap = SkyPainter.bar((barWidth.value * d).toInt(), (barHeight.value * d).toInt(), d, FloatArray(n) { at[it].value * d }, model.track.progress)
        Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.width(barWidth).height(barHeight))
    }

    /**
     * The bar's scale: each stop's time (and, with [names], its prayer above it), spread from edge
     * to edge with the next one in the accent. [stops] is where each time's middle lands, so the
     * bar's stops sit right under them.
     */
    inner class Scale(private val inner: Dp, private val names: Boolean) {
        private val items = model.track.stops
        private val nameSp = 8.5f * k
        private val baseSp = 9f * k
        private fun timeOf(i: Int) = clockDigits(t.time(items[i].time))
        private val raw = items.indices.map { i -> fit.monoWidth(timeOf(i), baseSp) to if (names) fit.monoWidth(t.name(items[i].slot), nameSp) else 0f }
        /** Shrinks the scale's type until every item has 6dp between it and the next. */
        private val f = run {
            val total = raw.sumOf { (a, b) -> maxOf(a, b).toDouble() }.toFloat()
            val room = inner.value - 6f * (items.size - 1)
            if (total <= room) 1f else (room / total).coerceAtLeast(0.5f)
        }
        private val widths = raw.map { (a, b) -> maxOf(a, b) * f to a * f }
        /** Too small to read (under three-quarters size): the scale is left out rather than shown tiny. */
        val small = f < 0.75f
        val height: Dp = line(baseSp * f) + if (names) line(nameSp * f) + Space.XS else 0.dp
        val stops: List<Dp> = run {
            val gap = (inner.value - widths.sumOf { it.first.toDouble() }.toFloat()) / (items.size - 1)
            var x = 0f
            widths.mapIndexed { i, (w, time) ->
                val center = when (i) {
                    0 -> x + time / 2
                    items.size - 1 -> x + w - time / 2
                    else -> x + w / 2
                }
                x += w + gap
                center.dp
            }
        }

        @Composable
        fun Draw() {
            Row(GlanceModifier.fillMaxWidth()) {
                items.forEachIndexed { i, item ->
                    val isNext = item.slot == next && item.time == model.next.time
                    val color = if (isNext) Accent else Muted
                    val align = when (i) {
                        0 -> Alignment.Start
                        items.size - 1 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                    Column(horizontalAlignment = align) {
                        if (names) {
                            Label(t.name(item.slot), nameSp * f, mono = true, color = color)
                            Spacer(GlanceModifier.height(Space.XS))
                        }
                        Label(timeOf(i), baseSp * f, mono = true, color = color)
                    }
                    if (i < items.size - 1) Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }

    /** What matters now and a sunnah fast tomorrow, for the tall sizes. */
    @Composable
    fun Footer(sp: Float, inner: Dp) {
        // Only what fits whole: both, else tomorrow's fast, else what matters now, else nothing.
        val fast = t.fast
        val nowW = 18f + fit.width(t.now, sp)
        val fastW = fast?.let { fit.width(it, sp) } ?: 0f
        val room = inner.value * 0.96f
        val showNow = nowW <= room && (fast == null || nowW + fastW + 8f <= room || fastW > room)
        val showFast = fast != null && fastW <= room - (if (showNow) nowW + 8f else 0f)
        if (!showNow && !showFast) return
        Spacer(GlanceModifier.height(Space.M))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showNow) {
                Image(ImageProvider(t.nowIcon), contentDescription = null, modifier = GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(MutedTint))
                Spacer(GlanceModifier.width(Space.S + Space.XS))
                Label(t.now, sp)
            }
            if (showFast) {
                Spacer(GlanceModifier.defaultWeight())
                Label(fast!!, sp, color = Accent)
            }
        }
    }
}

@Composable
private fun NoLocation() {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val bitmap = remember(size) {
        SkyPainter.backdrop((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, corner(context).value * d)
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(corner(context)).openToday(context), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
        Label(
            context.getString(R.string.widget_no_location),
            13f,
            GlanceModifier.padding(horizontal = 16.dp),
            maxLines = 4,
            center = true,
        )
    }
}

/** After the move to the new Adwam app: this widget says so and opens the new app. */
@Composable
internal fun MovedWidgetContent(newPackage: String) {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val bitmap = remember(size) {
        SkyPainter.backdrop((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, corner(context).value * d)
    }
    val open = context.packageManager.getLaunchIntentForPackage(newPackage)
    Box(
        GlanceModifier.fillMaxSize().cornerRadius(corner(context)).let { m -> open?.let { m.clickable(actionStartActivity(it)) } ?: m },
        contentAlignment = Alignment.Center,
    ) {
        Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
        Label(
            context.getString(R.string.handover_widget_moved),
            13f,
            GlanceModifier.padding(horizontal = 16.dp),
            maxLines = 4,
            center = true,
        )
    }
}
