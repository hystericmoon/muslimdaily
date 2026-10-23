package id.haho.muslimdaily.feature.settings.diagnostics

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.InfoRow
import id.haho.muslimdaily.core.designsystem.component.SettingsGroup
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.diagnostics.CrashLog
import id.haho.muslimdaily.core.diagnostics.ReminderLog
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.notifications.Notifier
import id.haho.muslimdaily.notifications.PlannedReminder
import id.haho.muslimdaily.notifications.ReminderPlanner
import id.haho.muslimdaily.notifications.ReminderScheduler
import id.haho.muslimdaily.notifications.ReminderType
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Everything the screen and the feedback email show; read fresh on each resume. */
data class Diagnostics(
    val notifications: Boolean,
    val exact: Boolean,
    /** True when Android may pause the app in the background (battery optimisation on). */
    val batteryRestricted: Boolean,
    val mutedChannels: List<String>,
    val planned: List<PlannedReminder>,
    val fired: List<ReminderLog.Entry>,
    val crashes: List<File>,
    val zone: ZoneId,
    val manufacturer: String,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val calculator: PrayerCalculator,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val clock: Clock,
) : ViewModel() {
    suspend fun load(): Diagnostics {
        val s = settings.current()
        val zone = s.location?.timeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val power = context.getSystemService(PowerManager::class.java)
        return Diagnostics(
            notifications = notifier.canPost(),
            exact = scheduler.canScheduleExact(),
            batteryRestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == false,
            mutedChannels = notifier.mutedChannels(),
            planned = ReminderPlanner.plan(Instant.now(clock), s, calculator)
                .filter { it.type != ReminderType.REFRESH }
                .sortedBy { it.at },
            fired = ReminderLog.entries(context),
            crashes = CrashLog.files(context),
            zone = zone,
            manufacturer = Build.MANUFACTURER.lowercase(),
        )
    }

    fun clearCrashes(onDone: () -> Unit) {
        viewModelScope.launch {
            CrashLog.clear(context)
            onDone()
        }
    }
}

@Composable
fun DiagnosticsRoute(onBack: () -> Unit, viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<Diagnostics?>(null) }
    var reload by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { reload++ }
    androidx.compose.runtime.LaunchedEffect(reload) { state = viewModel.load() }
    AppScreen(title = stringResource(R.string.diag_title), onBack = onBack) {
        state?.let { d ->
            DiagnosticsContent(
                d = d,
                time = TimeFormatter(DateFormat.is24HourFormat(context)),
                onSend = { sendFeedback(context, d) },
                onOpenNotifications = {
                    context.startActivitySafely(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
                onOpenExact = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivitySafely(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                    }
                },
                onOpenBattery = { context.startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                onClearCrashes = { viewModel.clearCrashes { reload++ } },
            )
        }
    }
}

@Composable
fun DiagnosticsContent(
    d: Diagnostics,
    time: TimeFormatter,
    onSend: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenExact: () -> Unit = {},
    onOpenBattery: () -> Unit = {},
    onClearCrashes: () -> Unit = {},
) {
    val gutter = AppTheme.dimens.screenGutter
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)) {
        item {
            Column(Modifier.padding(horizontal = gutter, vertical = AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                InfoNote(stringResource(R.string.diag_intro), icon = R.drawable.ic_lock)
                GlassButton(onClick = onSend, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.diag_send)) }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.diag_permissions)) {
                StatusRow(stringResource(R.string.diag_notifications), d.notifications, onOpenNotifications)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) StatusRow(stringResource(R.string.diag_exact), d.exact, onOpenExact)
                StatusRow(stringResource(R.string.diag_battery), !d.batteryRestricted, onOpenBattery, okText = R.string.diag_battery_free, badText = R.string.diag_battery_restricted)
                if (d.mutedChannels.isNotEmpty()) {
                    InfoNote(stringResource(R.string.diag_muted, d.mutedChannels.joinToString()), Modifier.padding(horizontal = gutter, vertical = AppTheme.spacing.sm))
                }
                oemHint(d.manufacturer)?.let {
                    InfoNote(stringResource(R.string.diag_oem, it), Modifier.padding(horizontal = gutter, vertical = AppTheme.spacing.sm))
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.diag_next)) {
                if (d.planned.isEmpty()) {
                    InfoRow(stringResource(R.string.diag_none_planned))
                } else {
                    d.planned.take(8).forEach { p ->
                        InfoRow(typeLabel(p.type), summary = dateTime(p.at, d.zone, time))
                    }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.diag_recent)) {
                if (d.fired.isEmpty()) {
                    InfoRow(stringResource(R.string.diag_none_fired))
                } else {
                    d.fired.take(10).forEach { e ->
                        val type = runCatching { ReminderType.valueOf(e.type) }.getOrNull()
                        InfoRow(
                            title = type?.let { typeLabel(it) } ?: e.type,
                            summary = stringResource(R.string.diag_fired_at, dateTime(e.due, d.zone, time), lateLabel(e.late)),
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.diag_crashes)) {
                InfoRow(
                    if (d.crashes.isEmpty()) stringResource(R.string.diag_no_crashes) else stringResource(R.string.diag_crash_count, d.crashes.size),
                    summary = d.crashes.firstOrNull()?.let { stringResource(R.string.diag_crash_latest, dateTime(Instant.ofEpochMilli(it.name.removePrefix("crash-").removeSuffix(".txt").toLongOrNull() ?: 0), d.zone, time)) },
                )
                if (d.crashes.isNotEmpty()) {
                    TextButton(onClick = onClearCrashes, modifier = Modifier.padding(horizontal = AppTheme.spacing.sm)) {
                        Text(stringResource(R.string.diag_clear_crashes))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    ok: Boolean,
    onOpen: () -> Unit,
    okText: Int = R.string.diag_allowed,
    badText: Int = R.string.diag_not_allowed,
) {
    Column {
        InfoRow(title, summary = stringResource(if (ok) okText else badText))
        if (!ok) {
            TextButton(onClick = onOpen, modifier = Modifier.padding(horizontal = AppTheme.spacing.sm)) {
                Text(stringResource(R.string.diag_open_settings))
            }
        }
    }
}

@Composable
private fun typeLabel(type: ReminderType): String = typeLabel(LocalContext.current, type)

private fun typeLabel(context: Context, type: ReminderType): String = type.prayer?.let { context.getString(R.string.diag_type_adhan, context.getString(it.labelRes())) }
    ?: context.getString(
        when (type) {
            ReminderType.MORNING_ADHKAR -> R.string.diag_type_morning
            ReminderType.EVENING_ADHKAR -> R.string.diag_type_evening
            ReminderType.BEFORE_SLEEP -> R.string.diag_type_sleep
            ReminderType.FRIDAY -> R.string.diag_type_friday
            ReminderType.REVIEW -> R.string.diag_type_review
            ReminderType.PRE_PRAYER -> R.string.diag_type_pre_prayer
            ReminderType.FASTING -> R.string.diag_type_fasting
            else -> R.string.diag_type_other
        },
    )

private val DayFormat get() = DateTimeFormatter.ofPattern("EEE d MMM", id.haho.muslimdaily.core.ui.AppLocale)

private fun dateTime(at: Instant, zone: ZoneId, time: TimeFormatter) = "${DayFormat.format(at.atZone(zone))}, ${time.format(at, zone)}"

@Composable
private fun lateLabel(late: Duration): String {
    val minutes = late.toMinutes()
    return if (minutes < 1) stringResource(R.string.diag_on_time) else stringResource(R.string.diag_late, minutes)
}

/** Brands whose battery managers are known to stop background alarms (dontkillmyapp.com). */
private fun oemHint(manufacturer: String): String? = when {
    manufacturer in setOf("xiaomi", "redmi", "poco") -> "xiaomi"
    manufacturer in setOf("oppo", "realme", "oneplus") -> manufacturer
    manufacturer == "vivo" -> "vivo"
    manufacturer == "huawei" || manufacturer == "honor" -> manufacturer
    manufacturer == "samsung" -> "samsung"
    manufacturer == "infinix" || manufacturer == "tecno" -> "transsion"
    else -> null
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** The diagnostics as plain text for the feedback email. */
fun diagnosticsText(context: Context, d: Diagnostics): String {
    val time = TimeFormatter(true)
    fun dt(at: Instant) = "${DateTimeFormatter.ofPattern("yyyy-MM-dd").format(at.atZone(d.zone))} ${time.format(at, d.zone)}"
    return buildString {
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Notifications: ${d.notifications}; exact alarms: ${d.exact}; battery restricted: ${d.batteryRestricted}")
        if (d.mutedChannels.isNotEmpty()) appendLine("Muted channels: ${d.mutedChannels.joinToString()}")
        appendLine("Zone: ${d.zone}")
        appendLine()
        appendLine("Next reminders:")
        d.planned.take(8).forEach { appendLine("  ${it.type}  ${dt(it.at)}") }
        appendLine("Recently fired (due → late):")
        d.fired.take(15).forEach { appendLine("  ${it.type}  ${dt(it.due)}  +${it.late.seconds}s  exact=${it.exact}") }
        appendLine("Crash reports: ${d.crashes.size}")
    }
}

/**
 * Opens the email app with a short template for the tester, the diagnostics, and the saved
 * crash reports attached. The user sees everything before sending; the app sends nothing.
 */
fun sendFeedback(context: Context, d: Diagnostics) {
    val export = File(context.cacheDir, "export").apply { mkdirs() }
    val attachments = d.crashes.map { f ->
        val copy = File(export, f.name).apply { writeText(f.readText()) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", copy)
    }
    val body = context.getString(R.string.diag_email_template) + "\n\n---\n" + diagnosticsText(context, d)
    val intent = Intent(if (attachments.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (BuildConfig.REPORT_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diag_email_subject, BuildConfig.VERSION_NAME))
        putExtra(Intent.EXTRA_TEXT, body)
        when (attachments.size) {
            0 -> Unit
            1 -> putExtra(Intent.EXTRA_STREAM, attachments.first())
            else -> putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(attachments))
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Only email apps, not every app that accepts text.
        selector = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.diag_send)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.report_no_email_app, Toast.LENGTH_LONG).show()
    }
}
