package id.haho.muslimdaily.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.labelRes
import java.time.format.DateTimeFormatter

/** Next prayer: name, time and a live countdown. 2×2 by default, resizable down to 2×1. */
class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 50.dp), DpSize(110.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { NextPrayerContent(data) }
    }
}

class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}

/** Today's six times in a row, the next one highlighted, with a countdown. 4×2 by default. */
class DayScheduleWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { DayScheduleContent(data) }
    }
}

class DayScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayScheduleWidget()
}

@Composable
private fun NoLocation(data: WidgetData) {
    val context = LocalContext.current
    GlassPanel(data.colors, GlanceModifier.fillMaxSize().openToday(context)) {
        Box(GlanceModifier.fillMaxSize().padding(WidgetPadding), contentAlignment = Alignment.Center) {
            Text(
                context.getString(R.string.widget_no_location),
                style = TextStyle(color = data.colors.onSurface, fontSize = 13.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
internal fun NextPrayerContent(data: WidgetData) {
    val snapshot = data.snapshot ?: return NoLocation(data)
    val context = LocalContext.current
    val c = data.colors
    val next = snapshot.next
    val name = context.getString(next.prayer.labelRes())
    val time = data.time.format(next.time, snapshot.zone)
    val compact = LocalSize.current.height < 100.dp
    val description = context.getString(R.string.widget_next_description, name, time, snapshot.locationName)
    GlassPanel(c, GlanceModifier.fillMaxSize().openToday(context).semantics { contentDescription = description }) {
        if (compact) {
            // Rows with an embedded chronometer lay out unreliably in RemoteViews, so the compact
            // form stacks: name and time, then the countdown.
            Column(
                GlanceModifier.fillMaxSize().padding(horizontal = WidgetPadding, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$name  $time", style = TextStyle(color = c.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Countdown(context, next.time, data.now, c, 13.sp)
            }
        } else {
            Column(GlanceModifier.fillMaxSize().padding(WidgetPadding), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    context.getString(R.string.widget_next_label, snapshot.locationName),
                    style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(name, style = TextStyle(color = c.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(time, style = TextStyle(color = c.onSurface, fontSize = 34.sp), maxLines = 1)
                Countdown(context, next.time, data.now, c, 13.sp)
            }
        }
    }
}

private val DayLabel = DateTimeFormatter.ofPattern("EEEE, d MMM", AppLocale)

@Composable
internal fun DayScheduleContent(data: WidgetData) {
    val snapshot = data.snapshot ?: return NoLocation(data)
    val context = LocalContext.current
    val c = data.colors
    val nextSlot = snapshot.nextSlot
    val description = buildString {
        append(DayLabel.format(snapshot.date)).append(", ").append(snapshot.locationName).append(". ")
        snapshot.today.forEach { append(context.getString(it.slot.labelRes())).append(' ').append(data.time.format(it.time, snapshot.zone)).append(". ") }
    }
    GlassPanel(c, GlanceModifier.fillMaxSize().openToday(context).semantics { contentDescription = description }) {
        Column(GlanceModifier.fillMaxSize().padding(WidgetPadding)) {
            Text(
                context.getString(R.string.widget_day_header, DayLabel.format(snapshot.date), snapshot.locationName),
                style = TextStyle(color = c.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Countdown(
                context, snapshot.next.time, data.now, c, 13.sp,
                format = context.getString(R.string.widget_countdown_named, context.getString(snapshot.next.prayer.labelRes())),
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth()) {
                snapshot.today.forEach { slot ->
                    val isNext = slot.slot == nextSlot
                    Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                        if (isNext) {
                            Image(
                                ImageProvider(R.drawable.widget_pill),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(c.highlight),
                                contentScale = ContentScale.FillBounds,
                                modifier = GlanceModifier.fillMaxWidth().height(52.dp),
                            )
                        }
                        Column(GlanceModifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val fg = if (isNext) c.onHighlight else c.onSurfaceVariant
                            Text(context.getString(slot.slot.labelRes()), style = TextStyle(color = fg, fontSize = 11.sp, textAlign = TextAlign.Center), maxLines = 1)
                            Text(
                                data.time.format(slot.time, snapshot.zone),
                                style = TextStyle(color = if (isNext) c.onHighlight else c.onSurface, fontSize = 15.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
