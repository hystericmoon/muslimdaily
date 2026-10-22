package id.haho.muslimdaily

import id.haho.muslimdaily.core.designsystem.glass.GlassBackdrop
import id.haho.muslimdaily.core.designsystem.glass.frostWhenOverlayOpen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val updates: UpdateViewModel by viewModels()

    /** Screen requested by a notification tap; consumed by [AppRoot]. */
    private val destination = MutableStateFlow<Destination?>(null)

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
            MuslimDailyTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                arabicScale = settings.arabicScale,
                translationScale = settings.translationScale,
                glass = settings.glassEffect,
            ) {
                GlassBackdrop {
                    AppRoot(
                        reviewDueCount = reviewDue,
                        pendingDestination = pending,
                        onDestinationHandled = { destination.value = null },
                        modifier = Modifier.frostWhenOverlayOpen(),
                    )
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
    settingsRepository: SettingsRepository,
    reviews: ReviewRepository,
    clock: Clock,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Badge on the Memorize tab. Re-evaluated on each launch; no streaks or history. */
    val reviewDue: StateFlow<Int> = reviews.dueCount(LocalDate.now(clock))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
