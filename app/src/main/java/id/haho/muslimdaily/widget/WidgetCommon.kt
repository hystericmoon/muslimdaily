package id.haho.muslimdaily.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
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

/** Everything the widget draws, read once per update. [model] is null until a location is set. */
class WidgetData(
    val model: WidgetModel?,
    val time: TimeFormatter,
    val now: Instant,
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val settings = deps.settings().current()
    val now = Instant.now(deps.clock())
    return WidgetData(
        model = WidgetModels.at(now, settings, deps.calculator()),
        time = TimeFormatter(DateFormat.is24HourFormat(context)),
        now = now,
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
 * Text in the app's typeface. Glance can only name system fonts, so each label is a small
 * RemoteViews layout whose TextView sets Plus Jakarta Sans (Regular or SemiBold, as in the app).
 * Sizes are in sp, so the system font size still applies.
 */
@Composable
fun Label(
    text: String,
    sizeSp: Float,
    modifier: GlanceModifier = GlanceModifier,
    semibold: Boolean = false,
    color: Int = OnSky,
    maxLines: Int = 1,
    center: Boolean = false,
    /** Take the space [modifier] gives (e.g. a weight) and shorten with "…" instead of growing. */
    fill: Boolean = false,
) {
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, if (semibold) R.layout.widget_text_semibold else R.layout.widget_text).apply {
        setTextViewText(R.id.text, text)
        setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.text, color)
        if (maxLines != 1) setInt(R.id.text, "setMaxLines", maxLines)
        if (center) setInt(R.id.text, "setGravity", Gravity.CENTER)
    }
    // An embedded RemoteViews fills its parent unless told otherwise, pushing siblings out.
    AndroidRemoteViews(views, if (fill) modifier else modifier.wrapContentSize())
}

/** The next prayer's time on a white pill: marked by shape and weight, not colour alone. */
@Composable
fun Pill(text: String, sizeSp: Float, padH: Dp = 7.dp, padV: Dp = 0.dp) {
    Box(GlanceModifier.background(ImageProvider(R.drawable.widget_pill)).padding(horizontal = padH, vertical = padV)) {
        Label(text, sizeSp, semibold = true, color = OnPill)
    }
}

/**
 * A live countdown to [target]. Glance has no chronometer, so this embeds a RemoteViews one;
 * the launcher ticks it every second without waking the app.
 */
@Composable
fun Countdown(
    target: Instant,
    now: Instant,
    sizeSp: Float,
    format: String = LocalContext.current.getString(R.string.widget_countdown_format),
    semibold: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, if (semibold) R.layout.widget_countdown_semibold else R.layout.widget_countdown).apply {
        val base = SystemClock.elapsedRealtime() + (target.toEpochMilli() - now.toEpochMilli())
        setChronometer(R.id.countdown, base, format, true)
        setChronometerCountDown(R.id.countdown, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.countdown, OnSky)
    }
    AndroidRemoteViews(views, modifier.wrapContentSize())
}
