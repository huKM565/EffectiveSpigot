package ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.chunk

import net.minecraft.world.level.chunk.LevelChunkSection
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager
import ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.McvModuleV1_21_9
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser
import kotlin.math.abs

object McvEffectiveChunkV1_21_9: IMcvEffectiveChunkManager {
    override fun getBlocks(chunkX: Int, chunkZ: Int, world: String): ShortArray {
        val mcvWorld = (EffectiveWorldParser.stringToWorld(world) as CraftWorld).handle
        val mcvChunk = mcvWorld.getChunk(chunkX, chunkZ)
        val types = ShortArray(16 * 16 * mcvWorld.height)
        var i = 0
        for (sectionY in 0 until mcvWorld.maxSectionY + abs(mcvWorld.minSectionY)) {
            val section: LevelChunkSection = mcvChunk.getSection(sectionY)

            if (section.hasOnlyAir()) {
                i += 16 * 16 * 16
                continue
            }

            for(y in 0 until 16) {
                for(x in 0 until 16) {
                    for(z in 0 until 16) {
                        val blockState = section.getBlockState(x, y, z)
                        val type = blockState.block

                        types[i] = McvModuleV1_21_9.mcvBlockToMaterialIndex(type)
                        i++
                    }
                }
            }
        }

        return types
    }
}