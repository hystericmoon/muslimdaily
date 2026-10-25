package id.haho.muslimdaily.core.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class AppLanguageTest {
    private val app = ApplicationProvider.getApplicationContext<Context>()

    /** The context Android hands the first activity before the pinned language reaches it. */
    private fun englishBase(): Context =
        app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(Locale.ENGLISH) })

    @Test
    fun `a first start on an English phone opens in Indonesian`() {
        AppLanguage.onAppStart(app)
        val activityContext = AppLanguage.wrap(englishBase())
        assertEquals("id", activityContext.resources.configuration.locales[0].language)
        assertEquals("Mulai", activityContext.getString(R.string.onboarding_start))
    }

    @Test
    fun `a context already in the chosen language is left alone`() {
        AppLanguage.onAppStart(app)
        val base = app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag("id")) })
        assertSame(base, AppLanguage.wrap(base))
    }
}
