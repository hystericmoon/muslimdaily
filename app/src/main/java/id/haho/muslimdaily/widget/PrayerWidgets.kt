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
import id.haho.muslimdaily.core.ui.clockPattern
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.handover.Handover
import id.haho.muslimdaily.feature.fasting.labelRes
import id.haho.muslimdaily.domain.prayer.SlotTime
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The prayer-times widget: one widget for every size from 1×1 to 5×3 (and larger), a card of
 * translucent black glass. It shows the current time, the time left until the next prayer and
 * how far the day has gone and, as it grows, the sun or the moon, the date and Hijri date, the
 * place, the day's times, what matters now and a sunnah fast tomorrow.
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

/** The widget's corners: the launcher's own widget radius on Android 12+, else 24dp. */
private fun corner(context: Context): Dp =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.resources.getDimension(android.R.dimen.system_app_widget_background_radius) / context.resources.displayMetrics.density).dp
    } else {
        24.dp
    }

private val DividerColor = Color(0x1AFFFFFF)
private val MutedTint = ColorProvider(Color(Muted))

/**
 * The glass is drawn at up to 2x and stretched by the launcher: a tint and a hairline edge look
 * the same on a 2.75x phone at about half the bitmap memory.
 */
private fun scale(context: Context) = min(context.resources.displayMetrics.density, 2f)

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

    /** The clock's pattern without the 12-hour marker, which is set smaller beside it. */
    val clockDigitsPattern: String = clockPattern(clock.is24Hour).removeSuffix(" a")
    val clockHoursPattern: String = if (clock.is24Hour) "HH" else "h"
    val clockMarker: String? = if (clock.is24Hour) null else "a"
    /** The clock at its widest, for fitting: the figures are tabular, so noon stands for any time. */
    val clockSample: String = DateTimeFormatter.ofPattern(clockDigitsPattern, AppLocale).format(LocalTime.NOON)
    val markerSample: String = DateTimeFormatter.ofPattern("a", AppLocale).format(LocalTime.NOON)

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

    /** "Maghrib 17.51": the prayer the countdown runs to, and its time. */
    fun target(long: Boolean): String = "${if (long) nextNameLong else nextName} $nextTime"
    /** "2 jam 47 menit menuju Maghrib 17.51", the prayer and its time in the accent. */
    fun until(long: Boolean): CharSequence = accent(s(R.string.widget_until, countdown, target(long)), target(long))
    /** "menuju Maghrib 17.51", the second line when the countdown is split in two. */
    fun untilTarget(long: Boolean): CharSequence = accent(s(R.string.widget_until_target, target(long)), target(long))

    private fun accent(text: String, part: String): CharSequence = SpannableString(text).apply {
        val i = text.lastIndexOf(part)
        if (i >= 0) setSpan(ForegroundColorSpan(Accent), i, i + part.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

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
    Box(GlanceModifier.fillMaxSize().cornerRadius(corner(context)).openToday(context).semantics { contentDescription = t.description }) {
        GlassBackground()
        val l = Layout(model, t, grid.scale, grid.tight, size.width, size.height, TextFit(context), context.resources.configuration.fontScale, data.now, data.liveClock)
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
private fun GlassBackground() {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val corner = corner(context)
    val bitmap = remember(size) {
        GlassPainter.glass((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, corner.value * d)
    }
    Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
}

/**
 * The widget's spacing, shared by every layout so edges and gaps line up between sizes. Text sits
 * [Pad] inside the glass ([PadSmall] on one-column widgets).
 */
private object Space {
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

/** How much smaller a 12-hour "AM"/"PM" is set than the clock's digits. */
private const val MarkerScale = 0.45f

/** How the time left is set on a card: with "Shalat Jumat" or "Jumat", on one line or two, at [sp]. */
private data class Countdown(val long: Boolean, val split: Boolean, val sp: Float)

/**
 * The widget's layouts, one per size class, from the same few parts: a header (place, date and
 * Hijri date), the current time large, the time left and the prayer it runs to (with that
 * prayer in the accent), the sun or moon, and the bar of the day with the prayer times as its
 * scale. [k] scales the type for big widgets; each layout shrinks it further, by [fit], when its
 * texts would not fit.
 */
private class Layout(
    val model: WidgetModel,
    val t: WidgetText,
    val k: Float,
    val tight: Boolean,
    val width: Dp,
    val height: Dp,
    val fit: TextFit,
    private val fontScale: Float,
    private val now: Instant,
    private val live: Boolean,
) {
    val next = model.nextSlot

    /** A line of [sp] text, in dp, for deciding what fits the height (labels use no font padding). */
    fun line(sp: Float): Dp = (sp * fontScale.coerceAtLeast(1f) * 1.2f).dp

    fun inner(pad: Dp) = width - pad * 2
    fun innerHeight(pad: Dp) = height - pad * 2

    /** The factor (at most 1) the type must shrink by for [need] to fit the card's height. */
    fun fitHeight(pad: Dp, need: Dp): Float = (innerHeight(pad) / need).coerceIn(0.5f, 1f)

    /**
     * The clock at its widest, in dp: its pattern with every figure the widest one, since not
     * every font's figures are tabular, so no time the clock ticks to can outgrow it.
     */
    fun clockWidth(sp: Float): Float =
        fit.boldWidth(widestClock, sp) + if (t.clockMarker != null) Space.S.value + fit.boldWidth(t.markerSample, sp * MarkerScale) else 0f

    private val widestClock: String by lazy {
        ('0'..'9').map { d -> t.clockSample.replace(Regex("\\d"), d.toString()) }.maxBy { fit.boldWidth(it, 20f) }
    }

    /** The factor (at most 1) for the clock at [sp] to fit [room]. */
    fun clockFit(room: Dp, sp: Float): Float = shrink(room) { clockWidth(sp * it) }

    /**
     * The factor (at most 1) for a line [width] measures at each factor to fit [room].
     * Re-measured at each smaller size, as [TextFit.factor] does: with Android 14's nonlinear
     * font scaling, small type grows more with the system font than large type does.
     */
    fun shrink(room: Dp, width: (Float) -> Float): Float {
        val fits = room.value * 0.96f
        var f = 1f
        repeat(5) {
            val w = width(f)
            if (w <= fits || w <= 0f) return f
            f *= fits / w
        }
        return f
    }

    /** The current time, its 12-hour marker smaller beside it. */
    @Composable
    fun Now(sp: Float) {
        Row(verticalAlignment = Alignment.Bottom) {
            Clock(t.clockDigitsPattern, sp, model.zone, now, live)
            t.clockMarker?.let {
                Spacer(GlanceModifier.width(Space.S))
                Clock(it, sp * MarkerScale, model.zone, now, live, modifier = GlanceModifier.padding(bottom = (sp * 0.08f).dp))
            }
        }
    }

    // ---- 1 row ----

    @Composable
    fun OneByOne() {
        val pad = Space.PadSmall
        val inner = inner(pad)
        val s = k * minOf(fit.factor(inner, FitLine(0.dp, t.nextName to 11f * k), FitLine(0.dp, t.countdownShort to 10f * k)), clockFit(inner, 22f * k))
        val head = line(22f * s) + Space.XS + line(11f * s)
        val showCountdown = innerHeight(pad) >= head + line(10f * s)
        val showBar = innerHeight(pad) >= head + line(10f * s) + Space.M + Space.BarThin
        Column(GlanceModifier.fillMaxSize().padding(pad), verticalAlignment = Alignment.CenterVertically) {
            Now(22f * s)
            Spacer(GlanceModifier.height(Space.XS))
            Label(t.nextName, 11f * s, semibold = true, color = Accent)
            if (showCountdown) Label(t.countdownShort, 10f * s, color = Muted)
            if (showBar && !tight) {
                Spacer(GlanceModifier.height(Space.M))
                Bar(inner, Space.BarThin)
            }
        }
    }

    @Composable
    fun TwoByOne() {
        val pad = Space.PadShort
        val inner = inner(Space.L)
        val target = t.target(long = false)
        val s = k * minOf(fit.factor(inner, FitLine(Space.M, target to 11f * k, t.countdownShort to 10f * k)), clockFit(inner, 28f * k))
        val base = line(11f * s) + Space.XS + line(28f * s)
        val showDate = innerHeight(pad) >= line(11f * s) + Space.XS + line(20f * s) + Space.S + line(10f * s) + Space.S + Space.BarThin
        val full = base + (if (showDate) Space.S + line(10f * s) else 0.dp) + Space.M + Space.BarThin
        val showBar = innerHeight(pad) >= full * 0.72f
        // With the date and bar both kept, the type shrinks to the height (large system fonts).
        val hv = (innerHeight(pad) / full).coerceIn(0.72f, 1f)
        Column(GlanceModifier.fillMaxSize().padding(horizontal = Space.L, vertical = pad), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(target, 11f * s * hv, semibold = true, color = Accent)
                Spacer(GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(Space.M))
                Label(t.countdownShort, 10f * s * hv, color = Muted)
            }
            if (!tight) {
                Spacer(GlanceModifier.height(Space.XS))
                Now(28f * s * hv)
                if (showDate) {
                    Spacer(GlanceModifier.height(Space.S))
                    Label("${t.dateShort} · ${t.hijriShort}", 10f * s * hv, GlanceModifier.fillMaxWidth().wrapContentHeight(), color = Muted, fill = true)
                }
                if (showBar) {
                    Spacer(GlanceModifier.height(Space.M))
                    Bar(inner, Space.BarThin)
                }
            }
        }
    }

    /** One row, 3 to 5 columns: the current time, the time left and its prayer, and (4+) the dates. */
    @Composable
    fun WideOne(columns: Int) {
        val pad = Space.Pad
        val inner = inner(pad)
        val dates = columns >= 4
        val dateW = if (tight) fit.width(t.dateTiny, 11f * k) + Space.S.value + fit.width(t.hijriShort, 11f * k) else maxOf(fit.width(t.dateShort, 11f * k), fit.width(t.hijriShort, 11f * k))
        val datesWidth = if (dates) dateW.dp + Space.L else 0.dp
        val vertical = if (tight) Space.S else Space.PadShort
        val timeSp = if (tight) 22f else 28f
        val target = t.target(long = false)
        val tall = (innerHeight(vertical) / (if (tight) line(timeSp * k) else maxOf(line(timeSp * k), line(14f * k) + Space.S + line(12f * k)))).coerceAtMost(1f)
        val untilTarget = t.untilTarget(false).toString()
        val s = k * tall * shrink(inner - datesWidth) { f ->
            val q = k * tall * f
            clockWidth(timeSp * q) + if (tight) {
                Space.S.value + Space.XS.value + fit.width(target, 11f * q) + Space.S.value + fit.width(t.countdownShort, 11f * q)
            } else {
                Space.L.value * 2 + maxOf(fit.width(t.countdown, 14f * q), fit.width(untilTarget, 12f * q))
            }
        }
        Row(GlanceModifier.fillMaxSize().padding(horizontal = pad, vertical = vertical), verticalAlignment = Alignment.CenterVertically) {
            Now(timeSp * s)
            if (tight) {
                // Too short for two lines: the prayer and the time left beside the clock.
                Spacer(GlanceModifier.width(Space.S + Space.XS))
                Label(target, 11f * s, semibold = true, color = Accent)
                Spacer(GlanceModifier.width(Space.S))
                Label(t.countdownShort, 11f * s, color = Muted)
            } else {
                Spacer(GlanceModifier.width(Space.L * 2))
                Column {
                    Label(t.countdown, 14f * s, medium = true)
                    Spacer(GlanceModifier.height(Space.S))
                    Label(t.untilTarget(false), 12f * s)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (dates) {
                Spacer(GlanceModifier.width(Space.L))
                if (tight) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(t.dateTiny, 11f * k)
                        Spacer(GlanceModifier.width(Space.S))
                        Label(t.hijriShort, 11f * k)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Label(t.dateShort, 11f * k)
                        Spacer(GlanceModifier.height(Space.S))
                        Label(t.hijriShort, 11f * k)
                    }
                }
            }
        }
    }

    // ---- 2 and 3 rows ----

    /**
     * One column: the date (tall), the current time with the hours stacked over the minutes (so a
     * narrow, tall widget gets large figures instead of empty space), the next prayer, the time
     * left and the bar.
     */
    @Composable
    fun Narrow(tall: Boolean) {
        val pad = Space.PadSmall
        val inner = inner(pad)
        val marker = t.clockMarker
        val head = if (tall) line(11f * k) + line(10f * k) * 3 + Space.M else 0.dp
        val rest = Space.XS + line(12f * k) + Space.S + line(11f * k) + Space.M + Space.BarThin + (if (marker != null) line(10f * k) else 0.dp)
        // Two lines of figures share what the height leaves, at most 44sp each.
        val figSp = ((innerHeight(pad) - head - rest).value / 2 / 1.2f / fontScale.coerceAtLeast(1f)).coerceIn(18f, 44f)
        val f = k * (inner.value * 0.96f / fit.boldWidth("00", figSp * k)).coerceAtMost(1f)
        val s = k * fit.factor(inner, FitLine(0.dp, t.nextName to 12f * k), FitLine(0.dp, t.countdownShort to 11f * k))
        Column(GlanceModifier.fillMaxSize().padding(pad)) {
            if (tall) {
                Label(t.dateTiny, 11f * s, GlanceModifier.fillMaxWidth().wrapContentHeight(), fill = true)
                val word = t.hijriShort.split(' ').maxBy { fit.width(it, 10f * s) }
                Label(t.hijriShort, 10f * s * fit.factor(inner, FitLine(0.dp, word to 10f * s)), GlanceModifier.fillMaxWidth().wrapContentHeight(), color = Muted, maxLines = 3, fill = true)
                Spacer(GlanceModifier.height(Space.M))
            }
            Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Clock(t.clockHoursPattern, figSp * f, model.zone, now, live)
                Clock("mm", figSp * f, model.zone, now, live)
                if (marker != null) Clock(marker, 10f * s, model.zone, now, live, color = Muted)
            }
            Spacer(GlanceModifier.height(Space.XS))
            Label(t.nextName, 12f * s, semibold = true, color = Accent)
            Spacer(GlanceModifier.height(Space.S))
            Label(t.countdownShort, 11f * s, color = Muted)
            Spacer(GlanceModifier.height(Space.M))
            Bar(inner, Space.BarThin)
        }
    }

    /**
     * Two columns: the date and Hijri date, the current time, the time left and its prayer, the
     * bar. Spare height goes to the time (up to half again as large).
     */
    @Composable
    fun Square(tall: Boolean) {
        val pad = Space.L
        val inner = inner(pad)
        val header = line(12f * k) * 2 + Space.S + Space.L
        val rest = line(12f * k) * 2 + Space.S + Space.XS + Space.M + Space.Bar + (if (tall) Space.M + line(11f * k) else 0.dp)
        val grow = ((innerHeight(pad) - header - rest) / line(34f * k)).coerceIn(0.5f, 1.5f)
        val timeSp = 34f * grow
        val target = t.untilTarget(false)
        val s = k * fit.factor(inner, FitLine(0.dp, t.countdown to 12f * k), FitLine(0.dp, target.toString() to 12f * k), FitLine(0.dp, t.hijriShort to 12f * k))
        val ts = k * clockFit(inner, timeSp * k) * minOf(1f, s / k)
        // Glance drops children past ten per column, so the parts are nested.
        Column(GlanceModifier.fillMaxSize().padding(pad)) {
            Column {
                Label(t.dateShort, 12f * s, GlanceModifier.fillMaxWidth().wrapContentHeight(), medium = true, fill = true)
                Spacer(GlanceModifier.height(Space.S))
                Label(t.hijriShort, 12f * s, medium = true)
            }
            Spacer(GlanceModifier.defaultWeight())
            Column {
                Now(timeSp * ts)
                Spacer(GlanceModifier.height(Space.S))
                Label(t.countdown, 12f * s, medium = true)
                Spacer(GlanceModifier.height(Space.XS))
                Label(target, 12f * s)
            }
            Spacer(GlanceModifier.height(Space.M))
            Bar(inner, Space.Bar)
            if (tall) Footer(11f * k, inner)
        }
    }

    /** Three columns: header, the current time and the sun or moon, the bar and its scale. */
    @Composable
    fun Medium(tall: Boolean) {
        val pad = Space.Pad
        Card(inner(pad), pad, crumb = t.dateShort, hijri = t.hijriShort, baseTimeSp = if (tall) 44f else 34f, baseCdSp = 13f, tall = tall)
    }

    /** Four and five columns: the reference layout. */
    @Composable
    fun Wide(columns: Int, tall: Boolean) {
        val pad = Space.Pad
        val five = columns >= 5
        Card(
            inner(pad), pad,
            crumb = t.crumb(if (five) t.dateMedium else t.dateShort),
            hijri = if (five || tall) t.hijriLong else t.hijriShort,
            baseTimeSp = if (tall) 52f else 38f,
            baseCdSp = if (tall) 17f else 15f,
            tall = tall,
            long = true,
        )
    }

    @Composable
    fun Card(inner: Dp, pad: Dp, crumb: CharSequence, hijri: String, baseTimeSp: Float, baseCdSp: Float, tall: Boolean, long: Boolean = false) {
        // The card's height inside its padding (more below the scale than above the header).
        val avail = height - Space.L - Space.Pad
        // Tall cells: spare height goes to the time and the time left (up to 2x, capped by width), not to gaps.
        val fixed = line(12f * k) + Space.M + 1.dp + (Space.L + Space.S) * 2 + Space.XS * 2 + line(baseCdSp * k) * 2 + Space.Bar + Space.S + line(9f * k) + line(8.5f * k) + Space.XS + Space.M + line(11f * k) + 4.dp
        val grow = if (tall) ((avail - fixed) / line(baseTimeSp * k)).coerceIn(1f, 2f) else 1f
        val timeSp = baseTimeSp * grow
        val cdSp = baseCdSp * (1f + (grow - 1f) / 2)
        // The Hijri date keeps its room; the crumb shortens before it.
        val hijriSp = 12f * k * fit.factor(inner, FitLine(64.dp, hijri to 12f * k))
        // Shrinks to the height too (large system fonts on a short cell), keeping the header, the
        // time and the bar; the scale and footer drop out before anything shrinks for them.
        val v = fitHeight(Space.L, line(12f * k) + Space.M + 1.dp + Space.M + line(baseTimeSp * k) + Space.XS + line(baseCdSp * k) + Space.M + Space.Bar + Space.XS)
        // The sun or moon takes about the clock's height on the right; the text keeps clear of it.
        val disc = minOf(line(timeSp * k * v), inner * 0.28f)
        val room = inner - disc - Space.L
        val ts = timeSp * k * v * clockFit(room, timeSp * k * v)
        // The time left and its prayer: one line where it fits nearly whole, else two; "Shalat
        // Jumat" where there is room, else "Jumat".
        val cd = cdSp * k * v
        fun oneLine(long: Boolean) = fit.factor(room, FitLine(0.dp, t.until(long).toString() to cd))
        fun twoLines(long: Boolean) = fit.factor(room, FitLine(0.dp, t.countdown to cd), FitLine(0.dp, t.untilTarget(long).toString() to cd))
        // One line with either name before two lines: the height a second line takes is better
        // spent on the scale's names and the footer.
        val options = listOf(true to false, false to false, true to true, false to true).filter { (l, _) -> long || !l }.mapNotNull { (l, split) ->
            val f = if (split) twoLines(l) else oneLine(l)
            if (f >= (if (split) 0.85f else 0.9f)) Countdown(l, split, cd * f) else null
        }.ifEmpty { listOf(Countdown(false, true, cd * twoLines(false))) }
        fun body(c: Countdown, clockSp: Float) = line(clockSp) + Space.XS + if (c.split) line(c.sp) * 2 + Space.XS else line(c.sp)
        val headerH = maxOf(line(12f * k * v), line(hijriSp * v))
        // The gaps as drawn: fixed on tall cards, at least M where they flex.
        val gap = if (tall) Space.L + Space.S else Space.M
        val withNames = Scale(inner, names = true).let { if (it.small) Scale(inner, names = false) else it }
        // Short cards give the scale's prayer names priority: the countdown takes one line if it
        // can, and the clock gives up to 15% of its size, before the names drop out.
        val others = headerH + Space.M + 1.dp + gap * 2 + Space.Bar + Space.S + withNames.height
        val (countdown, clockSp) = (if (tall) null else options.firstNotNullOfOrNull { c ->
            val most = (avail - others - body(c, 0f)).value / 1.2f / fontScale.coerceAtLeast(1f)
            if (most >= ts * 0.85f) c to minOf(ts, most) else null
        }) ?: (options.first() to ts)
        val split = countdown.split
        val useLong = countdown.long
        val cs = countdown.sp
        val bodyH = body(countdown, clockSp)
        val base = headerH + Space.M + 1.dp + gap + bodyH + gap + Space.Bar + Space.S
        // Decided by the room there is, not the size class: names over the scale's times where
        // they fit, then the footer under them.
        val scale = if (avail >= base + withNames.height) withNames else Scale(inner, names = false)
        val footerH = Space.M + maxOf(line(11f * k), 12.dp) + 2.dp
        val footer = avail >= base + withNames.height + footerH
        // Flexible gaps (short cards) can close to nothing; fixed ones (tall cards) cannot.
        val minGap = if (tall) gap else 0.dp
        val showScale = !scale.small && avail >= headerH + Space.M + 1.dp + minGap * 2 + bodyH + Space.Bar + Space.S + scale.height
        // Where the time sits, to centre the sun or moon on it: tall cards centre the whole group
        // with fixed gaps; short ones split the spare height between the two flexible gaps.
        val content = headerH + Space.M + 1.dp + bodyH + Space.Bar + (if (showScale) Space.S + scale.height else 0.dp) + (if (footer) footerH else 0.dp)
        val around = if (tall) gap else ((avail - content) / 2).coerceAtLeast(0.dp)
        val bodyTop = Space.L + (if (tall) ((avail - content - gap * 2) / 2).coerceAtLeast(0.dp) else 0.dp) + headerH + Space.M + 1.dp + around
        val square = minOf(disc / 0.6f, bodyH + around * 2)
        val glow = (square - square * 0.6f) / 2
        Box(GlanceModifier.fillMaxSize()) {
            Box(
                GlanceModifier.fillMaxSize().padding(top = (bodyTop + bodyH / 2 - square / 2).coerceAtLeast(0.dp), end = (pad - glow).coerceAtLeast(0.dp)),
                contentAlignment = Alignment.TopEnd,
            ) {
                Celestial(square)
            }
            Column(
                GlanceModifier.fillMaxSize().padding(start = pad, end = pad, top = Space.L, bottom = Space.Pad),
                verticalAlignment = if (tall) Alignment.CenterVertically else Alignment.Top,
            ) {
                Column(GlanceModifier.fillMaxWidth()) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Label(crumb, 12f * k * v, GlanceModifier.defaultWeight().wrapContentHeight(), fill = true)
                        Spacer(GlanceModifier.width(Space.M))
                        Label(hijri, hijriSp * v)
                    }
                    Spacer(GlanceModifier.height(Space.M))
                    Box(GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
                }
                Spacer(if (tall) GlanceModifier.height(gap) else GlanceModifier.defaultWeight())
                Column(GlanceModifier.fillMaxWidth()) {
                    Now(clockSp)
                    Spacer(GlanceModifier.height(Space.XS))
                    if (split) {
                        Label(t.countdown, cs, medium = true)
                        Spacer(GlanceModifier.height(Space.XS))
                        Label(t.untilTarget(useLong), cs, medium = true)
                    } else {
                        Label(t.until(useLong), cs, medium = true)
                    }
                }
                Spacer(if (tall) GlanceModifier.height(gap) else GlanceModifier.defaultWeight())
                Column(GlanceModifier.fillMaxWidth()) {
                    Bar(inner, Space.Bar, scale.stops)
                    if (showScale) {
                        Spacer(GlanceModifier.height(Space.S))
                        scale.Draw()
                    }
                }
                if (footer) Footer(11f * k, inner)
            }
        }
    }

    // ---- parts ----

    /** The sun by day, the moon at its phase by night, with its glow, in a square of [size]. */
    @Composable
    fun Celestial(size: Dp) {
        val resources = LocalContext.current.resources
        val px = (size.value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val phase = (model.moonPhase * 100).roundToInt() / 100f
        val bitmap = remember(px, model.daytime, phase) { if (model.daytime) GlassPainter.sun(resources, px) else GlassPainter.moon(resources, px, phase) }
        Image(ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(size))
    }

    /** The bar of the day (or night): accent up to now, dashed after. [stops] in dp from the start. */
    @Composable
    fun Bar(barWidth: Dp, barHeight: Dp, stops: List<Dp>? = null) {
        val context = LocalContext.current
        val d = context.resources.displayMetrics.density
        val n = model.track.stops.size
        val at = stops ?: List(n) { i -> barWidth * (i.toFloat() / (n - 1)) }
        val bitmap = GlassPainter.bar((barWidth.value * d).toInt(), (barHeight.value * d).toInt(), d, FloatArray(n) { at[it].value * d }, model.track.progress)
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
        GlassPainter.glass((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, corner(context).value * d)
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
        GlassPainter.glass((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, corner(context).value * d)
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
