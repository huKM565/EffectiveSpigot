package ru.hukm.effectiveSpigot.minecraft.nms.v1_21_6

import ru.hukm.effectiveSpigot.minecraft.nms.interfaces.INmsModule
import ru.hukm.effectiveSpigot.minecraft.nms.v1_21_6.chunk.NmsEffectiveChunkV1_21_6

class NmsModuleV1_21_6: INmsModule {
    override fun getEffectiveChunk() = NmsEffectiveChunkV1_21_6()
}