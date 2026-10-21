package id.haho.muslimdaily.domain.myset

import id.haho.muslimdaily.core.model.ContentStatus
import id.haho.muslimdaily.core.model.Dua
import org.junit.Test
import kotlin.test.assertEquals

class ActiveSetTest {
    private fun dua(id: String, order: Int, group: String? = null) = Dua(
        id, "adhkar", "morning", order, group, "[PLACEHOLDER]", false, "", "", "", null,
        emptyList(), null, emptyList(), null, null, emptyList(), ContentStatus.DRAFT,
    )

    private val duas = listOf(dua("a", 1), dua("v1", 2, "g"), dua("v2", 3, "g"), dua("b", 4), dua("c", 5))
    private val canonical = ActiveSet.canonical(duas)
    private fun ids(list: List<Dua>) = list.map { it.id }

    @Test
    fun `variant group becomes one item at the position of its first form`() {
        assertEquals(listOf("a", "vg:g", "b", "c"), canonical.map { it.key })
    }

    @Test
    fun `full set is canonical order with first form by default`() {
        assertEquals(listOf("a", "v1", "b", "c"), ids(ActiveSet.active(canonical, Customization())))
    }

    @Test
    fun `chosen form applies in full set too`() {
        val custom = Customization(choices = mapOf("g" to FormChoice.Fixed("v2")))
        assertEquals(listOf("a", "v2", "b", "c"), ids(ActiveSet.active(canonical, custom)))
    }

    @Test
    fun `rotation cycles through forms`() {
        listOf(0 to "v1", 1 to "v2", 2 to "v1", 5 to "v2").forEach { (index, expected) ->
            val custom = Customization(choices = mapOf("g" to FormChoice.Rotate(index)))
            assertEquals(expected, ActiveSet.active(canonical, custom)[1].id)
        }
    }

    @Test
    fun `my set uses custom order and hides disabled items`() {
        val custom = Customization(
            useMySet = true,
            entries = listOf(CustomEntry("c", true, 0), CustomEntry("a", false, 1), CustomEntry("vg:g", true, 2), CustomEntry("b", true, 3)),
        )
        assertEquals(listOf("c", "v1", "b"), ids(ActiveSet.active(canonical, custom)))
    }

    @Test
    fun `full set ignores customisation order even when entries exist`() {
        val custom = Customization(useMySet = false, entries = listOf(CustomEntry("c", true, 0), CustomEntry("a", false, 1)))
        assertEquals(listOf("a", "v1", "b", "c"), ids(ActiveSet.active(canonical, custom)))
    }

    @Test
    fun `items added to content later are appended enabled`() {
        val custom = Customization(useMySet = true, entries = listOf(CustomEntry("b", true, 0), CustomEntry("a", true, 1)))
        assertEquals(listOf("b", "a", "vg:g", "c"), ActiveSet.items(canonical, custom).map { it.item.key })
        assertEquals(listOf("b", "a", "v1", "c"), ids(ActiveSet.active(canonical, custom)))
    }

    @Test
    fun `removing an item starts from the full set`() {
        val entries = ActiveSet.withIncluded(canonical, Customization(), "b", included = false)
        val custom = Customization(useMySet = true, entries = entries)
        assertEquals(listOf("a", "v1", "c"), ids(ActiveSet.active(canonical, custom)))
    }
}
