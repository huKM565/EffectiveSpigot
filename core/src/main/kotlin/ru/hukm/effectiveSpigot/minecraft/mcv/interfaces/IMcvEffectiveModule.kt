package ru.hukm.effectiveSpigot.minecraft.mcv.interfaces

import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager

interface IMcvEffectiveModule {
    fun getChunk(): IMcvEffectiveChunkManager

    fun sendRelativeLook(player: Player, yawOffset: Float, pitchOffset: Float)
}