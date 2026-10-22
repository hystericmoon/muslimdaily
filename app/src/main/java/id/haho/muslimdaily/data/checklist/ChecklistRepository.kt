package id.haho.muslimdaily.data.checklist

import id.haho.muslimdaily.data.db.ChecklistEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Private daily checklist. Nothing is shared, there are no streaks, and unless the user turns on
 * history, earlier days are deleted so the list simply resets each day.
 */
@Singleton
class ChecklistRepository @Inject constructor(
    private val db: UserDatabase,
    private val settings: SettingsRepository,
) {
    private val dao get() = db.checklistDao()

    fun checked(day: LocalDate): Flow<Set<String>> = dao.observeDay(day.toEpochDay()).map { it.toSet() }

    suspend fun toggle(day: LocalDate, itemKey: String, checked: Boolean) {
        if (checked) dao.insert(ChecklistEntity(day.toEpochDay(), itemKey)) else dao.delete(day.toEpochDay(), itemKey)
    }

    suspend fun pruneBefore(today: LocalDate) {
        if (!settings.current().keepChecklistHistory) dao.deleteBefore(today.toEpochDay())
    }

    companion object {
        fun fardKey(prayer: String) = "fard:$prayer"
        fun rawatibKey(id: String) = "rawatib:$id"
        const val DUHA = "duha"
        const val WITR = "witr"
    }
}
