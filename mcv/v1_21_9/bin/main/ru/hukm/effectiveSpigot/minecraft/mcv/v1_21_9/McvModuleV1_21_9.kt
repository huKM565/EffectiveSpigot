package ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9

import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.IMcvEffectiveModule
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager
import ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.chunk.McvEffectiveChunkV1_21_9
import org.bukkit.Material
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser

object McvModuleV1_21_9 : IMcvEffectiveModule {
    private val mcvBlocksToMaterialsIndex: HashMap<Block, Short> = hashMapOf()

    fun mcvBlockToMaterialIndex(type: Block): Short {
        return EffectiveWorldParser.initAndGetFromCache(
            type,
            { Material.values().indexOf(Material.valueOf(BuiltInRegistries.BLOCK.wrapAsHolder(type).registeredName.substringAfter("minecraft:").uppercase())).toShort() },
            mcvBlocksToMaterialsIndex
        )
    }

    override fun getChunk(): IMcvEffectiveChunkManager {
        return McvEffectiveChunkV1_21_9
    }
}