package id.haho.muslimdaily.wear

import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Wear OS tile: the next prayer over a night-sky panel, with the two after it. The tile carries
 * a timeline (one entry per prayer boundary for the week the phone sent), so the watch moves to
 * the next prayer on its own without waking the phone.
 */
class PrayerTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        SuspendToFutureAdapter.launchFuture {
            val data = PrayerData.load(this@PrayerTileService)
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(if (data == null || data.times.isEmpty()) single(noData()) else timeline(data))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        SuspendToFutureAdapter.launchFuture {
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    private fun timeline(data: PrayerData): TimelineBuilders.Timeline {
        val now = Instant.now()
        val builder = TimelineBuilders.Timeline.Builder()
        // Each entry is valid from one prayer until the next; the first starts now.
        val upcoming = data.upcoming(now)
        var start = now
        fun add(root: LayoutElementBuilders.LayoutElement, from: Instant, until: Instant?) {
            val validity = TimelineBuilders.TimeInterval.Builder().setStartMillis(from.toEpochMilli())
            until?.let { validity.setEndMillis(it.toEpochMilli()) }
            builder.addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build())
                    .setValidity(validity.build())
                    .build(),
            )
        }
        upcoming.forEachIndexed { i, slot ->
            val rest = upcoming.drop(i)
            // "Besok" holds until midnight, so an entry that spans it is split there.
            val midnight = slot.day.atStartOfDay(data.zone).toInstant()
            if (start < midnight && midnight < slot.time) {
                add(nextLayout(data, rest, start), start, midnight)
                start = midnight
            }
            add(nextLayout(data, rest, start), start, slot.time)
            start = slot.time
        }
        // After the last prayer we know of, ask for the phone rather than show a stale time.
        add(noData(), start, null)
        return builder.build()
    }

    private fun single(root: LayoutElementBuilders.LayoutElement) = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(TimelineBuilders.TimelineEntry.Builder().setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build()).build())
        .build()

    private fun label(slot: PrayerData.Slot): String =
        if (slot.isFridayDhuhr) getString(R.string.jumuah) else resources.getStringArray(R.array.slots)[slot.slot]

    private fun clock(data: PrayerData, t: Instant): String =
        Locale.getDefault().let { l ->
            // Indonesian writes 04.32, English 04:32.
            val sep = if (l.language == "en") ":" else "."
            DateTimeFormatter.ofPattern(if (data.is24) "HH${sep}mm" else "h${sep}mm", l).format(t.atZone(data.zone))
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = Text.Builder()
        .setText(value)
        .setMaxLines(1)
        .setFontStyle(
            FontStyle.Builder()
                .setSize(sp(size))
                .setColor(argb(color))
                .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build(),
        )
        .build()

    /** The next prayer as shown from [shownFrom], the start of its timeline entry. */
    private fun nextLayout(data: PrayerData, upcoming: List<PrayerData.Slot>, shownFrom: Instant): LayoutElementBuilders.LayoutElement {
        val next = upcoming.first()
        val today = shownFrom.atZone(data.zone).toLocalDate()
        val header = getString(if (next.day.isAfter(today)) R.string.tile_tomorrow else R.string.tile_next, data.place)
        val after = upcoming.drop(1).take(2).joinToString("   ") { "${label(it)} ${clock(data, it.time)}" }
        val column = Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(header, 12f, SOFT))
            .addContent(Spacer.Builder().setHeight(dp(4f)).build())
            .addContent(text(label(next), 18f, GOLD, bold = true))
            .addContent(text(clock(data, next.time), 44f, WHITE))
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(text(after, 12f, SOFT))
            .build()
        return panel(column)
    }

    private fun noData(): LayoutElementBuilders.LayoutElement = panel(
        Text.Builder()
            .setText(getString(R.string.tile_no_data))
            .setMaxLines(4)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .setFontStyle(FontStyle.Builder().setSize(sp(14f)).setColor(argb(WHITE)).build())
            .build(),
    )

    /** Full-screen night-sky panel with the content centred. */
    private fun panel(content: LayoutElementBuilders.LayoutElement): LayoutElementBuilders.LayoutElement = Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setBackground(ModifiersBuilders.Background.Builder().setColor(argb(NIGHT)).build())
                .setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(14f)).build())
                .build(),
        )
        .addContent(content)
        .build()

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val NIGHT = 0xFF0B1530.toInt()
        const val GOLD = 0xFFE3C46A.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val SOFT = 0xC8FFFFFF.toInt()
    }
}
