package id.haho.muslimdaily.feature.dhikr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.data.settings.DHIKR_MAX
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class DhikrViewModelTest {
    private val settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `the tally goes up by one, never below zero or past the ceiling`() = runTest {
        settings.update { dhikrCount = 0; dhikrTarget = 33 }
        settings.update { dhikrCount = (dhikrCount + 1).coerceAtMost(DHIKR_MAX) }
        assertEquals(1, settings.current().dhikrCount)
        settings.update { dhikrCount = (dhikrCount - 2).coerceAtLeast(0) }
        assertEquals(0, settings.current().dhikrCount)
        settings.update { dhikrCount = DHIKR_MAX + 50 }
        assertEquals(DHIKR_MAX, settings.current().dhikrCount)
        assertEquals(33, settings.current().dhikrTarget)
    }
}
