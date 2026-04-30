package dk.dittmann.spotifypacer.bpm

import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persistent BPM cache keyed by Spotify track id. Positives are kept forever; negatives expire so
 * that a track newly added to GetSongBPM (or fixed by a query-normalisation change) gets retried.
 *
 * The on-disk format is versioned. Bumping [CACHE_VERSION] invalidates all entries written by older
 * versions — useful when matching logic changes meaningfully.
 *
 * Atomic writes: serialize to a sibling `.tmp` then rename, so a crash mid-write can't leave a
 * truncated cache file behind.
 */
class BpmCache(private val file: File, private val now: () -> Long = System::currentTimeMillis) {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var loaded: MutableMap<String, Entry> = mutableMapOf()
    private var ready = false

    suspend fun get(spotifyId: String): Lookup =
        mutex.withLock {
            ensureLoaded()
            val entry = loaded[spotifyId] ?: return Lookup.Miss
            if (entry.bpm != null) return Lookup.Hit(entry.bpm)
            // Negative entry: respect TTL.
            return if (now() - entry.timestamp <= NEGATIVE_TTL_MS) Lookup.NegativeHit
            else Lookup.Miss
        }

    suspend fun put(spotifyId: String, bpm: Double?) =
        mutex.withLock {
            ensureLoaded()
            loaded[spotifyId] = Entry(bpm = bpm, timestamp = now())
            persist()
        }

    private fun ensureLoaded() {
        if (ready) return
        ready = true
        if (!file.exists()) return
        runCatching {
            val text = file.readText()
            if (text.isBlank()) return
            val snapshot = json.decodeFromString<Snapshot>(text)
            if (snapshot.version == CACHE_VERSION) {
                loaded = snapshot.entries.toMutableMap()
            }
        }
    }

    private fun persist() {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        val text = json.encodeToString(Snapshot.serializer(), Snapshot(CACHE_VERSION, loaded))
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            // renameTo can fail across some filesystems; fall back to copy + delete.
            file.writeText(text)
            tmp.delete()
        }
    }

    sealed interface Lookup {
        data object Miss : Lookup

        data object NegativeHit : Lookup

        data class Hit(val bpm: Double) : Lookup
    }

    @Serializable
    private data class Snapshot(
        @SerialName("version") val version: Int,
        @SerialName("entries") val entries: Map<String, Entry>,
    )

    @Serializable
    private data class Entry(
        @SerialName("bpm") val bpm: Double? = null,
        @SerialName("ts") val timestamp: Long,
    )

    companion object {
        const val CACHE_VERSION = 1
        const val NEGATIVE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}
