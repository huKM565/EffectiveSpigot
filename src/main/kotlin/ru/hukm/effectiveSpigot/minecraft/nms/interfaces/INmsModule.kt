package ru.hukm.effectiveSpigot.minecraft.nms.interfaces

import ru.hukm.effectiveSpigot.minecraft.nms.interfaces.chunk.INmsEffectiveChunk

interface INmsModule {
    fun getEffectiveChunk(): INmsEffectiveChunk
}