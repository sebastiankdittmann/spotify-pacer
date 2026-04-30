package dk.dittmann.spotifypacer.bpm

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BpmCacheTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun newFile(): File = File(tmp.newFolder(), "bpm-cache.json")

    @Test
    fun get_on_empty_returns_miss() = runTest {
        val cache = BpmCache(newFile())
        assertEquals(BpmCache.Lookup.Miss, cache.get("anything"))
    }

    @Test
    fun put_then_get_returns_hit_and_persists() = runTest {
        val file = newFile()
        BpmCache(file).put("t1", 165.5)
        assertTrue(file.exists())

        val reloaded = BpmCache(file)
        assertEquals(BpmCache.Lookup.Hit(165.5), reloaded.get("t1"))
    }

    @Test
    fun negative_entry_within_ttl_returns_negative_hit() = runTest {
        val file = newFile()
        var clock = 1_000_000L
        val cache = BpmCache(file, now = { clock })
        cache.put("missing", null)

        clock += BpmCache.NEGATIVE_TTL_MS - 1
        assertEquals(BpmCache.Lookup.NegativeHit, cache.get("missing"))
    }

    @Test
    fun negative_entry_after_ttl_returns_miss() = runTest {
        val file = newFile()
        var clock = 1_000_000L
        val cache = BpmCache(file, now = { clock })
        cache.put("missing", null)

        clock += BpmCache.NEGATIVE_TTL_MS + 1
        assertEquals(BpmCache.Lookup.Miss, cache.get("missing"))
    }

    @Test
    fun version_mismatch_invalidates_loaded_entries() = runTest {
        val file = newFile()
        file.writeText("""{"version":99,"entries":{"t1":{"bpm":160.0,"ts":1}}}""")

        val cache = BpmCache(file)
        assertEquals(BpmCache.Lookup.Miss, cache.get("t1"))
    }

    @Test
    fun blank_file_treated_as_empty() = runTest {
        val file = newFile()
        file.writeText("")
        val cache = BpmCache(file)
        assertEquals(BpmCache.Lookup.Miss, cache.get("t1"))
    }
}
