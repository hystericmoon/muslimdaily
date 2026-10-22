package id.haho.muslimdaily.core.arabic

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArabicNormalizerTest {

    @Test
    fun `strips tashkeel`() {
        // ba + fatha, sin + sukun, mim + kasra
        assertEquals("بسم", ArabicNormalizer.normalize("بَسْمِ"))
    }

    @Test
    fun `strips shadda tanween and superscript alif`() {
        assertEquals("له", ArabicNormalizer.normalize("لًّٰه"))
    }

    @Test
    fun `removes tatweel`() {
        assertEquals("بسم", ArabicNormalizer.normalize("بــسم"))
    }

    @Test
    fun `unifies alif forms`() {
        listOf('أ', 'إ', 'آ', 'ٱ').forEach { alif ->
            assertEquals("اب", ArabicNormalizer.normalize(alif + "\u0628"))
        }
    }

    @Test
    fun `maps alif maqsura to ya and ta marbuta to ha`() {
        assertEquals("علي", ArabicNormalizer.normalize("على"))
        assertEquals("رحمه", ArabicNormalizer.normalize("رحمة"))
    }

    @Test
    fun `query and index normalise to the same form`() {
        val indexed = ArabicNormalizer.normalize("أَلْحَمْدُ")
        val typed = ArabicNormalizer.normalize("الحمد")
        assertEquals(typed, indexed)
    }

    @Test
    fun `latin is lower-cased and diacritics removed`() {
        assertEquals("doa pagi", ArabicNormalizer.normalize("  Dôa   PAGI "))
    }

    @Test
    fun `fts query uses prefix terms and drops syntax characters`() {
        assertEquals("doa* pagi*", ArabicNormalizer.toFtsQuery("Doa \"pagi\""))
        assertEquals("a* b*", ArabicNormalizer.toFtsQuery("a* -b"))
        assertNull(ArabicNormalizer.toFtsQuery("  \"*  "))
    }
}
