package ru.hukm.effectiveSpigot.minecraft.mcv.interfaces

import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.chunk.IMcvEffectiveChunkManager

interface IMcvEffectiveModule {
    fun getChunk(): IMcvEffectiveChunkManager

    /**
     * Отправляет игроку пакет для относительного изменения вращения камеры.
     * Это позволяет избежать подергиваний при движении мыши.
     *
     * @param player Игрок
     * @param yawOffset Смещение по горизонтали
     * @param pitchOffset Смещение по вертикали
     */
    fun sendRelativeLook(player: Player, yawOffset: Float, pitchOffset: Float)
}