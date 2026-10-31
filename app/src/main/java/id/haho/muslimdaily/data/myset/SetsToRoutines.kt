package id.haho.muslimdaily.data.myset

import androidx.room.withTransaction
import id.haho.muslimdaily.data.content.ContentRepository
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.data.routine.RoutineRepository
import id.haho.muslimdaily.domain.myset.ActiveSet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesuaikan (an occasion's own order and hidden duas) became routines. Each occasion read as its
 * own set, and different from the full one, becomes a routine named after the occasion; then the
 * occasion's order and toggles are cleared, so it reads the full set again. Form choices stay: the
 * occasion page still picks the form. Runs at start and after a restore or import, since older
 * files carry sets; once no occasion has its own set it does nothing.
 */
@Singleton
class SetsToRoutines @Inject constructor(
    private val db: UserDatabase,
    private val content: ContentRepository,
    private val mySets: MySetRepository,
    private val routines: RoutineRepository,
) {
    private val mutex = Mutex()

    /** Returns the number of routines made. */
    suspend fun run(): Int = mutex.withLock {
        val dao = db.mySetDao()
        val events = (dao.allItems().map { it.eventId } + dao.allPrefs().map { it.eventId }).distinct()
        var made = 0
        events.forEach { eventId ->
            val custom = mySets.customizationOnce(eventId)
            val canonical = ActiveSet.canonical(content.duasForEventOnce(eventId).filter { it.isRecitable })
            val own = ActiveSet.active(canonical, custom).map { it.id }
            val full = ActiveSet.active(canonical, custom.copy(useMySet = false)).map { it.id }
            val name = content.eventOnce(eventId)?.label
            // One transaction: a crash between the two steps must not make the routine twice.
            db.withTransaction {
                if (custom.useMySet && own.isNotEmpty() && own != full && name != null) {
                    routines.save(null, name, own)
                    made++
                }
                dao.clearItems(eventId)
                dao.clearPref(eventId)
            }
        }
        made
    }
}
