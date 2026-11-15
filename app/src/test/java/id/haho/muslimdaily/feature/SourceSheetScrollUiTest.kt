package id.haho.muslimdaily.feature

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.core.model.Grading
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.SourceSheetContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Audit B2: at 200% text the sources sheet must scroll so the grading policy can be reached on a phone. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class SourceSheetScrollUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun grading_policy_is_reachable_at_200_percent_on_a_phone_height() {
        // Placeholder text only: this checks layout, not content.
        val dua = DuaPreviewData.dua("x", 1).copy(
            sources = List(4) { SourceRef("Kitab contoh $it", "${it + 1}") },
            reference = "[Rujukan contoh " + "panjang ".repeat(20) + "]",
            grading = Grading("sahih", "Penilai contoh"),
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AdwamTheme { Box(Modifier.height(500.dp)) { SourceSheetContent(dua) } }
            }
        }
        val policy = compose.onNodeWithText("Doa diambil dari", substring = true)
        policy.performScrollTo().assertIsDisplayed()
    }
}
