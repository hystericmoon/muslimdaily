package id.haho.muslimdaily.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.feature.duas.components.DuaToggleHint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Audit D7: the one-time legend names both icon toggles and can be dismissed. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h800dp-xhdpi")
class DuaToggleHintUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun names_both_icons_and_dismisses() {
        var dismissed = 0
        compose.setContent { AdwamTheme { DuaToggleHint(onDismiss = { dismissed++ }) } }
        compose.onNodeWithText("Tambah ke hafalan").assertIsDisplayed()
        compose.onNodeWithText("Simpan ke rangkaian").assertIsDisplayed()
        compose.onNodeWithText("Mengerti").performClick()
        assertEquals(1, dismissed)
    }
}
