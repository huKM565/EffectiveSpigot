package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.Material
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser

object NmsChunkManager {
    private val blockToMaterialIndex = hashMapOf<Any, Short>()

    fun getBlocks(chunkX: Int, chunkZ: Int, world: String): ShortArray {
        val level = CraftReflection.getWorldHandle(EffectiveWorldParser.stringToWorld(world))
        val chunk = NmsProxies.serverLevel.getChunk(level, chunkX, chunkZ)

        val types = ShortArray(16 * 16 * NmsProxies.serverLevel.getHeight(level))
        var i = 0

        for (sectionIndex in 0 until NmsProxies.serverLevel.getSectionsCount(level)) {
            val section = NmsProxies.levelChunk.getSection(chunk, sectionIndex)

            if (NmsProxies.chunkSection.hasOnlyAir(section)) {
                i += 16 * 16 * 16
                continue
            }

            for (y in 0 until 16) {
                for (x in 0 until 16) {
                    for (z in 0 until 16) {
                        val blockState = NmsProxies.chunkSection.getBlockState(section, x, y, z)
                        val block = NmsProxies.blockState.getBlock(blockState)

                        types[i] = EffectiveWorldParser.initAndGetFromCache(
                            block,
                            { Material.entries.indexOf(CraftReflection.getMaterial(block)).toShort() },
                            blockToMaterialIndex
                        )
                        i++
                    }
                }
            }
        }

        return types
    }
}
