package ru.hukm.effectiveSpigot.minecraft.zone

import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import java.util.UUID

object EffectiveZoneSelection {
    val playerToSelectedCoords = hashMapOf<UUID, Pair<EffectiveBlockPos?, EffectiveBlockPos?>>()

    /**
     * Устанавливает одну из точек выделения для игрока
     * @param uuid UUID игрока
     * @param pos Координаты блока
     * @param posNum Номер точки ("1" или "2")
     * @return Обновленная пара координат
     */
    fun setSelection(uuid: UUID, pos: EffectiveBlockPos, posNum: String): Pair<EffectiveBlockPos?, EffectiveBlockPos?> {
        val current = playerToSelectedCoords[uuid] ?: (null to null)
        val updated = if (posNum == "1") pos to current.second else current.first to pos
        playerToSelectedCoords[uuid] = updated
        if (updated.first != null && updated.second != null) {
            EffectiveZoneRenderer.startRendering({ playerToSelectedCoords[uuid] })
        }
        return updated
    }

    /**
     * Получает текущее выделение игрока
     * @param uuid UUID игрока
     * @return Пара координат (может быть null)
     */
    fun getSelection(uuid: UUID): Pair<EffectiveBlockPos?, EffectiveBlockPos?>? {
        return playerToSelectedCoords[uuid]
    }
}