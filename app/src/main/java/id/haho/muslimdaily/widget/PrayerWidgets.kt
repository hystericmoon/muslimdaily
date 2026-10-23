package id.haho.muslimdaily.widget

import android.content.Context
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
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.PrayerTimeSlot
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.domain.prayer.SlotTime
import id.haho.muslimdaily.feature.fasting.labelRes
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.min

/**
 * The prayer-times widget: one widget for every size from 1×1 to 5×3 (and larger), over a sky
 * that follows the time of day. It shows the next prayer with a live countdown and, as it grows,
 * the date, the place, the day's times, what matters now and a sunnah fast tomorrow.
 */
open class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadWidgetData(context)
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

private val Corner: Dp = 24.dp
private val DividerColor = Color(0x4DFFFFFF)
private val White = ColorProvider(Color.White)

/** Bitmaps are drawn at up to 2x: sharp on any launcher, well inside the RemoteViews size limit. */
private fun scale(context: Context) = min(context.resources.displayMetrics.density, 2f)

/** The words the widget shows, in the app's language. */
private class WidgetText(private val context: Context, private val model: WidgetModel, private val clock: TimeFormatter) {
    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)
    private fun day(pattern: String): String {
        val text = DateTimeFormatter.ofPattern(pattern, AppLocale).format(model.day)
        return if (model.isTomorrow) s(R.string.widget_tomorrow, text) else text
    }
    private val month = context.resources.getStringArray(R.array.hijri_months)[(model.hijri.month - 1).coerceIn(0, 11)]

    fun name(slot: PrayerTimeSlot): String =
        if (slot == PrayerTimeSlot.DHUHR && model.friday) s(R.string.prayer_jumuah) else s(slot.labelRes())
    fun time(t: Instant): String = clock.format(t, model.zone)

    val nextName = s(model.next.prayer.labelRes())
    val nextTime = time(model.next.time)
    val dateTiny = day("EEE, d")
    val dateShort = day("EEE, d MMM")
    val dateMedium = day("EEE, d MMMM")
    val dateLong = day("EEEE, d MMMM")
    val hijriShort = s(R.string.widget_hijri_day, model.hijri.day, month)
    val hijriLong = s(R.string.hijri_date, model.hijri.day, month, model.hijri.year)
    val place = model.locationName

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

    /** TalkBack hears the widget as one sentence. */
    val description: String = buildString {
        append(dateLong).append(", ").append(hijriLong).append(", ").append(place).append(". ")
        append(s(R.string.widget_a11y_next, nextName, nextTime)).append(". ")
        append(now).append(". ")
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
    val t = WidgetText(context, model, data.time)
    Box(GlanceModifier.fillMaxSize().cornerRadius(Corner).openToday(context).semantics { contentDescription = t.description }) {
        SkyBackground(model.sky)
        val k = grid.scale
        val l = Layout(model, t, data.now, k, grid.tight)
        when (grid.rows) {
            1 -> when (grid.columns) {
                1 -> l.OneByOne()
                2 -> l.TwoByOne()
                3 -> l.WideOne(l.window(4))
                4 -> l.WideOne(model.prayers, hijri = true)
                else -> l.WideOne(model.slots, hijri = true, year = true)
            }
            2 -> when (grid.columns) {
                1 -> l.Narrow(count = 2)
                2 -> l.TwoByTwo()
                3 -> l.ThreeByTwo()
                4 -> l.Schedule(model.prayers, big = false)
                else -> l.Schedule(model.slots, big = false, year = true)
            }
            else -> when (grid.columns) {
                1 -> l.Narrow(count = 3, date = true)
                2 -> l.TwoByThree()
                3 -> l.ThreeByThree()
                4 -> l.Schedule(model.prayers, big = true)
                else -> l.Schedule(model.slots, big = true, year = true)
            }
        }
    }
}

@Composable
private fun SkyBackground(look: SkyLook) {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val bitmap = remember(size, look) {
        SkyPainter.paint(
            (size.width.value * d).toInt().coerceAtLeast(1),
            (size.height.value * d).toInt().coerceAtLeast(1),
            d,
            look,
            Corner.value * d,
        )
    }
    Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
}

/** The widget's layouts, one per size class, built from the same few parts. [k] scales the type. */
private class Layout(val model: WidgetModel, val t: WidgetText, val now: Instant, val k: Float, val tight: Boolean) {
    val next = model.nextSlot
    val inFormat: String
        @Composable get() = LocalContext.current.getString(R.string.widget_countdown_format)

    /** Four prayers that include the next one, for rows too narrow for five. */
    fun window(n: Int): List<SlotTime> {
        val prayers = model.prayers
        val i = prayers.indexOfFirst { it.slot == next }.coerceAtLeast(0)
        val start = i.coerceAtMost(prayers.size - n).coerceAtLeast(0)
        return prayers.subList(start, start + n)
    }

    /** Prayers other than the next one, nearest after it first. */
    fun others(n: Int): List<SlotTime> {
        val prayers = model.prayers
        val i = prayers.indexOfFirst { it.slot == next }
        return (prayers.drop(i + 1) + prayers.take(i).reversed()).take(n)
    }

    // ---- 1 row ----

    @Composable
    fun OneByOne() {
        Column(GlanceModifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Label(t.nextName, 12f * k, semibold = true)
            Spacer(GlanceModifier.height(2.dp))
            Pill(t.nextTime, 15f * k, padH = 6.dp)
            if (!tight) {
                Spacer(GlanceModifier.height(2.dp))
                Countdown(model.next.time, now, 11f * k, format = "%s", semibold = true)
            }
        }
    }

    @Composable
    fun TwoByOne() {
        Column(GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!tight) Label(t.dateShort, 12f * k)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(t.nextName, 14f * k, semibold = true)
                Spacer(GlanceModifier.width(8.dp))
                Pill(t.nextTime, 16f * k, padH = 8.dp)
            }
            if (!tight) Countdown(model.next.time, now, 12f * k, format = inFormat)
        }
    }

    @Composable
    fun WideOne(slots: List<SlotTime>, hijri: Boolean = false, year: Boolean = false) {
        Column(GlanceModifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!tight) {
                Row(GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (hijri) {
                        Label("${t.dateShort} · ${if (year) t.hijriLong else t.hijriShort}", 12f * k, GlanceModifier.defaultWeight(), fill = true)
                    } else {
                        NextAndCountdown(12f * k)
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    if (hijri) NextAndCountdown(12f * k) else Label(t.dateShort, 12f * k)
                }
                Spacer(GlanceModifier.height(4.dp))
            }
            TimeColumns(slots, 12f * k, 16f * k)
        }
    }

    // ---- 2 rows ----

    /** Glance drops children past ten per row or column, so the parts are nested. */
    @Composable
    fun Narrow(count: Int, date: Boolean = false) {
        Column(GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)) {
            if (date) {
                Label(t.dateTiny, 11f * k)
                Spacer(GlanceModifier.height(8.dp))
            }
            Column {
                Label(t.nextName, 12f * k, semibold = true)
                Label(t.nextTime, 22f * k, semibold = true)
                Countdown(model.next.time, now, 11f * k, format = "%s")
            }
            Spacer(GlanceModifier.height(8.dp))
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
            Column(GlanceModifier.defaultWeight().fillMaxWidth()) {
                others(count).forEach {
                    Spacer(GlanceModifier.defaultWeight())
                    Column {
                        Label(t.name(it.slot), 11f * k)
                        Label(t.time(it.time), 14f * k, semibold = true)
                    }
                }
            }
        }
    }

    @Composable
    fun TwoByTwo() {
        Column(GlanceModifier.fillMaxSize().padding(10.dp)) {
            Row(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Label(t.dateShort, 12f * k)
                Spacer(GlanceModifier.defaultWeight())
                Countdown(model.next.time, now, 12f * k, format = "%s", semibold = true)
            }
            Spacer(GlanceModifier.height(6.dp))
            Panel(GlanceModifier.defaultWeight().fillMaxWidth()) { TimeList(model.prayers, 13f * k) }
        }
    }

    @Composable
    fun ThreeByTwo() {
        Row(GlanceModifier.fillMaxSize().padding(12.dp)) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                Label(t.dateShort, 12f * k)
                Spacer(GlanceModifier.defaultWeight())
                Label(t.nextName, 14f * k, semibold = true)
                Label(t.nextTime, 32f * k)
                Countdown(model.next.time, now, 12f * k, format = inFormat)
            }
            Spacer(GlanceModifier.width(10.dp))
            Panel(GlanceModifier.width((112 * k).dp).fillMaxHeight()) { TimeList(model.prayers, 12f * k) }
        }
    }

    /** The reference layout: date and place over the day's times; at 3 rows, the next prayer large and what matters now. */
    @Composable
    fun Schedule(slots: List<SlotTime>, big: Boolean, year: Boolean = false) {
        Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
            Column(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconChip(R.drawable.ic_dark_mode)
                    Spacer(GlanceModifier.width(8.dp))
                    Label(if (year) t.dateLong else t.dateMedium, 13f * k, GlanceModifier.defaultWeight(), fill = true)
                    Spacer(GlanceModifier.width(8.dp))
                    Label(if (year || big) t.hijriLong else t.hijriShort, 13f * k)
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconChip(R.drawable.ic_location_on)
                    Spacer(GlanceModifier.width(8.dp))
                    Label(t.place, 13f * k, GlanceModifier.defaultWeight(), fill = true)
                    Spacer(GlanceModifier.width(8.dp))
                    if (big) NowLine(13f * k) else NextAndCountdown(13f * k)
                }
            }
            if (big) {
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Label(t.nextName, 16f * k, semibold = true, modifier = GlanceModifier.padding(bottom = 6.dp))
                    Spacer(GlanceModifier.width(10.dp))
                    Label(t.nextTime, (if (year) 45f else 36f) * k)
                    Spacer(GlanceModifier.defaultWeight())
                    Countdown(model.next.time, now, 13f * k, format = inFormat, modifier = GlanceModifier.padding(bottom = 6.dp))
                }
            }
            Spacer(GlanceModifier.height(if (big) 6.dp else 8.dp))
            Panel(GlanceModifier.defaultWeight().fillMaxWidth()) {
                Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    TimeColumns(slots, 13f * k, 16f * k, gap = if (big) 3.dp else 6.dp)
                }
            }
            val fast = t.fast
            if (big && fast != null) {
                Spacer(GlanceModifier.height(8.dp))
                InfoChip(R.drawable.ic_calendar_month, fast, 12f * k, GlanceModifier.fillMaxWidth())
            }
        }
    }

    // ---- 3 rows, narrower ----

    @Composable
    fun TwoByThree() {
        Column(GlanceModifier.fillMaxSize().padding(10.dp)) {
            Column(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Label(t.dateShort, 12f * k)
                NextAndCountdown(12f * k)
            }
            Spacer(GlanceModifier.height(8.dp))
            Panel(GlanceModifier.defaultWeight().fillMaxWidth()) { TimeList(model.prayers, 14f * k) }
            Spacer(GlanceModifier.height(8.dp))
            InfoChip(null, t.now, 12f * k, GlanceModifier.fillMaxWidth())
        }
    }

    @Composable
    fun ThreeByThree() {
        Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
            Column(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(GlanceModifier.fillMaxWidth()) {
                    Label(t.dateMedium, 12f * k, GlanceModifier.defaultWeight(), fill = true)
                    Spacer(GlanceModifier.width(8.dp))
                    Label(t.hijriShort, 12f * k)
                }
                Label(t.place, 12f * k, GlanceModifier.fillMaxWidth(), fill = true)
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                Label(t.nextName, 14f * k, semibold = true, modifier = GlanceModifier.padding(bottom = 5.dp))
                Spacer(GlanceModifier.width(8.dp))
                Label(t.nextTime, 32f * k)
                Spacer(GlanceModifier.defaultWeight())
                Countdown(model.next.time, now, 12f * k, format = "%s", modifier = GlanceModifier.padding(bottom = 5.dp))
            }
            Spacer(GlanceModifier.height(6.dp))
            Panel(GlanceModifier.defaultWeight().fillMaxWidth()) { TimeList(model.prayers, 13f * k) }
        }
    }

    // ---- parts ----

    @Composable
    fun NextAndCountdown(sp: Float) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(t.nextName, sp, semibold = true)
            Spacer(GlanceModifier.width(4.dp))
            Countdown(model.next.time, now, sp, format = inFormat)
        }
    }

    /** What matters now, with its icon, for a header row. */
    @Composable
    fun NowLine(sp: Float) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(t.nowIcon), contentDescription = null, modifier = GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(White))
            Spacer(GlanceModifier.width(6.dp))
            Label(t.now, sp)
        }
    }

    /** The times side by side, name over time; the next one bold on a pill. */
    @Composable
    fun TimeColumns(slots: List<SlotTime>, nameSp: Float, timeSp: Float, gap: Dp = 2.dp) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            slots.forEach { s ->
                val isNext = s.slot == next
                Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(t.name(s.slot), nameSp, semibold = isNext)
                    Spacer(GlanceModifier.height(gap))
                    if (isNext) Pill(t.time(s.time), timeSp, padH = 6.dp, padV = 2.dp) else Box(GlanceModifier.padding(vertical = 2.dp)) { Label(t.time(s.time), timeSp) }
                }
            }
        }
    }

    /** The times as a list, name left and time right; the next row on a white pill. */
    @Composable
    fun ColumnScope.TimeList(slots: List<SlotTime>, sp: Float) {
        slots.forEach { s ->
            val isNext = s.slot == next
            val color = if (isNext) OnPill else OnSky
            Box(GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                val row = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                Row(if (isNext) GlanceModifier.background(ImageProvider(R.drawable.widget_pill)).then(row) else row, verticalAlignment = Alignment.CenterVertically) {
                    Label(t.name(s.slot), sp, semibold = isNext, color = color)
                    Spacer(GlanceModifier.defaultWeight())
                    Label(t.time(s.time), sp, semibold = isNext, color = color)
                }
            }
        }
    }

    @Composable
    fun Panel(modifier: GlanceModifier, content: @Composable ColumnScope.() -> Unit) {
        Column(modifier.background(ImageProvider(R.drawable.widget_panel)).padding(4.dp), content = content)
    }
}

/** A small round chip with a white icon, before the date and the place. */
@Composable
private fun IconChip(@DrawableRes icon: Int, size: Dp = 20.dp) {
    Box(GlanceModifier.size(size).background(ImageProvider(R.drawable.widget_dot)), contentAlignment = Alignment.Center) {
        Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(White))
    }
}

/** A short line on a dark chip, with an icon when there is room: what matters now, or tomorrow's fast. */
@Composable
private fun InfoChip(@DrawableRes icon: Int?, text: String, sp: Float, modifier: GlanceModifier = GlanceModifier) {
    Row(modifier.background(ImageProvider(R.drawable.widget_chip)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(White))
            Spacer(GlanceModifier.width(6.dp))
        }
        Label(text, sp, GlanceModifier.defaultWeight(), fill = true)
    }
}

@Composable
private fun NoLocation() {
    val context = LocalContext.current
    val size = LocalSize.current
    val d = scale(context)
    val bitmap = remember(size) {
        SkyPainter.backdrop((size.width.value * d).toInt().coerceAtLeast(1), (size.height.value * d).toInt().coerceAtLeast(1), d, Corner.value * d)
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(Corner).openToday(context), contentAlignment = Alignment.Center) {
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
