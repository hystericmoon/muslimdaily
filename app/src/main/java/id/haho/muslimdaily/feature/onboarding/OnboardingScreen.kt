package id.haho.muslimdaily.feature.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.InfoNote
import id.haho.muslimdaily.core.designsystem.component.SwitchRow
import id.haho.muslimdaily.core.designsystem.glass.GlassButton
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.designsystem.glass.GlassTonalButton
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.data.location.LocationRepository
import id.haho.muslimdaily.data.settings.AdhanMode
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.notifications.Notifier
import id.haho.muslimdaily.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * First-launch setup: location, prayer reminders and the adhkar reminder, each explained before
 * Android's own permission dialog appears. Every choice is optional and can be changed later in
 * Settings; skipping leaves the app fully usable.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val locations: LocationRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun notificationsAllowed() = notifier.canPost()
    fun exactAllowed() = scheduler.canScheduleExact()

    fun useGps(onResult: (LocationRepository.FixResult) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { locations.updateFromGps() }.getOrDefault(LocationRepository.FixResult.Unavailable))
        }
    }

    fun setPrayerReminders(on: Boolean) {
        viewModelScope.launch { settings.update { FIVE_PRAYERS.forEach { setAdhan(it, if (on) AdhanMode.SOUND else AdhanMode.OFF) } } }
    }

    fun setAdhkarReminders(on: Boolean) {
        viewModelScope.launch {
            settings.update {
                remMorning = on
                remEvening = on
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            settings.update { onboardingDone = true }
            scheduler.rescheduleAll()
        }
    }
}

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(viewModel.notificationsAllowed()) }
    var exact by remember { mutableStateOf(viewModel.exactAllowed()) }
    var locationMessage by remember { mutableStateOf<Int?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifications = viewModel.notificationsAllowed()
        exact = viewModel.exactAllowed()
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            locationMessage = R.string.onboarding_location_finding
            viewModel.useGps { r ->
                locationMessage = when (r) {
                    is LocationRepository.FixResult.Success -> null
                    LocationRepository.FixResult.NoPermission -> R.string.location_permission_denied
                    LocationRepository.FixResult.Unavailable -> R.string.location_unavailable
                }
            }
        } else {
            locationMessage = R.string.onboarding_location_denied
        }
    }
    // A reminder is switched on only together with the permission it needs; turning it on is
    // the moment the user understands why the dialog appears.
    var pendingReminder by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted
        pendingReminder?.invoke(granted)
        pendingReminder = null
    }
    fun enable(apply: (Boolean) -> Unit) {
        if (notifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            apply(true)
        } else {
            pendingReminder = apply
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val s = settings ?: return
    OnboardingScreen(
        settings = s,
        notificationsAllowed = notifications,
        exactAllowed = exact,
        locationMessage = locationMessage?.let { stringResource(it) },
        actions = OnboardingActions(
            onUseLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            onPrayerReminders = { on -> if (on) enable(viewModel::setPrayerReminders) else viewModel.setPrayerReminders(false) },
            onAdhkarReminders = { on -> if (on) enable(viewModel::setAdhkarReminders) else viewModel.setAdhkarReminders(false) },
            onAllowExact = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                    }
                }
            },
            onFinish = viewModel::finish,
        ),
    )
}

class OnboardingActions(
    val onUseLocation: () -> Unit = {},
    val onPrayerReminders: (Boolean) -> Unit = {},
    val onAdhkarReminders: (Boolean) -> Unit = {},
    val onAllowExact: () -> Unit = {},
    val onFinish: () -> Unit = {},
)

@Composable
fun OnboardingScreen(
    settings: AppSettings,
    notificationsAllowed: Boolean,
    exactAllowed: Boolean,
    locationMessage: String?,
    actions: OnboardingActions,
) {
    val prayerOn = FIVE_PRAYERS.all { settings.reminders.adhan[it] != AdhanMode.OFF }
    val adhkarOn = settings.reminders.morningAdhkar && settings.reminders.eveningAdhkar
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Step(R.drawable.ic_my_location, stringResource(R.string.onboarding_location_title), stringResource(R.string.onboarding_location_body)) {
                val location = settings.location
                if (location != null) {
                    InfoNote(stringResource(R.string.onboarding_location_set, location.name), icon = R.drawable.ic_location_on)
                } else {
                    GlassTonalButton(onClick = actions.onUseLocation, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_location_use))
                    }
                    InfoNote(locationMessage ?: stringResource(R.string.onboarding_location_later))
                }
            }

            Step(R.drawable.ic_notifications, stringResource(R.string.onboarding_reminders_title), stringResource(R.string.onboarding_reminders_body)) {
                SwitchRow(
                    title = stringResource(R.string.onboarding_reminders_prayer),
                    checked = prayerOn,
                    onCheckedChange = actions.onPrayerReminders,
                )
                SwitchRow(
                    title = stringResource(R.string.onboarding_reminders_adhkar),
                    checked = adhkarOn,
                    onCheckedChange = actions.onAdhkarReminders,
                )
                if ((prayerOn || adhkarOn) && !notificationsAllowed) {
                    InfoNote(stringResource(R.string.onboarding_notifications_denied))
                }
                if (prayerOn && !exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    InfoNote(stringResource(R.string.onboarding_exact_body))
                    TextButton(onClick = actions.onAllowExact) { Text(stringResource(R.string.onboarding_exact_allow)) }
                }
            }
            InfoNote(stringResource(R.string.onboarding_later))
        }
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimens.screenGutter, vertical = AppTheme.spacing.md),
        ) {
            GlassButton(onClick = actions.onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_start))
            }
        }
    }
}

@Composable
private fun Step(@DrawableRes icon: Int, title: String, body: String, content: @Composable () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppTheme.dimens.icon))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
