package id.haho.muslimdaily.feature.memorize

import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MemorizeHomeStateTest {
    private val today = LocalDate.of(2026, 10, 21)

    private fun entry(id: String, box: Int, due: LocalDate, last: LocalDate?, isDue: Boolean) =
        HifzEntry(ReviewItem(id, LeitnerState(box, due), last), DuaPreviewData.dua(id, 1), id, isDue)

    private val memorized = entry("memorized", 5, today.plusDays(21), today.minusDays(9), false)
    private val learningLate = entry("learningLate", 3, today.plusDays(4), today.minusDays(3), false)
    private val learningSoon = entry("learningSoon", 2, today.plusDays(1), today.minusDays(2), false)
    private val notStarted = entry("notStarted", 1, today, null, false)
    private val due = entry("due", 2, today, today.minusDays(3), true)

    @Test
    fun `sorts by what needs the reader first`() {
        val state = MemorizeHomeState(today, listOf(memorized, learningLate, notStarted, due, learningSoon))
        assertEquals(listOf("due", "learningSoon", "learningLate", "notStarted", "memorized"), state.sorted.map { it.item.duaId })
    }

    @Test
    fun `continue opens a due review, else a dua not started`() {
        assertEquals("due", MemorizeHomeState(today, listOf(notStarted, due)).next?.item?.duaId)
        assertEquals("notStarted", MemorizeHomeState(today, listOf(memorized, notStarted)).next?.item?.duaId)
        assertNull(MemorizeHomeState(today, listOf(memorized, learningLate)).next)
    }
}
