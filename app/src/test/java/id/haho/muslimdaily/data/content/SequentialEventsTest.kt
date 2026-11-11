package id.haho.muslimdaily.data.content

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class SequentialEventsTest {
    /** "Start reading" is offered only where the duas are read one after another as a set. */
    @Test
    fun onlyMorningAndEveningAdhkarAreSequential() {
        val taxonomy = ContentJsonFormat.decodeFromString(TaxonomyFile.serializer(), File("../content/taxonomy.json").readText())
        val flagged = taxonomy.categories.flatMap { it.events }.filter { it.sequential }.map { it.id }
        assertEquals(listOf("morning", "evening"), flagged)
    }
}
