package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.nms.CraftReflection
import ru.hukm.effectiveSpigot.minecraft.nms.NmsChunkManager
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData

/**
 * Per-world block access for zones and custom blocks, read straight from the server's chunk sections
 * ([NmsChunkManager]) with no copy of the world, so [getBlock] always sees the live world and memory doesn't grow with
 * loaded chunks.
 *
 * Material searches remember their result per chunk and material ([ChunkMaterialCache]): a repeated
 * [findBlocksByMaterials] reads the remembered positions and only re-reads chunks that changed. Many block events fire
 * before the world changes (physics, fading, forming, growing), so a touched chunk is dropped from the cache at once and
 * again on the next tick, when the change behind the event is done. A change made with no event at all (a plugin
 * calling `setType`, WorldEdit) is not seen by a cached search until that chunk is touched again or reloads. Only loaded
 * chunks are searched and read; nothing here loads a chunk.
 *
 * [findBlocksByMaterialsAsync] scans the chunks without a cached result off the main thread, reading the sections
 * lock-free (read only; a section's palette and packed data are taken together from its volatile `data`). Back on the
 * main thread a chunk's result is stored only if the chunk wasn't touched or reloaded meanwhile
 * ([ChunkMaterialCache.isCurrent]); the rest, and any chunk whose scan failed on a race, are read again on the main
 * thread, so the result equals a synchronous search at the moment the future completes. The scan starts at once on
 * a daemon thread (`EffectiveSpigot-BlockSearch`), so the future completes on the next tick.
 * Internal — not part of the public API.
 */
internal class EffectiveWorld private constructor(val name: String) {
    data class BlockData(val x: Int, val y: Int, val z: Int, val material: Material)

    private val cache = ChunkMaterialCache()
    private var loadedSeeded = false
    private val pendingChunks = HashSet<Long>()
    private var flushScheduled = false

    companion object {
        object EffectiveWorldModule: IModule {
            override fun init() {
                EffectiveWorldEvents.init()
            }
        }

        val effectiveWorlds = arrayListOf<EffectiveWorld>()

        private val searchExecutor: ExecutorService by lazy {
            Executors.newSingleThreadExecutor { Thread(it, "EffectiveSpigot-BlockSearch").apply { isDaemon = true } }
        }

        fun getOrCreateInstance(world: World): EffectiveWorld {
            return getOrCreateInstance(EffectiveWorldParser.worldToString(world))
        }

        fun getOrCreateInstance(world: String): EffectiveWorld {
            return effectiveWorlds.find { effectiveWorld ->
                effectiveWorld.name == world
            } ?: EffectiveWorld(world).also {
                effectiveWorlds.add(it)
            }
        }

        fun findBlocksByMaterial(material: Material, world: World): ArrayList<EffectiveBlockData> {
            return getOrCreateInstance(world).findBlocksByMaterial(material)
        }

        fun findBlocksByMaterials(materials: List<Material>, world: World): ArrayList<EffectiveBlockData> {
            return getOrCreateInstance(world).findBlocksByMaterials(materials)
        }

        fun findBlocksByMaterialAsync(material: Material, world: World): CompletableFuture<ArrayList<EffectiveBlockData>> {
            return getOrCreateInstance(world).findBlocksByMaterialsAsync(listOf(material))
        }

        fun tryUploadChunk(chunk: Chunk) {
            getOrCreateInstance(chunk.world).tryUploadChunk(chunk)
        }

        fun setIsUnload(chunk: Chunk) {
            getOrCreateInstance(chunk.world).setIsUnload(chunk)
        }

        fun updateBlocks(blocks: List<Block>) {
            blocks.forEach { getOrCreateInstance(it.world).updateBlock(it) }
        }

        fun updateBlock(block: Block) {
            getOrCreateInstance(block.world).updateBlock(block)
        }

        fun setAirs(blocks: List<Block>) {
            blocks.forEach { getOrCreateInstance(it.world).setAir(it) }
        }

        fun setAir(block: Block) {
            getOrCreateInstance(block.world).setAir(block)
        }

        fun updateBlock(world: World, x: Int, y: Int, z: Int, material: Material) {
            getOrCreateInstance(world).updateBlock(x, y, z, material)
        }

        fun getBlock(world: World, pos: EffectiveBlockPos): EffectiveBlockData? {
            return getOrCreateInstance(world).getBlock(pos)
        }

        fun markDirty(block: Block) {
            getOrCreateInstance(block.world).markDirty(block)
        }

        fun markDirty(blocks: Collection<Block>) {
            blocks.forEach { markDirty(it) }
        }
    }

    fun findBlocksByMaterial(material: Material): ArrayList<EffectiveBlockData> {
        return findBlocksByMaterials(listOf(material))
    }

    fun findBlocksByMaterials(materials: List<Material>): ArrayList<EffectiveBlockData> {
        val world = Bukkit.getWorld(name) ?: return arrayListOf()
        seed(world)
        val foundBlocks = arrayListOf<EffectiveBlockData>()
        var level: Any? = null
        val wanted = if (materials.size == 1) setOf(materials[0]) else materials.toSet()
        val single = materials.singleOrNull()
        val read = HashMap<Material, MutableList<EffectiveBlockData>>()
        var targets: NmsChunkManager.Targets? = null

        for (entry in cache.loaded) {
            val cached = if (single != null) entry.found.containsKey(single) else entry.found.keys.containsAll(wanted)
            if (!cached) {
                read.clear()
                val handle = entry.handle ?: if (world.isChunkLoaded(entry.chunkX, entry.chunkZ)) {
                    if (level == null) level = CraftReflection.getWorldHandle(world)
                    NmsChunkManager.chunk(level, entry.chunkX, entry.chunkZ).also { entry.handle = it }
                } else null
                if (handle != null) {
                    if (targets == null) targets = NmsChunkManager.Targets(wanted)
                    NmsChunkManager.findInChunk(handle, world.minHeight, entry.chunkX, entry.chunkZ, targets, read)
                }
                wanted.forEach { entry.found[it] = read[it] ?: emptyList() }
            }
            if (single != null) {
                val list = entry.found[single]!!
                if (list.isNotEmpty()) foundBlocks.addAll(list)
            } else {
                for (material in materials) foundBlocks.addAll(entry.found[material]!!)
            }
        }

        return foundBlocks
    }

    private fun seed(world: World) {
        if (loadedSeeded) return
        world.loadedChunks.forEach { cache.load(it.x, it.z) }
        loadedSeeded = true
    }

    private class Scan(val entry: ChunkMaterialCache.Entry, val version: Long, val handle: Any) {
        var result: Map<Material, List<EffectiveBlockData>>? = null
    }

    fun findBlocksByMaterialsAsync(materials: List<Material>): CompletableFuture<ArrayList<EffectiveBlockData>> {
        val future = CompletableFuture<ArrayList<EffectiveBlockData>>()
        if (Bukkit.isPrimaryThread()) startAsync(materials, future)
        else Bukkit.getScheduler().runTask(instance, Runnable { startAsync(materials, future) })
        return future
    }

    private fun startAsync(materials: List<Material>, future: CompletableFuture<ArrayList<EffectiveBlockData>>) {
        try {
            val world = Bukkit.getWorld(name) ?: return run { future.complete(arrayListOf()) }
            seed(world)
            val wanted = materials.toSet()
            val level = CraftReflection.getWorldHandle(world)
            val scans = arrayListOf<Scan>()
            for (entry in cache.loaded) {
                if (entry.found.keys.containsAll(wanted)) continue
                val handle = entry.handle
                    ?: if (world.isChunkLoaded(entry.chunkX, entry.chunkZ)) NmsChunkManager.chunk(level, entry.chunkX, entry.chunkZ).also { entry.handle = it }
                    else continue
                scans += Scan(entry, entry.version, handle)
            }
            if (scans.isEmpty()) {
                future.complete(findBlocksByMaterials(materials))
                return
            }
            val targets = NmsChunkManager.Targets(wanted)
            val minHeight = world.minHeight
            searchExecutor.execute {
                for (scan in scans) {
                    val read = HashMap<Material, MutableList<EffectiveBlockData>>()
                    scan.result = try {
                        NmsChunkManager.findInChunk(scan.handle, minHeight, scan.entry.chunkX, scan.entry.chunkZ, targets, read)
                        read
                    } catch (_: Throwable) {
                        null
                    }
                }
                try {
                    Bukkit.getScheduler().runTask(instance, Runnable {
                        try {
                            for (scan in scans) {
                                val result = scan.result ?: continue
                                if (cache.isCurrent(scan.entry, scan.version)) wanted.forEach { scan.entry.found[it] = result[it] ?: emptyList() }
                            }
                            future.complete(findBlocksByMaterials(materials))
                        } catch (e: Throwable) {
                            future.completeExceptionally(e)
                        }
                    })
                } catch (e: Throwable) {
                    future.completeExceptionally(e)
                }
            }
        } catch (e: Throwable) {
            future.completeExceptionally(e)
        }
    }

    fun getBlock(pos: EffectiveBlockPos): EffectiveBlockData? {
        val world = Bukkit.getWorld(name) ?: return null
        val material = NmsChunkManager.materialAt(world, pos.x, pos.y, pos.z) ?: return null
        return EffectiveBlockData(pos.x, pos.y, pos.z, material)
    }

    fun tryUploadChunk(chunk: Chunk) {
        cache.load(chunk.x, chunk.z)
    }

    fun setIsUnload(chunk: Chunk) {
        cache.unload(chunk.x, chunk.z)
    }

    private fun key(chunkX: Int, chunkZ: Int) = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)

    fun updateBlocks(blocks: List<Block>) {
        blocks.forEach { updateBlock(it) }
    }

    fun updateBlock(block: Block) {
        touch(block.x, block.z)
    }

    fun setAirs(blocks: List<Block>) {
        blocks.forEach { setAir(it) }
    }

    fun setAir(block: Block) {
        touch(block.x, block.z)
    }

    fun updateBlock(x: Int, y: Int, z: Int, material: Material) {
        touch(x, z)
    }

    @JvmName("updateBlocksData")
    fun updateBlocks(blocks: List<EffectiveBlockData>) {
        blocks.forEach { touch(it.x, it.z) }
    }

    fun markDirty(x: Int, y: Int, z: Int) {
        touch(x, z)
    }

    fun markDirty(block: Block) = touch(block.x, block.z)

    private fun touch(x: Int, z: Int) {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        cache.invalidate(chunkX, chunkZ)
        pendingChunks.add(key(chunkX, chunkZ))
        if (flushScheduled) return
        flushScheduled = true
        Bukkit.getScheduler().runTask(instance, Runnable { flush() })
    }

    private fun flush() {
        flushScheduled = false
        pendingChunks.forEach { cache.invalidate((it shr 32).toInt(), it.toInt()) }
        pendingChunks.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EffectiveWorld) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
