package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.DuaEvent
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
            EventUiState(event, false, false, ActiveSet.items(canonical, Customization()), 0, ReadingPrefs()),
            {}, {}, { _, _ -> }, {},
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
                inMySet = true,
            ),
            DuaDetailActions(),
        )
    }

    @Test
    fun search() = captureVariants("23-duas-search") {
        SearchContent("tes", DuaPreviewData.duas.take(2), ReadingPrefs(), {}, {}, autoFocus = false)
    }
}
