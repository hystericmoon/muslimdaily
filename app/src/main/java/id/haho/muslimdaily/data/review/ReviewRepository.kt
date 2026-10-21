package id.haho.muslimdaily.data.review

import id.haho.muslimdaily.data.db.ReviewItemEntity
import id.haho.muslimdaily.data.db.UserDatabase
import id.haho.muslimdaily.domain.memorize.Leitner
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class ReviewItem(val duaId: String, val state: LeitnerState, val lastReviewed: LocalDate?) {
    val memorized: Boolean get() = Leitner.isMemorized(state)
}

/** Leitner review boxes for memorised duas. Private, local, and free of streaks. */
@Singleton
class ReviewRepository @Inject constructor(private val db: UserDatabase) {
    private val dao get() = db.reviewDao()

    fun items(): Flow<List<ReviewItem>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun item(duaId: String): Flow<ReviewItem?> = dao.observeItem(duaId).map { it?.toModel() }

    fun dueCount(today: LocalDate): Flow<Int> = dao.observeDueCount(today.toEpochDay())

    suspend fun dueCountOnce(today: LocalDate): Int = dao.dueCount(today.toEpochDay())

    suspend fun due(today: LocalDate): List<String> = dao.due(today.toEpochDay()).map { it.duaId }

    suspend fun rate(duaId: String, rating: Rating, today: LocalDate): LeitnerState {
        val existing = dao.item(duaId)
        val next = Leitner.rate(existing?.toModel()?.state, rating, today)
        dao.upsert(
            ReviewItemEntity(
                duaId = duaId,
                box = next.box,
                dueEpochDay = next.due.toEpochDay(),
                lastReviewedEpochDay = today.toEpochDay(),
                addedEpochDay = existing?.addedEpochDay ?: today.toEpochDay(),
            ),
        )
        return next
    }

    suspend fun remove(duaId: String) = dao.delete(duaId)

    private fun ReviewItemEntity.toModel() = ReviewItem(
        duaId = duaId,
        state = LeitnerState(box, LocalDate.ofEpochDay(dueEpochDay)),
        lastReviewed = lastReviewedEpochDay?.let(LocalDate::ofEpochDay),
    )
}
