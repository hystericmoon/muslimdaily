package id.haho.muslimdaily.data.review

import androidx.room.withTransaction
import id.haho.muslimdaily.data.db.ReviewItemEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class ReviewItem(val duaId: String, val state: LeitnerState, val lastReviewed: LocalDate?) {
    val memorized: Boolean get() = Leitner.isMemorized(state)

    /** Saved to learn later (e.g. from Run mode) and never rated: not yet in the review queue. */
    val notStarted: Boolean get() = lastReviewed == null
}

/** Duas with the same Arabic, each id mapped to its group; the first id is the one memorised. */
fun interface DuaTwins {
    suspend fun groups(): Map<String, List<String>>
}

/**
 * Leitner review boxes for memorised duas. Private, local, and free of streaks.
 *
 * Duas with the same text (morning and evening adhkar 1–19, ruku and sujud…) are memorised once:
 * every call maps an id to the first of its twins, and [link] folds older separate rows into it.
 */
@Singleton
class ReviewRepository @Inject constructor(private val db: UserDatabase, private val twins: DuaTwins) {
    private val dao get() = db.reviewDao()

    private suspend fun key(duaId: String): String = twins.groups()[duaId]?.first() ?: duaId

    /** The id and its twins, for screens that hide or mark a dua being memorised. */
    suspend fun withTwins(ids: Set<String>): Set<String> {
        val groups = twins.groups()
        return ids.flatMap { groups[it] ?: listOf(it) }.toSet()
    }

    fun items(): Flow<List<ReviewItem>> = flow {
        link()
        emitAll(dao.observeAll().map { list -> list.map { it.toModel() } })
    }

    fun item(duaId: String): Flow<ReviewItem?> = flow {
        link()
        emitAll(dao.observeItem(key(duaId)).map { it?.toModel() })
    }

    fun dueCount(today: LocalDate): Flow<Int> = flow {
        link()
        emitAll(dao.observeDueCount(today.toEpochDay()))
    }

    suspend fun dueCountOnce(today: LocalDate): Int {
        link()
        return dao.dueCount(today.toEpochDay())
    }

    suspend fun due(today: LocalDate): List<String> {
        link()
        return dao.due(today.toEpochDay()).map { it.duaId }
    }

    suspend fun rate(duaId: String, rating: Rating, today: LocalDate): LeitnerState {
        link()
        val id = key(duaId)
        val existing = dao.item(id)
        val next = Leitner.rate(existing?.toModel()?.state, rating, today)
        dao.upsert(
            ReviewItemEntity(
                duaId = id,
                box = next.box,
                dueEpochDay = next.due.toEpochDay(),
                lastReviewedEpochDay = today.toEpochDay(),
                addedEpochDay = existing?.addedEpochDay ?: today.toEpochDay(),
            ),
        )
        return next
    }

    /**
     * Saves a dua to learn later. It joins the review queue on its first rating; a dua already
     * saved or being reviewed is left as it is.
     */
    suspend fun add(duaId: String, today: LocalDate) {
        link()
        val id = key(duaId)
        if (dao.item(id) != null) return
        dao.upsert(ReviewItemEntity(id, box = 1, dueEpochDay = today.toEpochDay(), lastReviewedEpochDay = null, addedEpochDay = today.toEpochDay()))
    }

    suspend fun remove(duaId: String) {
        twins.groups()[duaId].orEmpty().plus(duaId).forEach { dao.delete(it) }
    }

    /**
     * Folds the rows of twins saved separately (before twins were linked, or from an older backup) into one,
     * under the first id. The state kept is the one reviewed last: it is the latest evidence of
     * recall. Nothing changes once every group has a single row.
     */
    suspend fun link() {
        val groups = twins.groups()
        if (groups.isEmpty()) return
        val rows = dao.all()
        val stray = rows.filter { row -> groups[row.duaId]?.first()?.let { it != row.duaId } == true }
        if (stray.isEmpty()) return
        db.withTransaction {
            rows.filter { it.duaId in groups }.groupBy { groups.getValue(it.duaId).first() }.forEach { (id, group) ->
                if (group.size == 1 && group.single().duaId == id) return@forEach
                val kept = group.maxWith(compareBy<ReviewItemEntity>({ it.lastReviewedEpochDay ?: Long.MIN_VALUE }, { -it.box }))
                group.forEach { dao.delete(it.duaId) }
                dao.upsert(kept.copy(duaId = id, addedEpochDay = group.minOf { it.addedEpochDay }))
            }
        }
    }

    private fun ReviewItemEntity.toModel() = ReviewItem(
        duaId = duaId,
        state = LeitnerState(box, LocalDate.ofEpochDay(dueEpochDay)),
        lastReviewed = lastReviewedEpochDay?.let(LocalDate::ofEpochDay),
    )
}
