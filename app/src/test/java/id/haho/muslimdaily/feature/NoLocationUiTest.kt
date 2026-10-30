package id.haho.muslimdaily.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.feature.onboarding.OnboardingActions
import id.haho.muslimdaily.feature.onboarding.OnboardingScreen
import id.haho.muslimdaily.feature.today.TodayActions
import id.haho.muslimdaily.feature.today.TodayScreen
import id.haho.muslimdaily.feature.today.TodayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Without a location, the adhkar stay reachable and a city can be chosen by hand. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h860dp")
class NoLocationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun today_without_location_still_starts_adhkar() {
        val started = mutableListOf<String>()
        compose.setContent {
            AdwamTheme { TodayScreen(TodayUiState.NoLocation, TodayActions(onStartAdhkar = { started += it })) }
        }
        compose.onNodeWithText("Lokasi belum diatur").assertIsDisplayed()
        compose.onNodeWithText("Dzikir pagi").performScrollTo().performClick()
        compose.onNodeWithText("Dzikir petang").performScrollTo().performClick()
        assertEquals(listOf("morning", "evening"), started)
    }

    @Test
    fun onboarding_offers_choosing_a_city() {
        var picked = false
        compose.setContent {
            AdwamTheme {
                OnboardingScreen(
                    AppSettings(),
                    notificationsAllowed = false,
                    exactAllowed = false,
                    locationMessage = null,
                    actions = OnboardingActions(onPickCity = { picked = true }),
                    showLanguage = false,
                )
            }
        }
        compose.onNodeWithText("Pilih kota").performScrollTo().performClick()
        assertTrue(picked)
        compose.onNodeWithText("Teks Arab", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }
}
