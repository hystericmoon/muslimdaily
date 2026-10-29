package id.haho.muslimdaily.screenshots

import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.FadeOrder
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.memorize.HifzEntry
import id.haho.muslimdaily.feature.memorize.MemorizeHomeContent
import id.haho.muslimdaily.feature.memorize.MemorizeHomeState
import id.haho.muslimdaily.feature.memorize.session.MemorizeActions
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionScreen
import id.haho.muslimdaily.feature.memorize.session.MemorizeUiState
import id.haho.muslimdaily.feature.memorize.session.Playing
import id.haho.muslimdaily.feature.memorize.session.Step
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1000dp-xhdpi")
class MemorizeScreenshotTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val dua = DuaPreviewData.dua("morning_02", 2)

    private fun state(
        step: Step,
        fade: FadeLevel = FadeLevel.LIGHT,
        playing: Playing = Playing.NONE,
        playback: PlaybackState = PlaybackState(),
        order: FadeOrder = FadeOrder.RANDOM,
        firstLetters: Boolean = false,
        fromReview: Boolean = false,
    ): MemorizeUiState {
        val words = Recall.words(dua)
        return MemorizeUiState(
            dua = dua, eventLabel = "Dzikir pagi", step = step, playing = playing, playback = playback,
            settings = MemorizeSettings(), fade = fade, words = words,
            hiddenWords = Recall.hiddenWords(words, fade, dua.id, order), review = null, rated = null,
            fromReview = fromReview, nextDueId = null, ab = 0..1, fadeOrder = order, firstLetters = firstLetters,
            position = 2, positionOf = 19,
        )
    }

    @Test
    fun home() = captureVariants("50-memorize-home") {
        MemorizeHomeContent(
            MemorizeHomeState(
                today,
                listOf(
                    HifzEntry(ReviewItem("a", LeitnerState(2, today), today.minusDays(3)), DuaPreviewData.dua("a", 1), "Bangun tidur", due = true),
                    HifzEntry(ReviewItem("b", LeitnerState(5, today.plusDays(21)), today.minusDays(9)), DuaPreviewData.dua("b", 2), "Dzikir pagi", due = false),
                    HifzEntry(ReviewItem("c", LeitnerState(3, today.plusDays(4)), today.minusDays(3)), DuaPreviewData.dua("c", 3), "Sebelum tidur", due = false),
                    HifzEntry(ReviewItem("d", LeitnerState(1, today), null), DuaPreviewData.dua("d", 1), "Masuk masjid", due = false),
                ),
            ),
        ) { _, _ -> }
    }

    @Test
    fun buildPause() = captureVariants("51-memorize-build") {
        MemorizeSessionScreen(
            state(Step.BUILD, playing = Playing.BUILD, playback = PlaybackState(duaId = dua.id, isPlaying = true, sequenceIndex = 9, inPause = true)),
            MemorizeActions(),
            talkBack = false,
        )
    }

    @Test
    fun recall() = captureVariants("52-memorize-recall") {
        MemorizeSessionScreen(state(Step.RECALL, fade = FadeLevel.MEDIUM), MemorizeActions(), talkBack = false)
    }

    @Test
    fun site() = captureSite("130-site-recall") {
        MemorizeSessionScreen(state(Step.RECALL, fade = FadeLevel.MEDIUM), MemorizeActions(), talkBack = false)
    }

    @Test
    fun check() = captureVariants("53-memorize-check") {
        MemorizeSessionScreen(state(Step.CHECK), MemorizeActions(), talkBack = false)
    }

    @Test
    fun reviewRecallFirst() = captureVariants("121-memorize-review-cue") {
        MemorizeSessionScreen(state(Step.RECALL, fade = FadeLevel.FULL, fromReview = true), MemorizeActions(), talkBack = false)
    }
}
