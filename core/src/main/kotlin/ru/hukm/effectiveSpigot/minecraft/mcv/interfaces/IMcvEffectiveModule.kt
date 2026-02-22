package ru.hukm.effectiveSpigot.minecraft.mcv.interfaces

import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager

interface IMcvEffectiveModule {
    fun getChunk(): IMcvEffectiveChunkManager
    fun getCurrentTick(): Int
}