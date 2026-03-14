package ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk

interface IMcvEffectiveChunkManager {
    fun getBlocks(chunkX: Int, chunkZ: Int, world: String): ShortArray
}