package id.haho.muslimdaily.data.mosque

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/** Reads the real bundled OpenStreetMap data. */
@RunWith(AndroidJUnit4::class)
class MosqueRepositoryTest {
    private val repo = MosqueRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun `finds Istiqlal next to Istiqlal, nearest first`() = runTest {
        val list = repo.nearest(-6.1702, 106.8312, limit = 10, radiusKm = 1.0)
        assertTrue(list.isNotEmpty())
        assertTrue(list.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
        assertTrue(list.any { it.name.contains("Istiqlal", ignoreCase = true) && it.distanceMeters < 400 }, list.toString())
    }

    @Test
    fun `respects the radius and the limit`() = runTest {
        val list = repo.nearest(-6.2146, 106.8451, limit = 5, radiusKm = 2.0)
        assertTrue(list.size <= 5)
        assertTrue(list.all { it.distanceMeters <= 2000 })
    }

    @Test
    fun `open sea has nothing`() = runTest {
        assertTrue(repo.nearest(-5.0, 110.0, radiusKm = 5.0).isEmpty())
    }
}
