package id.haho.muslimdaily.screenshots

import id.haho.muslimdaily.core.designsystem.theme.AppPalette
import id.haho.muslimdaily.feature.duas.components.sourceLabel
import id.haho.muslimdaily.feature.duas.detail.DuaShareCard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.designsystem.glass.GlassCard
import id.haho.muslimdaily.core.model.Dua
import id.haho.muslimdaily.core.model.DuaCategory
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.data.content.ContentJsonFormat
import id.haho.muslimdaily.data.content.ContentMapper
import id.haho.muslimdaily.data.content.DuasFile
import id.haho.muslimdaily.data.content.RawatibFile
import id.haho.muslimdaily.data.content.TaxonomyFile
import id.haho.muslimdaily.data.content.toModel
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.duas.components.SourceDetails
import id.haho.muslimdaily.feature.duas.detail.DuaDetailActions
import id.haho.muslimdaily.feature.duas.detail.DuaDetailContent
import id.haho.muslimdaily.feature.duas.detail.DuaDetailState
import id.haho.muslimdaily.feature.duas.event.EventContent
import id.haho.muslimdaily.feature.duas.event.EventUiState
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.feature.memorize.session.MemorizeActions
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionScreen
import id.haho.muslimdaily.feature.memorize.session.MemorizeUiState
import id.haho.muslimdaily.feature.memorize.session.Playing
import id.haho.muslimdaily.feature.memorize.session.Step
import java.io.File

/** The shipped content files in content/, so layout is checked against real lengths. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "id-w400dp-h1800dp-xhdpi")
class RealContentScreenshotTest {
    private val content = File("../content")
    private val taxonomy = ContentJsonFormat.decodeFromString(TaxonomyFile.serializer(), File(content, "taxonomy.json").readText())
    private val bundle = ContentMapper.map(
        ContentJsonFormat.decodeFromString(DuasFile.serializer(), File(content, "duas.json").readText()),
        RawatibFile(),
        taxonomy,
    )
    private val duas: List<Dua> = bundle.duas.map { it.toModel() }
    private fun dua(id: String) = duas.first { it.id == id }
    private fun event(id: String) = bundle.events.first { it.id == id }.let { DuaEvent(it.id, it.categoryId, it.label, it.sortOrder) }

    private val categories = taxonomy.categories.sortedBy { it.order }.map { c ->
        DuaCategory(
            c.id, c.label, c.order,
            c.events.sortedBy { it.order }.map { e -> DuaEvent(e.id, c.id, e.label, e.order, duas.count { it.event == e.id }) }
                .filter { it.duaCount > 0 }, // as ContentRepository.categories()
        )
    }.filter { it.events.isNotEmpty() }

    private fun detail(name: String, id: String) = captureVariants(name) {
        val d = dua(id)
        DuaDetailContent(
            DuaDetailState(d, event(d.event), ReadingPrefs(), PlaybackState(), inMySet = false),
            DuaDetailActions(),
        )
    }

    private fun sources(name: String, id: String) = captureVariants(name) {
        GlassCard(Modifier.padding(16.dp), shape = MaterialTheme.shapes.extraLarge) {
            SourceDetails(dua(id), Modifier.padding(24.dp))
        }
    }

    @Test
    fun library() = captureVariants("60-real-library") { DuaLibraryContent(categories) {} }

    @Test
    fun morningEvent() = captureVariants("61-real-morning") {
        val canonical = ActiveSet.canonical(duas.filter { it.event == "morning" })
        EventContent(
            EventUiState(event("morning"), false, false, ActiveSet.items(canonical, Customization()), 0, ReadingPrefs()),
            {}, {}, { _, _ -> }, {},
        )
    }

    @Test
    fun ayatKursi() = detail("62-real-detail-ayat-kursi", "morning_01")

    @Test
    fun longestDua() = detail("63-real-detail-longest", duas.maxBy { it.arabic.length }.id)

    @Test
    fun sourcesWithGrade() = sources("64-real-sources-graded", duas.first { it.grading != null }.id)

    @Test
    fun sourcesFallback() = sources("65-real-sources-fallback", duas.first { it.sources.first().book == "Hisnul Muslim" }.id)

    @Test
    fun quranEvent() = captureVariants("66-real-quran-ease") {
        val canonical = ActiveSet.canonical(duas.filter { it.event == "quran_ease" })
        EventContent(
            EventUiState(event("quran_ease"), false, false, ActiveSet.items(canonical, Customization()), 0, ReadingPrefs()),
            {}, {}, { _, _ -> }, {},
        )
    }

    @Test
    fun quranDetail() = detail("67-real-detail-quran", "quran_16")

    @Test
    fun quranSources() = sources("68-real-sources-quran", "quran_16")

    /** The share image, rendered from real content, written next to the screenshots. */
    @Test
    fun shareCards() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        for ((name, duaId, palette) in listOf(
            Triple("69-share-ayat-kursi", "morning_01", AppPalette.TEAL),
            Triple("69-share-quran-rose", "quran_16", AppPalette.ROSE),
            Triple("69-share-graded-navy", duas.first { it.grading != null }.id, AppPalette.NAVY),
        )) {
            val d = dua(duaId)
            val source = d.sources.joinToString("; ") { sourceLabel(it) } +
                (d.grading?.let { " · ${it.grade.replaceFirstChar(Char::titlecase)} (${it.grader})" } ?: "")
            val bmp = DuaShareCard.render(context, d, event(d.event).label, source, palette)
            java.io.File("../docs/screenshots/$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** The same Hisn dua and Qur'anic dua in each Arabic typeface the reader can choose. */
    @Test
    fun arabicFonts() {
        id.haho.muslimdaily.core.designsystem.theme.ArabicFont.entries.forEach { font ->
            com.github.takahirom.roborazzi.captureRoboImage("67-real-font-${font.name.lowercase()}.png") {
                id.haho.muslimdaily.core.designsystem.theme.MuslimDailyTheme(themeMode = id.haho.muslimdaily.core.designsystem.theme.ThemeMode.LIGHT, arabicFont = font) {
                    id.haho.muslimdaily.core.designsystem.glass.GlassBackdrop {
                        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                            id.haho.muslimdaily.core.designsystem.component.ArabicText(dua("istiftah_05").arabic, style = id.haho.muslimdaily.core.designsystem.theme.AppTheme.arabic.arabicBody)
                            id.haho.muslimdaily.core.designsystem.component.ArabicText(dua("quran_01").arabic, style = id.haho.muslimdaily.core.designsystem.theme.AppTheme.arabic.quranBody)
                        }
                    }
                }
            }
        }
    }

    /** First-letter cues on real Arabic, so the letter shapes (and the tatweel) can be checked. */
    @Test
    fun memorizeFirstLetters() = captureVariants("122-memorize-first-letters") {
        val d = dua("morning_04")
        val words = Recall.words(d)
        MemorizeSessionScreen(
            MemorizeUiState(
                dua = d, eventLabel = event("morning").label, step = Step.RECALL, playing = Playing.NONE,
                playback = PlaybackState(), settings = MemorizeSettings(), fade = FadeLevel.FULL, words = words,
                hiddenWords = Recall.hiddenWords(words, FadeLevel.FULL, d.id), review = null, rated = null,
                fromReview = false, nextDueId = null, ab = 0..0, firstLetters = true,
            ),
            MemorizeActions(),
            talkBack = false,
        )
    }
}
