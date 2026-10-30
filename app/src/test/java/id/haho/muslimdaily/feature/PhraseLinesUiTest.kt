package id.haho.muslimdaily.feature

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assert
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.core.designsystem.theme.AdwamTheme
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.PhraseLines
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** One phrase per line; each phrase with audio plays from there, and the recited one says so. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "id-w400dp-h860dp")
class PhraseLinesUiTest {
    @get:Rule val compose = createComposeRule()

    private val dua = DuaPreviewData.dua("a", 1)

    @Test
    fun tapping_a_phrase_plays_from_it() {
        val played = mutableListOf<Int>()
        compose.setContent {
            AdwamTheme { PhraseLines(dua.arabic, dua.phrases, activePhrase = 2, style = AdwamTheme.arabic.arabicBody, onPlayFrom = { played += it }) }
        }
        val phrases = compose.onAllNodes(hasClickAction())
        phrases.assertCountEquals(3)
        phrases[1].performClick()
        assertEquals(listOf(1), played)
        phrases[1].assert(SemanticsMatcher("labelled") { it.config[SemanticsActions.OnClick].label == "Putar dari frasa ini" })
        phrases[2].assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sedang dibaca"))
    }

    @Test
    fun without_audio_phrases_are_text_only() {
        compose.setContent {
            AdwamTheme { PhraseLines(dua.arabic, dua.phrases, activePhrase = -1, style = AdwamTheme.arabic.arabicBody, onPlayFrom = null) }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}
