package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.nms.NmsChunkManager
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.world.chunk.EffectiveChunkSoA
import ru.hukm.effectiveSpigot.minecraft.world.chunk.EffectiveChunkSoA.EffectiveChunkCursor
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData

class EffectiveWorld private constructor(val name: String) {
    data class BlockData(val x: Int, val y: Int, val z: Int, val material: Material)

    val effectiveChunkSoA = EffectiveChunkSoA()

    companion object {
        object EffectiveWorldModule: IModule {
            override fun init() {
                instance.server.pluginManager.registerEvents(EffectiveWorldEvents(), instance)
            }
        }

        val effectiveWorlds = arrayListOf<EffectiveWorld>()

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
    }

    fun findBlocksByMaterial(material: Material): ArrayList<EffectiveBlockData> {
        return findBlocksByMaterials(listOf(material))
    }

    fun findBlocksByMaterials(materials: List<Material>): ArrayList<EffectiveBlockData> {
        val materialsIndexes = materials.map { EffectiveWorldParser.materialToMaterialIndex(it) }
        val foundBlocks = arrayListOf<EffectiveBlockData>()

        effectiveChunkSoA.forEachCursors { effectiveChunkCursor ->
            foundBlocks.addAll(effectiveChunkCursor.findBlocksByMaterialIndexes(materialsIndexes, effectiveChunkSoA)!!)
        }

        return foundBlocks
    }

    fun getBlock(pos: EffectiveBlockPos): EffectiveBlockData? {
        return effectiveChunkSoA.getBlock(pos)
    }

    fun tryUploadChunk(chunk: Chunk) {
        if(!chunk.isLoaded) throw IllegalArgumentException(Locale.getMessage("errors.chunk_not_loaded", chunk.x, chunk.z, name))
        if(EffectiveWorldParser.worldToString(chunk.world) != name) throw IllegalArgumentException(Locale.getMessage("errors.chunk_wrong_world", chunk.x, chunk.z, chunk.world.name, name))

        val cursor = effectiveChunkSoA.find(chunk.x, chunk.z)?.apply {
            this.setLoad(true, effectiveChunkSoA)
        }

        if(cursor == null) {
            effectiveChunkSoA.add(
                chunk.x,
                chunk.z,
                NmsChunkManager.getBlocks(chunk.x, chunk.z, name)
            )
        }
    }

    fun setIsUnload(chunk: Chunk) {
        effectiveChunkSoA.find(chunk.x, chunk.z)?.setLoad(false, effectiveChunkSoA)
    }

    fun updateBlocks(blocks: List<Block>) {
        blocks.forEach { updateBlock(it) }
    }

    fun updateBlock(block: Block) {
        updateBlock(block.chunk.x, block.chunk.z, block.x and 15, block.y, block.z and 15, block.type)
    }

    private fun updateBlock(chunkX: Int, chunkZ: Int, x: Int, y: Int, z: Int, material: Material) {
        findChunkOrLoad(chunkX, chunkZ)?.updateBlock(x, y, z, material, effectiveChunkSoA)
    }

    fun setAirs(blocks: List<Block>) {
        blocks.forEach { setAir(it) }
    }

    fun setAir(block: Block) {
        findChunkOrLoad(block)?.setAir(block.x % 16, block.y, block.z and 15, effectiveChunkSoA)
    }

    fun updateBlock(x: Int, y: Int, z: Int, material: Material) {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val blockX = x and 15
        val blockZ = z and 15
        
        updateBlock(chunkX, chunkZ, blockX, y, blockZ, material)
    }

    @JvmName("updateBlocksData")
    fun updateBlocks(blocks: List<EffectiveBlockData>) {
        val blocksByChunk = blocks.groupBy { block ->
            Pair(block.x shr 4, block.z shr 4)
        }

        blocksByChunk.forEach { (chunkCoords, chunkBlocks) ->
            val cursor = findChunkOrLoad(chunkCoords.first, chunkCoords.second)
            cursor?.let { chunkCursor ->
                chunkBlocks.forEach { blockData ->
                    chunkCursor.updateBlock(
                        blockData.x and 15,
                        blockData.y,
                        blockData.z and 15,
                        blockData.material,
                        effectiveChunkSoA
                    )
                }
            }
        }
    }

    private fun findChunkOrLoad(block: Block): EffectiveChunkCursor? {
        return findChunkOrLoad(block.chunk.x, block.chunk.z)
    }

    private fun findChunkOrLoad(chunkX: Int, chunkZ: Int): EffectiveChunkCursor? {
        val cursor = effectiveChunkSoA.find(chunkX, chunkZ)
        if (cursor == null) {
            instance.logger.warning(
                Locale.getMessage("errors.world.update_unloaded_chunk", chunkX, chunkZ, name)
            )
            return null
        }
        return cursor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EffectiveWorld

        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + effectiveChunkSoA.hashCode()
        return result
    }
}