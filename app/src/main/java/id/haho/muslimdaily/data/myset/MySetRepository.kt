package id.haho.muslimdaily.data.myset

import id.haho.muslimdaily.data.db.EventItemEntity
import id.haho.muslimdaily.data.db.EventPrefEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.db.VariantChoiceEntity
import id.haho.muslimdaily.domain.myset.CustomEntry
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.domain.myset.FormChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Persists per-event customisation (My set) and variant choices. */
@Singleton
class MySetRepository @Inject constructor(private val db: UserDatabase) {
    private val dao get() = db.mySetDao()

    fun customization(eventId: String): Flow<Customization> = combine(
        dao.observePref(eventId),
        dao.observeItems(eventId),
        dao.observeChoices(eventId),
    ) { pref, items, choices ->
        Customization(
            useMySet = pref?.useMySet ?: false,
            entries = items.map { CustomEntry(it.itemKey, it.enabled, it.position) },
            choices = choices.associate { it.variantGroup to it.toChoice() },
        )
    }

    suspend fun customizationOnce(eventId: String): Customization = customization(eventId).first()

    suspend fun setUseMySet(eventId: String, useMySet: Boolean) = dao.upsertPref(EventPrefEntity(eventId, useMySet))

    /** Saves the full ordered list of items with their toggles and switches the event to My set. */
    suspend fun saveItems(eventId: String, entries: List<CustomEntry>) {
        dao.replaceItems(eventId, entries.mapIndexed { i, e -> EventItemEntity(eventId, e.key, e.enabled, i) })
        dao.upsertPref(EventPrefEntity(eventId, true))
    }

    suspend fun setChoice(eventId: String, group: String, choice: FormChoice) {
        val entity = when (choice) {
            is FormChoice.Fixed -> VariantChoiceEntity(eventId, group, choice.duaId, 0)
            is FormChoice.Rotate -> VariantChoiceEntity(eventId, group, VariantChoiceEntity.ROTATE, choice.index)
        }
        dao.upsertChoice(entity)
    }

    /** Called when a run session completes: rotating groups move to their next form. */
    suspend fun advanceRotation(eventId: String) {
        dao.choices(eventId).filter { it.choice == VariantChoiceEntity.ROTATE }.forEach {
            dao.upsertChoice(it.copy(rotateIndex = it.rotateIndex + 1))
        }
    }

    suspend fun reset(eventId: String) = dao.reset(eventId)

    private fun VariantChoiceEntity.toChoice(): FormChoice =
        if (choice == VariantChoiceEntity.ROTATE) FormChoice.Rotate(rotateIndex) else FormChoice.Fixed(choice)

    // ---- Export / import -------------------------------------------------------------------

    @Serializable
    data class ExportFile(
        val format: String = FORMAT,
        val version: Int = 1,
        val events: List<ExportEvent>,
    )

    @Serializable
    data class ExportEvent(
        val event: String,
        val useMySet: Boolean,
        val items: List<ExportItem>,
        val choices: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class ExportItem(val key: String, val enabled: Boolean)

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun exportJson(): String {
        val prefs = dao.allPrefs().associateBy { it.eventId }
        val items = dao.allItems().groupBy { it.eventId }
        val choices = dao.allChoices().groupBy { it.eventId }
        val events = (prefs.keys + items.keys + choices.keys).sorted().map { id ->
            ExportEvent(
                event = id,
                useMySet = prefs[id]?.useMySet ?: false,
                items = items[id].orEmpty().sortedBy { it.position }.map { ExportItem(it.itemKey, it.enabled) },
                choices = choices[id].orEmpty().associate { it.variantGroup to it.choice },
            )
        }
        return json.encodeToString(ExportFile.serializer(), ExportFile(events = events))
    }

    /** Replaces the customisation of every event in the file. Returns the number of events imported. */
    suspend fun importJson(text: String): Int {
        val file = json.decodeFromString(ExportFile.serializer(), text)
        require(file.format == FORMAT) { "Not a My set export" }
        file.events.forEach { e ->
            dao.reset(e.event)
            dao.replaceItems(e.event, e.items.mapIndexed { i, it -> EventItemEntity(e.event, it.key, it.enabled, i) })
            e.choices.forEach { (group, choice) -> dao.upsertChoice(VariantChoiceEntity(e.event, group, choice, 0)) }
            dao.upsertPref(EventPrefEntity(e.event, e.useMySet))
        }
        return file.events.size
    }

    companion object {
        const val FORMAT = "muslimdaily.mysets"
    }
}
