package id.haho.muslimdaily.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.notifications.Destination
import id.haho.muslimdaily.notifications.Notifier
import java.time.Clock
import java.time.Instant

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun settings(): SettingsRepository
    fun calculator(): PrayerCalculator
    fun clock(): Clock
}

/**
 * Everything the widget draws, read once per update. [model] is null until a location is set.
 * [elapsed] is [SystemClock.elapsedRealtime] at [now], so a countdown drawn later (Glance
 * recomposes a live session on resize) still ends at the prayer time.
 */
class WidgetData(
    val model: WidgetModel?,
    val time: TimeFormatter,
    val now: Instant,
    val elapsed: Long = SystemClock.elapsedRealtime(),
)

/** The chronometer base (in elapsed realtime) that reaches zero at [target], from a [now] read at [elapsedAtNow]. */
fun chronometerBase(target: Instant, now: Instant, elapsedAtNow: Long): Long = elapsedAtNow + (target.toEpochMilli() - now.toEpochMilli())

suspend fun loadWidgetData(context: Context): WidgetData {
    val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val settings = deps.settings().current()
    val elapsed = SystemClock.elapsedRealtime()
    val now = Instant.now(deps.clock())
    return WidgetData(
        model = WidgetModels.at(now, settings, deps.calculator()),
        time = TimeFormatter(DateFormat.is24HourFormat(context)),
        now = now,
        elapsed = elapsed,
    )
}

/** Bumped by [GlanceRefresher] so a widget re-reads its data even while its session is alive. */
val WidgetRefreshKey = longPreferencesKey("refreshed_at")

/**
 * The widget's data, reloaded whenever [WidgetRefreshKey] changes. Glance keeps a session alive
 * for a while after an update and recomposes it instead of calling provideGlance again, so data
 * read once in provideGlance would go stale.
 */
@Composable
fun rememberWidgetData(context: Context, initial: WidgetData): WidgetData {
    val stamp = currentState(WidgetRefreshKey) ?: 0L
    val data by produceState(initial, stamp) { if (stamp != 0L) value = loadWidgetData(context) }
    return data
}

/** Taps open the Today screen. */
fun GlanceModifier.openToday(context: Context): GlanceModifier = clickable(
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Notifier.EXTRA_DESTINATION, Destination.TODAY.name),
    ),
)

/** Text on the sky is white; text on the white pill is the night sky's darkest blue. */
const val OnSky: Int = 0xFFFFFFFF.toInt()
const val OnPill: Int = 0xFF0B1A3A.toInt()

/**
 * A widget label: a small RemoteViews layout whose TextView asks for Plus Jakarta Sans (Regular or
 * SemiBold). Launchers inflate it in a restricted context, where TextView does not load font
 * resources, so on phones it shows in the system font; only previews and tests show the app's.
 * Sizes are in sp, so the system font size still applies.
 */
@Composable
fun Label(
    text: CharSequence,
    sizeSp: Float,
    modifier: GlanceModifier = GlanceModifier,
    semibold: Boolean = false,
    color: Int = OnSky,
    maxLines: Int = 1,
    center: Boolean = false,
    /** Take the space [modifier] gives (e.g. a weight) and shorten with "…" instead of growing. */
    fill: Boolean = false,
    /** At the end of the width [modifier] gives (regular weight only). */
    end: Boolean = false,
) {
    val context = LocalContext.current
    val layout = when {
        semibold -> R.layout.widget_text_semibold
        end -> R.layout.widget_text_end
        else -> R.layout.widget_text
    }
    val views = RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.text, text)
        setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.text, color)
        if (maxLines != 1) setInt(R.id.text, "setMaxLines", maxLines)
        if (center) setInt(R.id.text, "setGravity", Gravity.CENTER)
    }
    // An embedded RemoteViews fills its parent unless told otherwise, pushing siblings out.
    AndroidRemoteViews(views, if (fill) modifier else modifier.wrapContentSize())
}

/** How much smaller a 12-hour "AM"/"PM" is set than the digits. */
private const val MeridiemScale = 0.7f
private val Meridiem = Regex("""\s+\p{L}[\p{L}.]*$""")

/** A clock time with its 12-hour "AM"/"PM", if any, set smaller, so narrow columns keep large digits. */
fun clockText(time: String): CharSequence {
    val m = Meridiem.find(time) ?: return time
    return SpannableString(time).apply { setSpan(RelativeSizeSpan(MeridiemScale), m.range.first, time.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
}

/** [clockText]'s parts at their sizes, for [TextFit]. */
fun clockParts(time: String, sp: Float): Array<Pair<String, Float>> {
    val m = Meridiem.find(time) ?: return arrayOf(time to sp)
    return arrayOf(time.substring(0, m.range.first) to sp, m.value to sp * MeridiemScale)
}

/** One line of a layout: its texts side by side at their sizes, plus the fixed padding and gaps between them. */
class FitLine(val extra: Dp, vararg val parts: Pair<String, Float>)

/**
 * The factor (at most 1) the type must shrink by for every line to fit [width], so a 12-hour
 * "11:41 AM", a long name or a large system font scales the layout down instead of ending in "…".
 * Measured with the widget's own SemiBold label (Plus Jakarta Sans, wider than the system font
 * launchers fall back to), so the estimate matches what the label needs.
 */
class TextFit(context: Context) {
    private val metrics = context.resources.displayMetrics
    private val paint = TextPaint((LayoutInflater.from(context).inflate(R.layout.widget_text_semibold, null) as TextView).paint)

    /** Width in dp, like the widget's size; sp follow the system font size as the label's do. */
    fun width(text: String, sp: Float): Float {
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
        return paint.measureText(text) / metrics.density
    }

    fun factor(width: Dp, vararg lines: FitLine): Float = lines.minOfOrNull { line ->
        val room = (width - line.extra).value * Slack
        // Re-measured at each smaller size: with Android 14's nonlinear font scaling, small type
        // grows more with the system font than large type does, so one ratio is not enough.
        var f = 1f
        repeat(4) {
            val text = line.parts.sumOf { (s, sp) -> width(s, sp * f).toDouble() }.toFloat()
            if (text <= room || text <= 0f) return@minOfOrNull f
            f *= (room / text).coerceAtLeast(0f)
        }
        f
    } ?: 1f

    private companion object {
        /** Room for a launcher font a little wider than the one measured. */
        const val Slack = 0.96f
    }
}

/** The next prayer's time on a white pill: marked by shape and weight, not colour alone. */
@Composable
fun Pill(text: CharSequence, sizeSp: Float, padH: Dp = 7.dp, padV: Dp = 0.dp) {
    Box(GlanceModifier.background(ImageProvider(R.drawable.widget_pill)).padding(horizontal = padH, vertical = padV)) {
        Label(text, sizeSp, semibold = true, color = OnPill)
    }
}

/**
 * A live countdown that reaches zero at [base] (see [chronometerBase]). Glance has no chronometer,
 * so this embeds a RemoteViews one; the launcher ticks it every second without waking the app.
 */
@Composable
fun Countdown(
    base: Long,
    sizeSp: Float,
    format: String = LocalContext.current.getString(R.string.widget_countdown_format),
    semibold: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, if (semibold) R.layout.widget_countdown_semibold else R.layout.widget_countdown).apply {
        setChronometer(R.id.countdown, base, format, true)
        setChronometerCountDown(R.id.countdown, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.countdown, OnSky)
    }
    AndroidRemoteViews(views, modifier.wrapContentSize())
}
