package id.haho.muslimdaily.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.labelRes
import java.time.format.DateTimeFormatter
import kotlin.math.min

/** Next prayer over a living sky: name, time and a live countdown. 2×2, resizable to 2×1. */
class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadWidgetData(context)
        provideContent { NextPrayerContent(rememberWidgetData(context, initial)) }
    }
}

class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}

/** Today over a living sky: the sun's path with each prayer on it, and the six times. 4×2. */
class DayScheduleWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadWidgetData(context)
        provideContent { DayScheduleContent(rememberWidgetData(context, initial)) }
    }
}

class DayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayScheduleWidget()
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color.White.copy(alpha = 0.78f))
private val Corner: Dp = 24.dp
private val StripHeight: Dp = 54.dp

/** Bitmaps are drawn at up to 2x: sharp on any launcher, well inside the RemoteViews size limit. */
private fun scale(context: Context) = min(context.resources.displayMetrics.density, 2f)

@Composable
private fun Sky(
    data: WidgetData,
    arcTop: Float,
    arcBottom: Float,
    markers: Boolean,
    arc: Boolean,
    strip: Dp = 0.dp,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val snapshot = data.snapshot ?: return
    val d = scale(context)
    val bitmap = remember(size, data, arcTop, arcBottom, markers, arc, strip) {
        SkyPainter.paint(
            SkyPainter.Spec(
                widthPx = (size.width.value * d).toInt().coerceAtLeast(1),
                heightPx = (size.height.value * d).toInt().coerceAtLeast(1),
                density = d,
                snapshot = snapshot,
                now = data.now,
                arcTop = arcTop,
                arcBottom = arcBottom,
                markers = markers,
                arc = arc,
                stripPx = (strip.value * d).toInt(),
                cornerPx = Corner.value * d,
            ),
        )
    }
    Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
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
        Text(
            context.getString(R.string.widget_no_location),
            style = TextStyle(color = White, fontSize = 13.sp, textAlign = TextAlign.Center),
            modifier = GlanceModifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
internal fun NextPrayerContent(data: WidgetData) {
    val snapshot = data.snapshot ?: return NoLocation()
    val context = LocalContext.current
    val next = snapshot.next
    val name = context.getString(next.prayer.labelRes())
    val time = data.time.format(next.time, snapshot.zone)
    val compact = LocalSize.current.height < 100.dp
    val description = context.getString(R.string.widget_next_description, name, time, snapshot.locationName)
    Box(GlanceModifier.fillMaxSize().cornerRadius(Corner).openToday(context).semantics { contentDescription = description }) {
        if (compact) {
            Sky(data, 0f, 0f, markers = false, arc = false)
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(name, style = TextStyle(color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    Spacer(GlanceModifier.padding(start = 8.dp))
                    Text(time, style = TextStyle(color = White, fontSize = 24.sp, fontFamily = FontFamily("sans-serif-light")), maxLines = 1)
                }
                Countdown(context, next.time, data.now, data.colors, 12.sp, white = true)
            }
        } else {
            Sky(data, arcTop = 0.74f, arcBottom = 0.92f, markers = true, arc = true)
            Column(GlanceModifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
                Text(
                    context.getString(
                        if (snapshot.nextSlot == null) R.string.widget_next_label_tomorrow else R.string.widget_next_label,
                        snapshot.locationName,
                    ),
                    style = TextStyle(color = WhiteSoft, fontSize = 11.sp),
                    maxLines = 1,
                )
                Text(name, style = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(time, style = TextStyle(color = White, fontSize = 36.sp, fontFamily = FontFamily("sans-serif-light")), maxLines = 1)
                Countdown(context, next.time, data.now, data.colors, 12.sp, white = true)
            }
        }
    }
}

private val DayLabel = DateTimeFormatter.ofPattern("EEEE, d MMM", AppLocale)

@Composable
internal fun DayScheduleContent(data: WidgetData) {
    val snapshot = data.snapshot ?: return NoLocation()
    val context = LocalContext.current
    val size = LocalSize.current
    val nextSlot = snapshot.nextSlot
    val next = snapshot.next
    val nextName = context.getString(next.prayer.labelRes())
    // Room for the sun's path only when the widget is tall enough; otherwise the strip says it.
    val roomy = size.height >= 170.dp
    val description = buildString {
        append(DayLabel.format(snapshot.date)).append(", ").append(data.hijri).append(", ").append(snapshot.locationName).append(". ")
        snapshot.today.forEach { append(context.getString(it.slot.labelRes())).append(' ').append(data.time.format(it.time, snapshot.zone)).append(". ") }
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(Corner).openToday(context).semantics { contentDescription = description }) {
        val stripTop = 1f - (StripHeight.value + 8f) / size.height.value
        Sky(
            data,
            arcTop = 0.36f,
            arcBottom = (stripTop - 0.06f).coerceAtLeast(0.4f),
            markers = true,
            arc = roomy,
            strip = StripHeight,
        )
        Column(GlanceModifier.fillMaxSize()) {
            Column(GlanceModifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp)) {
                Text(
                    context.getString(R.string.widget_day_header, DayLabel.format(snapshot.date), data.hijri),
                    style = TextStyle(color = WhiteSoft, fontSize = 11.sp),
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(nextName, style = TextStyle(color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    Spacer(GlanceModifier.padding(start = 8.dp))
                    Text(
                        data.time.format(next.time, snapshot.zone),
                        style = TextStyle(color = White, fontSize = 24.sp, fontFamily = FontFamily("sans-serif-light")),
                        maxLines = 1,
                    )
                }
                Countdown(
                    context, next.time, data.now, data.colors, 12.sp, white = true,
                    format = context.getString(R.string.widget_countdown_place, snapshot.locationName),
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth().height(StripHeight).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                val d = scale(context)
                val pill = remember(size) { SkyPainter.pill(((size.width.value - 24f) / 6f * d).toInt(), (44 * d).toInt(), d) }
                snapshot.today.forEach { slot ->
                    val isNext = slot.slot == nextSlot
                    Box(GlanceModifier.defaultWeight().height(44.dp), contentAlignment = Alignment.Center) {
                        if (isNext) {
                            Image(ImageProvider(pill), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = GlanceModifier.fillMaxSize())
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                context.getString(slot.slot.labelRes()),
                                style = TextStyle(color = if (isNext) White else WhiteSoft, fontSize = 10.sp, textAlign = TextAlign.Center),
                                maxLines = 1,
                            )
                            Text(
                                data.time.format(slot.time, snapshot.zone),
                                style = TextStyle(color = White, fontSize = 14.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
        }
    }
}
