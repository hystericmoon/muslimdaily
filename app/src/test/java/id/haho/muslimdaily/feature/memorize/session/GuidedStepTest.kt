package id.haho.muslimdaily.feature.memorize.session

import org.junit.Assert.assertEquals
import org.junit.Test

class GuidedStepTest {
    @Test
    fun `each guided step has its own advanced tab, so switching keeps the place`() {
        assertEquals(Step.entries, GuidedStep.entries.map { it.advanced })
        Step.entries.forEach { assertEquals(it, GuidedStep.of(it).advanced) }
    }
}
