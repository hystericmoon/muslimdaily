package id.haho.muslimdaily

import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.core.designsystem.glass.GlassBackdrop
import id.haho.muslimdaily.core.designsystem.glass.frostWhenOverlayOpen
import android.content.Context
import android.content.Intent
import id.haho.muslimdaily.core.ui.AppLanguage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import id.haho.muslimdaily.feature.settings.diagnostics.sendFeedback
import id.haho.muslimdaily.feature.settings.diagnostics.DiagnosticsViewModel
import id.haho.muslimdaily.core.diagnostics.CrashLog
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import id.haho.muslimdaily.core.designsystem.glass.GlassAlertDialog
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import id.haho.muslimdaily.core.designsystem.theme.MuslimDailyTheme
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.data.review.ReviewRepository
import id.haho.muslimdaily.feature.settings.UpdateActions
import id.haho.muslimdaily.feature.settings.UpdatePromptDialog
import id.haho.muslimdaily.navigation.AppRoot
import id.haho.muslimdaily.update.UpdateViewModel
import id.haho.muslimdaily.notifications.Destination
import id.haho.muslimdaily.notifications.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import id.haho.muslimdaily.feature.onboarding.OnboardingRoute
import id.haho.muslimdaily.feature.handover.MovedScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import id.haho.muslimdaily.feature.handover.LegacyImportScreen
import id.haho.muslimdaily.feature.handover.LegacyImportViewModel
import id.haho.muslimdaily.data.handover.Handover
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import id.haho.muslimdaily.feature.whatsnew.WhatsNew
import id.haho.muslimdaily.feature.whatsnew.WhatsNewDialog
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val updates: UpdateViewModel by viewModels()
    private val diagnostics: DiagnosticsViewModel by viewModels()

    /** Screen requested by a notification tap; consumed by [AppRoot]. */
    private val destination = MutableStateFlow<Destination?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destination.value = intent.destination()
    }

    private fun Intent.destination(): Destination? =
        getStringExtra(Notifier.EXTRA_DESTINATION)?.let { runCatching { Destination.valueOf(it) }.getOrNull() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            destination.value = intent.destination()
            updates.autoCheck()
        }
        setContent {
            val pending by destination.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val reviewDue by viewModel.reviewDue.collectAsStateWithLifecycle()
            val onboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()
            // Once the data has gone to the new Adwam app, this app only points there.
            var movedTo by remember { mutableStateOf(Handover.movedTo(this)) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { movedTo = Handover.movedTo(this@MainActivity) }
            MuslimDailyTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                palette = settings.palette,
                arabicScale = settings.arabicScale,
                translationScale = settings.translationScale,
                glass = settings.glassEffect,
                arabicFont = settings.arabicFont,
            ) {
                GlassBackdrop {
                    movedTo?.let { moved ->
                        MovedScreen(moved)
                        return@GlassBackdrop
                    }
                    // First launch as id.haho.adwam with the previous app installed: bring its data over.
                    val legacy: LegacyImportViewModel = hiltViewModel()
                    val legacyStep by legacy.step.collectAsStateWithLifecycle()
                    if (legacyStep != LegacyImportViewModel.Step.HIDDEN) {
                        LegacyImportScreen(legacyStep, legacy.legacyPackage, onMove = legacy::move, onFinish = legacy::finish)
                        return@GlassBackdrop
                    }
                    when (onboarding) {
                        null -> Unit // settings still loading: a blank backdrop for a moment
                        true -> OnboardingRoute()
                        false -> AppRoot(
                            reviewDueCount = reviewDue,
                            pendingDestination = pending,
                            onDestinationHandled = { destination.value = null },
                            modifier = Modifier.frostWhenOverlayOpen(),
                        )
                    }
                    val whatsNew by viewModel.whatsNew.collectAsStateWithLifecycle()
                    if (onboarding == false && whatsNew.isNotEmpty()) {
                        WhatsNewDialog(whatsNew, onDismiss = viewModel::dismissWhatsNew)
                    }
                    // Once per new crash report: offer to send it, never send it silently.
                    var crashPending by remember { mutableStateOf(CrashLog.unseen(this@MainActivity) != null) }
                    val crashScope = rememberCoroutineScope()
                    if (crashPending && onboarding == false) {
                        GlassAlertDialog(
                            onDismissRequest = { CrashLog.markSeen(this@MainActivity); crashPending = false },
                            title = { Text(stringResource(R.string.crash_prompt_title)) },
                            text = { Text(stringResource(R.string.crash_prompt_body)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    CrashLog.markSeen(this@MainActivity)
                                    crashPending = false
                                    crashScope.launch { sendFeedback(this@MainActivity, diagnostics.load()) }
                                }) { Text(stringResource(R.string.crash_prompt_send)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { CrashLog.markSeen(this@MainActivity); crashPending = false }) {
                                    Text(stringResource(R.string.crash_prompt_later))
                                }
                            },
                        )
                    }
                    val update by updates.state.collectAsStateWithLifecycle()
                    if (update.prompt) {
                        UpdatePromptDialog(
                            state = update,
                            actions = UpdateActions(
                                onDownload = updates::download,
                                onInstall = { updates.installIntent()?.let(::startActivity) },
                            ),
                            onDismiss = updates::dismissPrompt,
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    reviews: ReviewRepository,
    clock: Clock,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /**
     * The first-launch setup is shown to a new install (no location yet), decided once at launch
     * so that picking a location inside the setup doesn't close it; finishing it does.
     */
    private val firstRun = MutableStateFlow<Boolean?>(null)

    /** Release highlights to show once after an update; empty on a fresh install. */
    private val _whatsNew = MutableStateFlow<List<WhatsNew.Release>>(emptyList())
    val whatsNew: StateFlow<List<WhatsNew.Release>> = _whatsNew

    init {
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val fresh = !s.onboardingDone && s.location == null
            firstRun.value = fresh
            if (s.whatsNewSeen != BuildConfig.VERSION_NAME) {
                _whatsNew.value = WhatsNew.since(s.whatsNewSeen, BuildConfig.VERSION_NAME, firstRun = fresh)
                if (fresh) settingsRepository.update { whatsNewSeen = BuildConfig.VERSION_NAME }
            }
        }
    }

    fun dismissWhatsNew() {
        _whatsNew.value = emptyList()
        viewModelScope.launch { settingsRepository.update { whatsNewSeen = BuildConfig.VERSION_NAME } }
    }

    val showOnboarding: StateFlow<Boolean?> = combine(firstRun, settingsRepository.settings) { first, s ->
        first?.let { it && !s.onboardingDone }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Badge on the Memorize tab. Re-evaluated on each launch; no streaks or history. */
    val reviewDue: StateFlow<Int> = reviews.dueCount(LocalDate.now(clock))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
