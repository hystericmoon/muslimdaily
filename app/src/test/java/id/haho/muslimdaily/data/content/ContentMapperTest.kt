package id.haho.muslimdaily.data.content

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
    private fun dua(id: String, block: DuaJson.() -> DuaJson = { this }) = DuaJson(
        id = id, category = "daily", event = "waking_up", arabic = "aaa bbb", phraseBreaks = listOf(4),
        translations = mapOf("id" to "uji"),
        sources = listOf(SourceRef("Buku", "1")), reference = "Catatan uji.", grading = GradingJson("sahih", "Penguji"),
        audio = AudioJson("$id.opus", listOf(listOf(0.0, 1.0), listOf(1.0, 2.0))),
    ).block()

    private fun map(duas: List<DuaJson>) = ContentMapper.map(DuasFile(3, duas), RawatibFile(), taxonomy)

    @Test
    fun `every valid entry is imported with its reference`() {
        val bundle = map(listOf(dua("a"), dua("b")))
        assertEquals(setOf("a", "b"), bundle.duas.map { it.id }.toSet())
        assertEquals("Catatan uji.", bundle.duas.first().reference)
    }

    @Test
    fun `the full entry and the dua's audio range are kept`() {
        val bundle = map(listOf(dua("a") { copy(arabicFull = "ccc aaa bbb", translationsFull = mapOf("id" to "uji penuh"), audio = audio!!.copy(range = listOf(0.0, 2.5))) }))
        val row = bundle.duas.single()
        assertEquals("ccc aaa bbb", row.arabicFull)
        assertEquals("uji penuh", row.translationIdFull)
        assertEquals(0.0 to 2.5, row.audioStart to row.audioEnd)
    }

    @Test
    fun `a malformed audio range plays the whole file`() {
        val row = map(listOf(dua("a") { copy(audio = audio!!.copy(range = listOf(3.0, 1.0))) })).duas.single()
        assertNull(row.audioStart)
    }

    @Test
    fun `grading and audio are optional`() {
        val bundle = map(listOf(dua("a") { copy(grading = null, audio = null) }))
        assertEquals(listOf("a"), bundle.duas.map { it.id })
        assertNull(bundle.duas.single().gradingGrade)
    }

    @Test
    fun `placeholder text is rejected`() {
        val bundle = map(listOf(dua("a") { copy(arabic = "[PLACEHOLDER]", phraseBreaks = emptyList()) }))
        assertEquals("empty Arabic", bundle.rejected["a"])
    }

    @Test
    fun `grade without grader is rejected`() {
        val bundle = map(listOf(dua("a") { copy(grading = GradingJson("sahih", "")) }))
        assertTrue(bundle.duas.isEmpty())
        assertEquals("grade without grader", bundle.rejected["a"])
    }

    @Test
    fun `grade outside sahih or hasan is rejected`() {
        val bundle = map(listOf(dua("a") { copy(grading = GradingJson("dhaif", "X")) }))
        assertEquals("grade out of scope", bundle.rejected["a"])
    }

    @Test
    fun `entry without source is rejected`() {
        val bundle = map(listOf(dua("a") { copy(sources = emptyList()) }))
        assertEquals("missing source", bundle.rejected["a"])
    }

    @Test
    fun `segments that do not match phrases are dropped, entry kept`() {
        val bundle = map(
            listOf(dua("a") { copy(audio = AudioJson("a.opus", listOf(listOf(0.0, 2.0)))) }),
        )
        assertEquals("[]", bundle.duas.single().segmentsJson)
    }

    @Test
    fun `unknown event is rejected`() {
        val bundle = map(listOf(dua("a") { copy(event = "nope") }))
        assertEquals("unknown event", bundle.rejected["a"])
    }

    @Test
    fun `fts row holds normalised text`() {
        val bundle = map(listOf(dua("a") { copy(translations = mapOf("id" to "Dôa PAGI")) }))
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
