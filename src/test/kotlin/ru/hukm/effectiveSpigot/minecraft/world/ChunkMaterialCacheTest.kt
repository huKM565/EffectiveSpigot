package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData

class ChunkMaterialCacheTest {
    private val note = listOf(EffectiveBlockData(1, 2, 3, Material.NOTE_BLOCK))

    @Test fun loadedChunkStartsEmpty() {
        val cache = ChunkMaterialCache()
        cache.load(0, 0)
        assertNull(cache.get(0, 0, Material.NOTE_BLOCK))
        assertEquals(listOf(0 to 0), cache.loaded.map { it.chunkX to it.chunkZ })
    }

    @Test fun storesPerChunkAndMaterial() {
        val cache = ChunkMaterialCache()
        cache.load(4, -2)
        cache.put(4, -2, Material.NOTE_BLOCK, note)
        cache.put(4, -2, Material.STONE, emptyList())
        assertEquals(note, cache.get(4, -2, Material.NOTE_BLOCK))
        assertEquals(emptyList<EffectiveBlockData>(), cache.get(4, -2, Material.STONE))
        assertNull(cache.get(-2, 4, Material.NOTE_BLOCK))
    }

    @Test fun putIgnoresUnloadedChunk() {
        val cache = ChunkMaterialCache()
        cache.put(9, 9, Material.NOTE_BLOCK, note)
        assertNull(cache.get(9, 9, Material.NOTE_BLOCK))
    }

    @Test fun invalidateKeepsChunkLoaded() {
        val cache = ChunkMaterialCache()
        cache.load(0, 0)
        cache.load(0, 1)
        cache.put(0, 0, Material.NOTE_BLOCK, note)
        cache.put(0, 1, Material.NOTE_BLOCK, note)
        cache.invalidate(0, 0)
        assertNull(cache.get(0, 0, Material.NOTE_BLOCK))
        assertEquals(note, cache.get(0, 1, Material.NOTE_BLOCK))
        assertEquals(2, cache.loaded.size)
    }

    @Test fun unloadForgetsChunk() {
        val cache = ChunkMaterialCache()
        cache.load(7, 7)
        cache.put(7, 7, Material.NOTE_BLOCK, note)
        cache.unload(7, 7)
        assertNull(cache.get(7, 7, Material.NOTE_BLOCK))
        assertEquals(0, cache.loaded.size)
    }

    @Test fun reloadStartsEmpty() {
        val cache = ChunkMaterialCache()
        cache.load(1, 1)
        cache.put(1, 1, Material.NOTE_BLOCK, note)
        cache.load(1, 1)
        assertNull(cache.get(1, 1, Material.NOTE_BLOCK))
    }

    @Test fun untouchedEntryIsCurrent() {
        val cache = ChunkMaterialCache()
        cache.load(2, 3)
        val entry = cache.loaded.single()
        assertEquals(true, cache.isCurrent(entry, entry.version))
    }

    @Test fun invalidateMakesSnapshotStale() {
        val cache = ChunkMaterialCache()
        cache.load(2, 3)
        val entry = cache.loaded.single()
        val version = entry.version
        cache.invalidate(2, 3)
        assertEquals(false, cache.isCurrent(entry, version))
    }

    @Test fun reloadMakesSnapshotStale() {
        val cache = ChunkMaterialCache()
        cache.load(2, 3)
        val entry = cache.loaded.single()
        val version = entry.version
        cache.load(2, 3)
        assertEquals(false, cache.isCurrent(entry, version))
    }

    @Test fun unloadMakesSnapshotStale() {
        val cache = ChunkMaterialCache()
        cache.load(2, 3)
        val entry = cache.loaded.single()
        val version = entry.version
        cache.unload(2, 3)
        cache.load(2, 3)
        assertEquals(false, cache.isCurrent(entry, version))
    }
}
