package id.haho.muslimdaily.domain.myset

import id.haho.muslimdaily.core.model.Dua

/** One item of an event's set: a single dua, or a variant group shown as one item. */
sealed interface SetItem {
    val key: String
    val forms: List<Dua>

    data class Single(val dua: Dua) : SetItem {
        override val key: String get() = dua.id
        override val forms: List<Dua> get() = listOf(dua)
    }

    data class Variants(val group: String, override val forms: List<Dua>) : SetItem {
        override val key: String get() = groupKey(group)
    }

    companion object {
        fun groupKey(group: String) = "vg:$group"
    }
}

/** How a variant group is said: one fixed form, or rotating through the forms each session. */
sealed interface FormChoice {
    data class Fixed(val duaId: String) : FormChoice
    data class Rotate(val index: Int) : FormChoice
}

data class CustomEntry(val key: String, val enabled: Boolean, val position: Int)

data class Customization(
    val useMySet: Boolean = false,
    val entries: List<CustomEntry> = emptyList(),
    val choices: Map<String, FormChoice> = emptyMap(),
)

data class ResolvedItem(val item: SetItem, val form: Dua, val enabled: Boolean)

/**
 * Pure rules for an event's active set:
 *  - The full set is every item in canonical order. It is the default.
 *  - My set uses the user's order and toggles. Items added to the content later are appended,
 *    enabled, so updates are never silently hidden.
 *  - A variant group is one item. Its form is the user's choice (in either mode), the first form
 *    by default, or the next form each session when rotating.
 */
object ActiveSet {

    fun canonical(duas: List<Dua>): List<SetItem> {
        val ordered = duas.sortedWith(compareBy({ it.order }, { it.id }))
        val result = mutableListOf<SetItem>()
        val seen = mutableSetOf<String>()
        for (dua in ordered) {
            val group = dua.variantGroup
            if (group == null) {
                result += SetItem.Single(dua)
            } else if (seen.add(group)) {
                result += SetItem.Variants(group, ordered.filter { it.variantGroup == group })
            }
        }
        return result
    }

    /** All items in display order with their enabled state; used by the customize screen. */
    fun items(canonical: List<SetItem>, custom: Customization): List<ResolvedItem> {
        val ordered = if (custom.useMySet || custom.entries.isNotEmpty()) {
            val byKey = custom.entries.associateBy { it.key }
            val known = canonical.filter { it.key in byKey }.sortedBy { byKey.getValue(it.key).position }
            val added = canonical.filter { it.key !in byKey }
            known.map { it to byKey.getValue(it.key).enabled } + added.map { it to true }
        } else {
            canonical.map { it to true }
        }
        return ordered.map { (item, enabled) -> ResolvedItem(item, formFor(item, custom.choices), enabled) }
    }

    /** The duas to read, in order: full set or the enabled part of My set. */
    fun active(canonical: List<SetItem>, custom: Customization): List<Dua> =
        if (custom.useMySet) {
            items(canonical, custom).filter { it.enabled }.map { it.form }
        } else {
            canonical.map { formFor(it, custom.choices) }
        }

    /** Entries after including or excluding one item, starting from the current effective set. */
    fun withIncluded(canonical: List<SetItem>, custom: Customization, key: String, included: Boolean): List<CustomEntry> =
        items(canonical, custom).mapIndexed { i, r ->
            CustomEntry(r.item.key, if (r.item.key == key) included else r.enabled, i)
        }

    fun formFor(item: SetItem, choices: Map<String, FormChoice>): Dua = when (item) {
        is SetItem.Single -> item.dua
        is SetItem.Variants -> when (val choice = choices[item.group]) {
            is FormChoice.Fixed -> item.forms.firstOrNull { it.id == choice.duaId } ?: item.forms.first()
            is FormChoice.Rotate -> item.forms[Math.floorMod(choice.index, item.forms.size)]
            null -> item.forms.first()
        }
    }
}
