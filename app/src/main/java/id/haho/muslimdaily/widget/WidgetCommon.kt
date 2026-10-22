package id.haho.muslimdaily.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.wrapContentSize
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.theme.ThemeMode
import id.haho.muslimdaily.core.designsystem.theme.colorSchemeFor
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshot
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
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

/** Everything a widget draws, read once per update. */
class WidgetData(
    val snapshot: PrayerSnapshot?,
    val colors: WidgetColors,
    val time: TimeFormatter,
    val now: Instant,
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val settings = deps.settings().current()
    val now = Instant.now(deps.clock())
    return WidgetData(
        snapshot = PrayerSnapshots.at(now, settings, deps.calculator()),
        colors = WidgetColors.of(settings),
        time = TimeFormatter(DateFormat.is24HourFormat(context)),
        now = now,
    )
}

/**
 * Widget colours from the app's palette. Widgets can't blur what is behind them, so the glass is
 * a translucent fill with a light rim. Day/night follow the app's theme mode; "system" lets the
 * launcher switch with the device.
 */
class WidgetColors(
    val glass: ColorProvider,
    val rim: ColorProvider,
    val onSurface: ColorProvider,
    val onSurfaceVariant: ColorProvider,
    val primary: ColorProvider,
    val highlight: ColorProvider,
    val onHighlight: ColorProvider,
    /** Raw colours for the countdown, which is a plain RemoteViews Chronometer. */
    val countdownDay: Color,
    val countdownNight: Color,
) {
    companion object {
        fun of(settings: AppSettings): WidgetColors {
            val light = colorSchemeFor(settings.palette, dark = false)
            val dark = colorSchemeFor(settings.palette, dark = true)
            val day = when (settings.themeMode) { ThemeMode.DARK -> dark; else -> light }
            val night = when (settings.themeMode) { ThemeMode.LIGHT -> light; else -> dark }
            fun pick(f: (androidx.compose.material3.ColorScheme) -> Color) = ColorProvider(day = f(day), night = f(night))
            return WidgetColors(
                // Opaque here: the panel and rim drawables carry the alpha (a tint's alpha is dropped).
                glass = ColorProvider(day = if (day === dark) day.surfaceContainer else day.surface, night = if (night === dark) night.surfaceContainer else night.surface),
                rim = ColorProvider(day = Color.White, night = Color.White),
                onSurface = pick { it.onSurface },
                onSurfaceVariant = pick { it.onSurfaceVariant },
                primary = pick { it.primary },
                highlight = pick { it.secondaryContainer },
                onHighlight = pick { it.onSecondaryContainer },
                countdownDay = day.primary,
                countdownNight = night.primary,
            )
        }
    }
}

/** Translucent rounded panel with a light rim: the glass look within what widgets can draw. */
@Composable
fun GlassPanel(colors: WidgetColors, modifier: GlanceModifier = GlanceModifier, content: @Composable () -> Unit) {
    Box(modifier) {
        Image(
            ImageProvider(R.drawable.widget_panel),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.glass),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Image(
            ImageProvider(R.drawable.widget_rim),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.rim),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
        content()
    }
}

/** Taps open the Today screen. */
fun GlanceModifier.openToday(context: Context): GlanceModifier = clickable(
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Notifier.EXTRA_DESTINATION, Destination.TODAY.name),
    ),
)

/**
 * A live countdown to [target]. Glance has no chronometer, so this embeds a RemoteViews one; the
 * launcher ticks it every second without waking the app.
 */
@Composable
fun Countdown(
    context: Context,
    target: Instant,
    now: Instant,
    colors: WidgetColors,
    size: TextUnit,
    format: String = context.getString(R.string.widget_countdown_format),
    modifier: GlanceModifier = GlanceModifier,
) {
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        val base = SystemClock.elapsedRealtime() + (target.toEpochMilli() - now.toEpochMilli())
        setChronometer(R.id.countdown, base, format, true)
        setChronometerCountDown(R.id.countdown, true)
        setTextViewTextSize(R.id.countdown, android.util.TypedValue.COMPLEX_UNIT_SP, size.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorInt(R.id.countdown, "setTextColor", colors.countdownDay.toArgb(), colors.countdownNight.toArgb())
        } else {
            setTextColor(R.id.countdown, colors.countdownDay.toArgb())
        }
    }
    // An embedded RemoteViews fills its parent unless told otherwise, pushing siblings out.
    AndroidRemoteViews(views, modifier.wrapContentSize())
}

val WidgetPadding: Dp = 14.dp
