package id.haho.muslimdaily.data.content

import id.haho.muslimdaily.core.model.ContentStatus
import id.haho.muslimdaily.core.model.Segment
import id.haho.muslimdaily.core.model.SourceRef
import id.haho.muslimdaily.core.model.splitPhrases
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentMapperTest {

    private val taxonomy = TaxonomyFile(
        listOf(CategoryJson("daily", "Harian", order = 1, events = listOf(EventJson("waking_up", "Bangun tidur", order = 1)))),
    )

    // Neutral test strings only; no religious text is generated in tests.
    private fun dua(id: String, status: ContentStatus, block: DuaJson.() -> DuaJson = { this }) = DuaJson(
        id = id, category = "daily", event = "waking_up", arabic = "aaa bbb", phraseBreaks = listOf(4),
        translations = mapOf("id" to "uji"),
        sources = listOf(SourceRef("Buku", "1")), grading = GradingJson("sahih", "Penguji"),
        audio = AudioJson("$id.opus", listOf(listOf(0.0, 1.0), listOf(1.0, 2.0))), status = status,
    ).block()

    private fun map(duas: List<DuaJson>, drafts: Boolean) =
        ContentMapper.map(DuasFile(3, duas), RawatibFile(), taxonomy, includeDrafts = drafts)

    @Test
    fun `release import keeps only published entries`() {
        val bundle = map(listOf(dua("a", ContentStatus.PUBLISHED), dua("b", ContentStatus.DRAFT)), drafts = false)
        assertEquals(listOf("a"), bundle.duas.map { it.id })
        assertEquals("not published", bundle.rejected["b"])
    }

    @Test
    fun `debug import keeps drafts`() {
        val bundle = map(listOf(dua("a", ContentStatus.PUBLISHED), dua("b", ContentStatus.DRAFT)), drafts = true)
        assertEquals(setOf("a", "b"), bundle.duas.map { it.id }.toSet())
    }

    @Test
    fun `published entry without grader is rejected`() {
        val bundle = map(listOf(dua("a", ContentStatus.PUBLISHED) { copy(grading = GradingJson("sahih", "")) }), drafts = true)
        assertTrue(bundle.duas.isEmpty())
        assertEquals("missing grading", bundle.rejected["a"])
    }

    @Test
    fun `grade outside sahih or hasan is rejected`() {
        val bundle = map(listOf(dua("a", ContentStatus.PUBLISHED) { copy(grading = GradingJson("dhaif", "X")) }), drafts = true)
        assertEquals("grade out of scope", bundle.rejected["a"])
    }

    @Test
    fun `published entry without source is rejected`() {
        val bundle = map(listOf(dua("a", ContentStatus.PUBLISHED) { copy(sources = emptyList()) }), drafts = true)
        assertEquals("missing source", bundle.rejected["a"])
    }

    @Test
    fun `segments that do not match phrases are dropped, entry kept`() {
        val bundle = map(
            listOf(dua("a", ContentStatus.DRAFT) { copy(audio = AudioJson("a.opus", listOf(listOf(0.0, 2.0)))) }),
            drafts = true,
        )
        assertEquals("[]", bundle.duas.single().segmentsJson)
    }

    @Test
    fun `unknown event is rejected`() {
        val bundle = map(listOf(dua("a", ContentStatus.DRAFT) { copy(event = "nope") }), drafts = true)
        assertEquals("unknown event", bundle.rejected["a"])
    }

    @Test
    fun `fts row holds normalised text`() {
        val bundle = map(listOf(dua("a", ContentStatus.DRAFT) { copy(translations = mapOf("id" to "Dôa PAGI")) }), drafts = true)
        assertEquals("doa pagi", bundle.fts.single().translationId)
        assertEquals("bangun tidur", bundle.fts.single().eventLabel)
    }

    @Test
    fun `split phrases trims and attaches matching segments`() {
        val phrases = splitPhrases("aaa bbb ccc", listOf(4, 8), listOf(Segment(0.0, 1.0), Segment(1.0, 2.0), Segment(2.0, 3.0)))
        assertEquals(listOf("aaa", "bbb", "ccc"), phrases.map { it.text })
        assertEquals(4..6, phrases[1].range)
        assertEquals(Segment(2.0, 3.0), phrases[2].segment)
    }

    @Test
    fun `split phrases drops segments on count mismatch`() {
        val phrases = splitPhrases("aaa bbb", listOf(4), listOf(Segment(0.0, 1.0)))
        assertEquals(2, phrases.size)
        assertNull(phrases[0].segment)
    }
}
