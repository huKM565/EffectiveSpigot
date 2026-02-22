package ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_11

import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.IMcvEffectiveModule
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager
import ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_11.chunk.NmsEffectiveChunkV1_21_11
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_21_R7.CraftServer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorldParser

object McvModuleV1_21_11 : IMcvEffectiveModule {
    private val mcvBlocksToMaterialsIndex: HashMap<Block, Short> = hashMapOf()

    fun mcvBlockToMaterialIndex(type: Block): Short {
        return EffectiveWorldParser.initAndGetFromCache(
            type,
            { Material.entries.indexOf(Material.valueOf(BuiltInRegistries.BLOCK.wrapAsHolder(type).registeredName.substringAfter("minecraft:").uppercase())).toShort() },
            mcvBlocksToMaterialsIndex
        )
    }

    override fun getChunk(): IMcvEffectiveChunkManager {
        return NmsEffectiveChunkV1_21_11
    }

    override fun getCurrentTick(): Int {
        return (Bukkit.getServer() as CraftServer).server.tickCount
    }
}