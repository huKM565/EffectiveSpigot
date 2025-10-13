package ru.hukm.effectiveSpigot.minecraft.nms.interfaces.chunk

interface INmsEffectiveChunk {
    fun getBlocks(chunkX: Int, chunkZ: Int, world: String): ShortArray
}