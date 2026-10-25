package id.haho.muslimdaily.feature.settings

import id.haho.muslimdaily.core.ui.clockPattern
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.glass.GlassAlertDialog
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.AppScreen
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.NavigationRow
import id.haho.muslimdaily.core.designsystem.component.SegmentedRow
import id.haho.muslimdaily.core.designsystem.component.SettingsGroup
import id.haho.muslimdaily.core.designsystem.component.StepperRow
import id.haho.muslimdaily.core.designsystem.component.SwitchRow
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.ui.AppLocale
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsEditor
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.notifications.Notifier
import id.haho.muslimdaily.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import id.haho.muslimdaily.widget.GlanceRefresher
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val glance: GlanceRefresher,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exactAllowed() = scheduler.canScheduleExact()
    fun notificationsAllowed() = notifier.canPost()

    fun update(block: SettingsEditor.() -> Unit) {
        viewModelScope.launch { repository.update(block) }
    }

    /** After a permission change: re-plan alarms and post the lock-screen notification if it can now show. */
    fun reschedule() {
        viewModelScope.launch {
            scheduler.rescheduleAll()
            glance.refresh()
        }
    }
}

data class PermissionState(val notifications: Boolean, val exactAlarms: Boolean)

@Composable
fun RemindersRoute(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(PermissionState(viewModel.notificationsAllowed(), viewModel.exactAllowed())) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = PermissionState(viewModel.notificationsAllowed(), viewModel.exactAllowed())
        viewModel.reschedule()
    }
    var refocus by remember { mutableIntStateOf(0) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissions = permissions.copy(notifications = viewModel.notificationsAllowed())
        viewModel.reschedule()
        refocus++
    }
    val askNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    }
    val askExact = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
        }
    }
    AppScreen(title = stringResource(R.string.settings_reminders), onBack = onBack) {
        settings?.let { s ->
            RemindersContent(
                settings = s,
                permissions = permissions,
                onAskNotifications = askNotifications,
                onAskExact = askExact,
                refocus = refocus,
                update = { block ->
                    viewModel.update(block)
                    if (!permissions.notifications) askNotifications()
                },
            )
        }
    }
}

private val GROUPS = listOf("adhan", "adhkar", "glance", "other")
private const val PROMPT_SETTLE_MS = 500L

private val TimeFormat get() = DateTimeFormatter.ofPattern(clockPattern(true), AppLocale)

@Composable
fun RemindersContent(
    settings: AppSettings,
    permissions: PermissionState,
    onAskNotifications: () -> Unit,
    onAskExact: () -> Unit,
    update: (SettingsEditor.() -> Unit) -> Unit,
    /** Bumped after a permission prompt closes: focus returns to the group that asked for it. */
    refocus: Int = 0,
) {
    val r = settings.reminders
    var timeDialog by remember { mutableStateOf<TimeTarget?>(null) }
    // When the app's window comes back after the system prompt, TalkBack would start again at the
    // tab bar. Requesting focus in the group whose setting asked keeps it there; on API 37 TalkBack
    // then returns to the option that was just chosen.
    val groupFocus = remember { GROUPS.associateWith { FocusRequester() } }
    var lastGroup by rememberSaveable { mutableStateOf(GROUPS.first()) }
    LaunchedEffect(refocus) {
        if (refocus > 0) {
            delay(PROMPT_SETTLE_MS)
            runCatching { groupFocus.getValue(lastGroup).requestFocus() }
        }
    }
    fun inGroup(key: String): (SettingsEditor.() -> Unit) -> Unit = { block -> lastGroup = key; update(block) }
    fun titleFocus(key: String) = Modifier.focusRequester(groupFocus.getValue(key)).focusable()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)) {
        // Stable keys: when a permission card goes away, the groups below keep their identity, so
        // TalkBack's focus stays where it was instead of jumping back to the top.
        item(key = "intro") {
            InfoNote(
                stringResource(R.string.reminders_intro),
                Modifier.padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.sm),
            )
        }
        if (!permissions.notifications) {
            item(key = "perm-notifications") { PermissionCard(stringResource(R.string.reminders_notif_permission), stringResource(R.string.reminders_allow), onAskNotifications) }
        }
        if (!permissions.exactAlarms) {
            item(key = "perm-exact") { PermissionCard(stringResource(R.string.reminders_exact_permission), stringResource(R.string.reminders_allow_exact), onAskExact) }
        }
        item(key = "adhan") {
            val update = inGroup("adhan")
            SettingsGroup(stringResource(R.string.reminders_adhan), titleModifier = titleFocus("adhan")) {
                if (settings.location == null) {
                    InfoNote(stringResource(R.string.reminders_need_location), Modifier.padding(AppTheme.dimens.screenGutter))
                }
                FIVE_PRAYERS.forEach { prayer ->
                    SegmentedRow(
                        title = stringResource(prayer.labelRes()),
                        options = AdhanMode.entries,
                        selected = r.adhan[prayer] ?: AdhanMode.OFF,
                        onSelect = { mode -> update { setAdhan(prayer, mode) } },
                        label = {
                            stringResource(
                                when (it) {
                                    AdhanMode.OFF -> R.string.settings_off
                                    AdhanMode.SOUND -> R.string.reminders_sound
                                    AdhanMode.SILENT -> R.string.reminders_silent
                                },
                            )
                        },
                    )
                }
                InfoNote(stringResource(R.string.reminders_adhan_note), Modifier.padding(AppTheme.dimens.screenGutter))
            }
        }
        item(key = "adhkar") {
            val update = inGroup("adhkar")
            SettingsGroup(stringResource(R.string.reminders_adhkar), titleModifier = titleFocus("adhkar")) {
                SwitchRow(
                    title = stringResource(R.string.adhkar_morning),
                    summary = stringResource(R.string.reminders_after_fajr, r.morningOffsetMinutes),
                    checked = r.morningAdhkar,
                    onCheckedChange = { v -> update { remMorning = v } },
                    icon = R.drawable.ic_wb_sunny,
                )
                if (r.morningAdhkar) OffsetStepper(r.morningOffsetMinutes) { v -> update { remMorningOffset = v } }
                SwitchRow(
                    title = stringResource(R.string.adhkar_evening),
                    summary = stringResource(R.string.reminders_after_asr, r.eveningOffsetMinutes),
                    checked = r.eveningAdhkar,
                    onCheckedChange = { v -> update { remEvening = v } },
                    icon = R.drawable.ic_wb_twilight,
                )
                if (r.eveningAdhkar) OffsetStepper(r.eveningOffsetMinutes) { v -> update { remEveningOffset = v } }
            }
        }
        item(key = "glance") {
            val update = inGroup("glance")
            SettingsGroup(stringResource(R.string.reminders_glance), titleModifier = titleFocus("glance")) {
                SwitchRow(
                    title = stringResource(R.string.reminders_mosque_title),
                    summary = stringResource(R.string.reminders_mosque_summary, r.mosqueBeforeMinutes),
                    checked = r.mosqueBefore,
                    onCheckedChange = { v -> update { remMosque = v } },
                    icon = R.drawable.ic_mosque,
                )
                if (r.mosqueBefore) LeadStepper(r.mosqueBeforeMinutes) { v -> update { remMosqueMinutes = v } }
                SwitchRow(
                    title = stringResource(R.string.reminders_ongoing_title),
                    summary = stringResource(R.string.reminders_ongoing_summary),
                    checked = r.nextPrayerOngoing,
                    onCheckedChange = { v -> update { remOngoing = v } },
                    icon = R.drawable.ic_schedule,
                )
                InfoNote(stringResource(R.string.reminders_widget_note), Modifier.padding(AppTheme.dimens.screenGutter))
            }
        }
        item(key = "other") {
            val update = inGroup("other")
            SettingsGroup(stringResource(R.string.reminders_other), titleModifier = titleFocus("other")) {
                SwitchRow(
                    title = stringResource(R.string.notif_sleep_title),
                    checked = r.beforeSleep,
                    onCheckedChange = { v -> update { remSleep = v } },
                    icon = R.drawable.ic_bedtime,
                )
                if (r.beforeSleep) TimeRow(r.beforeSleepTime) { timeDialog = TimeTarget.SLEEP }
                SwitchRow(
                    title = stringResource(R.string.notif_friday_title),
                    summary = stringResource(R.string.notif_friday_text),
                    checked = r.friday,
                    onCheckedChange = { v -> update { remFriday = v } },
                    icon = R.drawable.ic_mosque,
                )
                if (r.friday) TimeRow(r.fridayTime) { timeDialog = TimeTarget.FRIDAY }
                SwitchRow(
                    title = stringResource(R.string.reminders_fasting_title),
                    summary = stringResource(R.string.reminders_fasting_summary),
                    checked = r.fasting,
                    onCheckedChange = { v -> update { remFasting = v } },
                    icon = R.drawable.ic_calendar_month,
                )
                if (r.fasting) {
                    TimeRow(r.fastingTime) { timeDialog = TimeTarget.FASTING }
                    SwitchRow(
                        title = stringResource(R.string.reminders_fasting_weekly),
                        checked = r.fastingWeekly,
                        onCheckedChange = { v -> update { remFastingWeekly = v } },
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.notif_review_title),
                    summary = stringResource(R.string.reminders_review_summary),
                    checked = r.reviewQueue,
                    onCheckedChange = { v -> update { remReview = v } },
                    icon = R.drawable.ic_record_voice_over,
                )
                if (r.reviewQueue) TimeRow(r.reviewTime) { timeDialog = TimeTarget.REVIEW }
            }
        }
    }
    timeDialog?.let { target ->
        val initial = when (target) {
            TimeTarget.SLEEP -> r.beforeSleepTime
            TimeTarget.FRIDAY -> r.fridayTime
            TimeTarget.REVIEW -> r.reviewTime
            TimeTarget.FASTING -> r.fastingTime
        }
        TimeDialog(initial, onDismiss = { timeDialog = null }) { time ->
            update {
                when (target) {
                    TimeTarget.SLEEP -> setSleepTime(time)
                    TimeTarget.FRIDAY -> setFridayTime(time)
                    TimeTarget.REVIEW -> setReviewTime(time)
                    TimeTarget.FASTING -> setFastingTime(time)
                }
            }
            timeDialog = null
        }
    }
}

private enum class TimeTarget { SLEEP, FRIDAY, REVIEW, FASTING }

@Composable
private fun PermissionCard(text: String, action: String, onClick: () -> Unit) {
    GlassCard(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Glass.colors.tinted.alpha),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.xs),
    ) {
        Column(Modifier.padding(AppTheme.spacing.lg)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            GlassButton(onClick = onClick, modifier = Modifier.padding(top = AppTheme.spacing.sm)) { Text(action) }
        }
    }
}

@Composable
private fun OffsetStepper(value: Int, onChange: (Int) -> Unit) {
    val title = stringResource(R.string.reminders_offset)
    StepperRow(
        title = title,
        valueText = stringResource(R.string.settings_minutes, value),
        onDecrement = { onChange((value - 5).coerceIn(0, 120)) },
        onIncrement = { onChange((value + 5).coerceIn(0, 120)) },
        decrementDescription = stringResource(R.string.settings_decrease, title),
        incrementDescription = stringResource(R.string.settings_increase, title),
        canDecrement = value > 0,
        canIncrement = value < 120,
    )
}

@Composable
private fun LeadStepper(value: Int, onChange: (Int) -> Unit) {
    val title = stringResource(R.string.reminders_lead)
    StepperRow(
        title = title,
        valueText = stringResource(R.string.settings_minutes, value),
        onDecrement = { onChange((value - 5).coerceIn(5, 30)) },
        onIncrement = { onChange((value + 5).coerceIn(5, 30)) },
        decrementDescription = stringResource(R.string.settings_decrease, title),
        incrementDescription = stringResource(R.string.settings_increase, title),
        canDecrement = value > 5,
        canIncrement = value < 30,
    )
}

@Composable
private fun TimeRow(time: LocalTime, onClick: () -> Unit) {
    NavigationRow(
        title = stringResource(R.string.reminders_time),
        summary = TimeFormat.format(time),
        onClick = onClick,
        icon = R.drawable.ic_schedule,
    )
}

@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state) },
    )
}
