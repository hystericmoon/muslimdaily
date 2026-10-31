package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.settings.AppSettings
import id.haho.muslimdaily.data.settings.PhraseFill
import id.haho.muslimdaily.data.settings.PhraseLayout
import id.haho.muslimdaily.feature.settings.PhraseLayoutGroup
import androidx.compose.foundation.layout.Column
import id.haho.muslimdaily.data.settings.TranslationMode
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.duas.detail.DuaDetailActions
import id.haho.muslimdaily.feature.duas.detail.DuaDetailContent
import id.haho.muslimdaily.feature.duas.detail.DuaDetailState
import id.haho.muslimdaily.feature.duas.event.EventContent
import id.haho.muslimdaily.feature.duas.event.EventUiState
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.duas.search.SearchContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1600dp-xhdpi")
class DuasScreenshotTest {
    private val event = DuaEvent("morning", "adhkar", "Dzikir pagi", 1)

    @Test
    fun library() = captureVariants("20-duas-library") { DuaLibraryContent(DuaPreviewData.categories) {} }

    @Test
    fun event() = captureVariants("21-duas-event") {
        val canonical = ActiveSet.canonical(DuaPreviewData.duas)
        EventContent(
            EventUiState(event, ActiveSet.items(canonical, Customization()), ReadingPrefs()),
            {}, { _, _ -> },
        )
    }

    /** The first dua saved to a routine, the second in My hifz. */
    @Test
    fun eventSaved() = captureVariants("133-duas-event-saved") {
        val canonical = ActiveSet.canonical(DuaPreviewData.duas)
        EventContent(
            EventUiState(
                event, ActiveSet.items(canonical, Customization()), ReadingPrefs(),
                hifz = setOf("morning_02"),
                routines = listOf(id.haho.muslimdaily.data.routine.Routine("1", "Doa saya", listOf("morning_01"))),
            ),
            {}, { _, _ -> },
        )
    }

    @Test
    fun detailPlaying() = captureVariants("22-dua-detail-playing") {
        DuaDetailContent(
            DuaDetailState(
                dua = DuaPreviewData.dua("morning_02", 2, count = 3),
                event = event,
                prefs = ReadingPrefs(TranslationMode.BOTH, transliteration = true),
                playback = PlaybackState(duaId = "morning_02", isPlaying = true, activePhrase = 1),
            ),
            DuaDetailActions(),
        )
    }

    /** Layout B: flowing text with a pause mark between phrases. */
    @Test
    fun detailInline() = captureVariants("138-dua-layout-inline") {
        DuaDetailContent(
            DuaDetailState(
                dua = DuaPreviewData.dua("morning_02", 2, count = 3),
                event = event,
                prefs = ReadingPrefs(layout = PhraseLayout.INLINE),
                playback = PlaybackState(duaId = "morning_02", isPlaying = true, activePhrase = 1),
            ),
            DuaDetailActions(),
        )
    }

    /** Layout C: one phrase at a time, following the recitation. */
    @Test
    fun detailFocus() = captureVariants("139-dua-layout-focus") {
        DuaDetailContent(
            DuaDetailState(
                dua = DuaPreviewData.dua("morning_02", 2, count = 3),
                event = event,
                prefs = ReadingPrefs(layout = PhraseLayout.FOCUS),
                playback = PlaybackState(duaId = "morning_02", isPlaying = true, activePhrase = 1),
            ),
            DuaDetailActions(),
        )
    }

    /** Layout B with alternating phrase tones, audio stopped. */
    @Test
    fun detailInlineAlternating() = captureVariants("140-dua-fill-alternating") {
        DuaDetailContent(
            DuaDetailState(
                dua = DuaPreviewData.dua("morning_02", 2, count = 3),
                event = event,
                prefs = ReadingPrefs(layout = PhraseLayout.INLINE, fill = PhraseFill.ALTERNATING),
                playback = PlaybackState(duaId = "morning_02", activePhrase = 1),
            ),
            DuaDetailActions(),
        )
    }

    /** Layout A with a bubble per phrase. */
    @Test
    fun detailLinesBubbles() = captureVariants("141-dua-fill-bubbles") {
        DuaDetailContent(
            DuaDetailState(
                dua = DuaPreviewData.dua("morning_02", 2, count = 3),
                event = event,
                prefs = ReadingPrefs(fill = PhraseFill.BUBBLES),
                playback = PlaybackState(duaId = "morning_02", activePhrase = 1),
            ),
            DuaDetailActions(),
        )
    }

    /** The reading sheet opened from the dua page. */
    @Test
    fun readingSheet() = captureVariants("142-dua-reading-sheet") {
        Column { PhraseLayoutGroup(AppSettings(phraseFill = PhraseFill.ALTERNATING), {}) }
    }

    /** A dua shown as the words to recite, with the book's full entry opened below it. */
    @Test
    fun detailFullEntry() = captureVariants("126-dua-full-entry") {
        val dua = DuaPreviewData.dua("morning_02", 2)
        DuaDetailContent(
            DuaDetailState(
                // Placeholder text: "an example of the explanation before the text:" + the sample.
                dua = dua.copy(arabicFull = "مِثَالُ شَرْحٍ قَبْلَ النَّصِّ: ((" + dua.arabic + "))", translationIdFull = "[Contoh keterangan:] " + dua.translationId),
                event = event,
                prefs = ReadingPrefs(),
                playback = PlaybackState(),
            ),
            DuaDetailActions(),
            fullEntryOpen = true,
        )
    }

    @Test
    fun search() = captureVariants("23-duas-search") {
        SearchContent("tes", DuaPreviewData.duas.take(2), ReadingPrefs(), {}, {}, autoFocus = false)
    }
}
