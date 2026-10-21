package id.haho.muslimdaily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import id.haho.muslimdaily.navigation.AppRoot
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val reviewDue by viewModel.reviewDue.collectAsStateWithLifecycle()
            MuslimDailyTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                arabicScale = settings.arabicScale,
                translationScale = settings.translationScale,
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    AppRoot(reviewDueCount = reviewDue)
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
