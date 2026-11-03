package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.block.Block
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.nms.CraftReflection
import ru.hukm.effectiveSpigot.minecraft.nms.NmsChunkManager
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData

/**
 * A persistent world owned by a plugin, with its own folder: a lobby, a game world, anything that should live apart
 * from the main world.
 *
 * A subclass declares identity ([getNamespacedData]) and, optionally, how the world generates ([configure]). One
 * instance is one world; several worlds of one kind are several instances of one class:
 * ```kotlin
 * object Lobby : EffectiveWorld() {
 *     override fun getNamespacedData() = MyPlugin.instance to "lobby"
 *     override fun configure(creator: WorldCreator) {
 *         creator.generator(VoidGenerator)
 *     }
 * }
 *
 * class GameWorld(private val id: String) : EffectiveWorld() {
 *     override fun getNamespacedData() = MyPlugin.instance to "game_$id"
 * }
 * ```
 * Like other `Effective*` classes, an instance has to be created (an `object` touched) in `onEnable` to be declared.
 * The world is created or loaded on the first access to [world]; one nobody touched is created by the framework when
 * the server finishes loading, before zones are restored, and one declared later is created on the next tick.
 * Accessing [world] before the server loaded its main world (a `load: STARTUP` plugin in `onEnable`) throws.
 *
 * Where the folder lives depends on the server version. From 26.1 Paper keeps every world inside the main save, at
 * `world/dimensions/<plugin>/<name>/`. Before 26.1 it is a folder in the server root named `<plugin>_<name>/`; when
 * that server is upgraded to 26.x, Paper imports the folder into `world/dimensions/<plugin>/<name>/` by itself, so
 * the world is kept.
 *
 * Every other loaded world (the main world, the nether, the end, worlds of other plugins) has an `EffectiveWorld`
 * made by the framework; [of] returns the one of any world. The framework uses it for its own block search.
 * Worlds are never unloaded or deleted by the framework; the server saves them as usual.
 */
abstract class EffectiveWorld internal constructor(internal val isDeclared: Boolean) {
    constructor() : this(true)

    private val cache = ChunkMaterialCache()
    private var loadedSeeded = false
    private val pendingChunks = HashSet<Long>()
    private var flushScheduled = false

    init {
        if (isDeclared) {
            declaredWorlds += this
            if (serverLoaded) Bukkit.getScheduler().runTask(instance, Runnable { createLogged(this) })
        }
    }

    /** Owning plugin and the world's name; the name must fit a [NamespacedKey] (`[a-z0-9/._-]`). */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /**
     * Sets how the world generates (generator, environment, seed…) through the plain Paper [creator]; by default
     * the world is an ordinary vanilla one. Paper doesn't store the generator, so this runs on every start and has to
     * give the same settings each time.
     */
    open fun configure(creator: WorldCreator) {}

    /** The loaded world, created or loaded on first access. */
    val world: World by lazy { createWorld() }

    /** `<plugin>:<name>`, the same as `world.key` on every supported version. */
    fun getKey(): NamespacedKey = fixedKey() ?: getNamespacedData().let { (plugin, name) -> NamespacedKey(plugin, name) }

    internal open fun fixedKey(): NamespacedKey? = null

    internal open fun createWorld(): World {
        val key = getKey()
        check(Bukkit.getWorlds().isNotEmpty()) {
            "EffectiveWorld $key: the main world is not loaded yet; declare it from a POSTWORLD plugin or let the framework create it on server load"
        }
        val existing = worldsByKey[key]
        require(existing == null || existing === this || !existing.isDeclared) { "EffectiveWorld $key is declared twice" }
        worldsByKey[key] = this
        val creator = EffectiveWorldCreator.creatorFor(key, EffectiveMinecraftUtils.isVersionAtLeast(26, 1))
        configure(creator)
        return creator.createWorld() ?: throw IllegalStateException("Failed to create world $key")
    }

    private fun bukkitWorld(): World? = Bukkit.getWorld(getKey())

    companion object {
        internal object EffectiveWorldModule : IModule {
            override fun init() {
                EffectiveWorldEvents.init()

                event<ServerLoadEvent>(EventPriority.LOWEST) {
                    serverLoaded = true
                    declaredWorlds.toList().forEach { createLogged(it) }
                }
            }
        }

        private val declaredWorlds = arrayListOf<EffectiveWorld>()
        private val worldsByKey = hashMapOf<NamespacedKey, EffectiveWorld>()
        private var serverLoaded = false

        private val searchExecutor: ExecutorService by lazy {
            Executors.newSingleThreadExecutor { Thread(it, "EffectiveSpigot-BlockSearch").apply { isDaemon = true } }
        }

        private fun createLogged(effectiveWorld: EffectiveWorld) {
            try {
                effectiveWorld.world
            } catch (e: Exception) {
                instance.logger.log(Level.SEVERE, "Failed to create EffectiveWorld ${effectiveWorld.javaClass.name}", e)
            }
        }

        /** The `EffectiveWorld` of a loaded [world]: the one a plugin declared, or the framework's own. */
        fun of(world: World): EffectiveWorld =
            worldsByKey[world.key] ?: ExistingEffectiveWorld(world).also { worldsByKey[world.key] = it }

        internal fun findBlocksByMaterial(material: Material, world: World): ArrayList<EffectiveBlockData> {
            return of(world).findBlocksByMaterial(material)
        }

        internal fun findBlocksByMaterials(materials: List<Material>, world: World): ArrayList<EffectiveBlockData> {
            return of(world).findBlocksByMaterials(materials)
        }

        internal fun findBlocksByMaterialAsync(material: Material, world: World): CompletableFuture<ArrayList<EffectiveBlockData>> {
            return of(world).findBlocksByMaterialsAsync(listOf(material))
        }

        internal fun tryUploadChunk(chunk: Chunk) {
            of(chunk.world).tryUploadChunk(chunk)
        }

        internal fun setIsUnload(chunk: Chunk) {
            of(chunk.world).setIsUnload(chunk)
        }

        internal fun updateBlocks(blocks: List<Block>) {
            blocks.forEach { of(it.world).updateBlock(it) }
        }

        internal fun updateBlock(block: Block) {
            of(block.world).updateBlock(block)
        }

        internal fun setAirs(blocks: List<Block>) {
            blocks.forEach { of(it.world).setAir(it) }
        }

        internal fun setAir(block: Block) {
            of(block.world).setAir(block)
        }

        internal fun updateBlock(world: World, x: Int, y: Int, z: Int, material: Material) {
            of(world).updateBlock(x, y, z, material)
        }

        internal fun getBlock(world: World, pos: EffectiveBlockPos): EffectiveBlockData? {
            return of(world).getBlock(pos)
        }

        internal fun markDirty(block: Block) {
            of(block.world).markDirty(block)
        }

        internal fun markDirty(blocks: Collection<Block>) {
            blocks.forEach { markDirty(it) }
        }
    }

    internal fun findBlocksByMaterial(material: Material): ArrayList<EffectiveBlockData> {
        return findBlocksByMaterials(listOf(material))
    }

    /**
     * Searches loaded chunks straight from the server's chunk sections ([NmsChunkManager]) with no copy of the world.
     *
     * Results are remembered per chunk and material ([ChunkMaterialCache]): a repeated search reads the remembered
     * positions and only re-reads chunks that changed. Many block events fire before the world changes (physics,
     * fading, forming, growing), so a touched chunk is dropped from the cache at once and again on the next tick. A
     * change made with no event at all (a plugin calling `setType`, WorldEdit) is not seen by a cached search until
     * that chunk is touched again or reloads. Nothing here loads a chunk, and nothing here touches [world], which may
     * still be being created while its chunk events arrive.
     */
    internal fun findBlocksByMaterials(materials: List<Material>): ArrayList<EffectiveBlockData> {
        val world = bukkitWorld() ?: return arrayListOf()
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

    /**
     * [findBlocksByMaterials] with the chunks that have no cached result scanned off the main thread, lock-free and
     * read only (a section's palette and packed data are taken together from its volatile `data`). Back on the main
     * thread a chunk's result is stored only if the chunk wasn't touched or reloaded meanwhile
     * ([ChunkMaterialCache.isCurrent]); the rest, and any chunk whose scan failed on a race, are read again on the
     * main thread, so the result equals a synchronous search at the moment the future completes. The scan starts at
     * once on a daemon thread (`EffectiveSpigot-BlockSearch`), so the future completes on the next tick.
     */
    internal fun findBlocksByMaterialsAsync(materials: List<Material>): CompletableFuture<ArrayList<EffectiveBlockData>> {
        val future = CompletableFuture<ArrayList<EffectiveBlockData>>()
        if (Bukkit.isPrimaryThread()) startAsync(materials, future)
        else Bukkit.getScheduler().runTask(instance, Runnable { startAsync(materials, future) })
        return future
    }

    private fun startAsync(materials: List<Material>, future: CompletableFuture<ArrayList<EffectiveBlockData>>) {
        try {
            val world = bukkitWorld() ?: return run { future.complete(arrayListOf()) }
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

    internal fun getBlock(pos: EffectiveBlockPos): EffectiveBlockData? {
        val world = bukkitWorld() ?: return null
        val material = NmsChunkManager.materialAt(world, pos.x, pos.y, pos.z) ?: return null
        return EffectiveBlockData(pos.x, pos.y, pos.z, material)
    }

    internal fun tryUploadChunk(chunk: Chunk) {
        cache.load(chunk.x, chunk.z)
    }

    internal fun setIsUnload(chunk: Chunk) {
        cache.unload(chunk.x, chunk.z)
    }

    private fun key(chunkX: Int, chunkZ: Int) = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)

    internal fun updateBlocks(blocks: List<Block>) {
        blocks.forEach { updateBlock(it) }
    }

    internal fun updateBlock(block: Block) {
        touch(block.x, block.z)
    }

    internal fun setAirs(blocks: List<Block>) {
        blocks.forEach { setAir(it) }
    }

    internal fun setAir(block: Block) {
        touch(block.x, block.z)
    }

    internal fun updateBlock(x: Int, y: Int, z: Int, material: Material) {
        touch(x, z)
    }

    @JvmName("updateBlocksData")
    internal fun updateBlocks(blocks: List<EffectiveBlockData>) {
        blocks.forEach { touch(it.x, it.z) }
    }

    internal fun markDirty(x: Int, y: Int, z: Int) {
        touch(x, z)
    }

    internal fun markDirty(block: Block) = touch(block.x, block.z)

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
}

private class ExistingEffectiveWorld(private val loaded: World) : EffectiveWorld(false) {
    override fun getNamespacedData(): Pair<JavaPlugin, String> = EffectiveSpigot.instance to loaded.key.key
    override fun fixedKey(): NamespacedKey = loaded.key
    override fun createWorld(): World = loaded
}
