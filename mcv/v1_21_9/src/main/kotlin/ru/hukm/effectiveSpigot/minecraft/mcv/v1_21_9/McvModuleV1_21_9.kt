package ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.Relative
import net.minecraft.world.level.block.Block
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.IMcvEffectiveModule
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager
import ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.chunk.McvEffectiveChunkV1_21_9
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

    override fun sendRelativeLook(player: Player, yawOffset: Float, pitchOffset: Float) {
        val craftPlayer = player as CraftPlayer
        val handle = craftPlayer.handle

        val packet = ClientboundPlayerRotationPacket(
            yawOffset, true, pitchOffset, true
        )

        handle.connection.send(packet)
    }
}