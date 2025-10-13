package ru.hukm.effectiveSpigot.minecraft.nms.v1_21_6.chunk

import net.minecraft.world.level.chunk.LevelChunkSection
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser
import ru.hukm.effectiveSpigot.minecraft.nms.interfaces.chunk.INmsEffectiveChunk
import kotlin.math.abs

class NmsEffectiveChunkV1_21_6: INmsEffectiveChunk {
    override fun getBlocks(chunkX: Int, chunkZ: Int, world: String): ShortArray {
        val nmsWorld = (EffectiveWorldParser.stringToWorld(world) as CraftWorld).handle
        val nmsChunk = nmsWorld.getChunk(chunkX, chunkZ)
        val types = ShortArray(16 * 16 * nmsWorld.height)
        var i = 0
        for (sectionY in 0 until nmsWorld.maxSectionY + abs(nmsWorld.minSectionY)) {
            val section: LevelChunkSection = nmsChunk.getSection(sectionY)

            if (section.hasOnlyAir()) {
                i += 16 * 16 * 16
                continue
            }

            for(y in 0 until 16) {
                for(x in 0 until 16) {
                    for(z in 0 until 16) {
                        val blockState = section.getBlockState(x, y, z)
                        val type = blockState.block

                        types[i] = EffectiveWorldParser.nmsBlockToMaterialIndex(type)
                        i++
                    }
                }
            }
        }

        return types
    }
}