package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Material
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData
import java.util.EnumMap

/**
 * The loaded chunks of one world together with the remembered results of material searches in each: "in chunk
 * (3, −7) the note blocks are here". A repeated search walks [loaded] and reads the lists instead of the chunks; any
 * change inside a chunk must [invalidate] it, so only that chunk is read again. Results can only be stored for a
 * [load]ed chunk, and [unload] forgets the chunk with everything remembered about it.
 *
 * Every [load] and [invalidate] bumps the entry's [Entry.version], so a scan made off the main thread can tell
 * whether its snapshot is still valid ([isCurrent]) before its result is stored.
 */
internal class ChunkMaterialCache {
    class Entry(val chunkX: Int, val chunkZ: Int) {
        internal val found = EnumMap<Material, List<EffectiveBlockData>>(Material::class.java)
        internal var handle: Any? = null

        @Volatile
        var version = 0L
            internal set
    }

    private val entries = HashMap<Long, Entry>()

    val loaded: Collection<Entry> get() = entries.values

    private fun key(chunkX: Int, chunkZ: Int) = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)

    fun load(chunkX: Int, chunkZ: Int) {
        val entry = entries.getOrPut(key(chunkX, chunkZ)) { Entry(chunkX, chunkZ) }
        entry.found.clear()
        entry.handle = null
        entry.version++
    }

    fun unload(chunkX: Int, chunkZ: Int) {
        entries.remove(key(chunkX, chunkZ))
    }

    fun invalidate(chunkX: Int, chunkZ: Int) {
        val entry = entries[key(chunkX, chunkZ)] ?: return
        entry.found.clear()
        entry.version++
    }

    fun isCurrent(entry: Entry, version: Long) = entries[key(entry.chunkX, entry.chunkZ)] === entry && entry.version == version

    fun get(chunkX: Int, chunkZ: Int, material: Material): List<EffectiveBlockData>? = entries[key(chunkX, chunkZ)]?.found?.get(material)

    fun put(chunkX: Int, chunkZ: Int, material: Material, blocks: List<EffectiveBlockData>) {
        entries[key(chunkX, chunkZ)]?.found?.put(material, blocks)
    }
}
