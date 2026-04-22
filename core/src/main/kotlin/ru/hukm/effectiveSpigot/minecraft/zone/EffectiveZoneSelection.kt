package ru.hukm.effectiveSpigot.minecraft.zone

import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import java.util.UUID

object EffectiveZoneSelection {
    val playerToSelectedCoords = hashMapOf<UUID, Triple<EffectiveBlockPos?, EffectiveBlockPos?, UUID>>()

    /**
     * Устанавливает одну из точек выделения для игрока
     * @param uuid UUID игрока
     * @param pos Координаты блока
     * @param posNum Номер точки ("1" или "2")
     * @param worldUUID UUID мира
     * @return Обновленная пара координат
     */
    fun setSelection(uuid: UUID, pos: EffectiveBlockPos, posNum: String, worldUUID: UUID): Pair<EffectiveBlockPos?, EffectiveBlockPos?> {
        val current = playerToSelectedCoords[uuid]

        val currentCoords = if (current != null && current.third != worldUUID) {
            playerToSelectedCoords.remove(uuid)
            null to null
        } else {
            current?.let { it.first to it.second } ?: (null to null)
        }
        
        val updated = if (posNum == "1") pos to currentCoords.second else currentCoords.first to pos
        playerToSelectedCoords[uuid] = Triple(updated.first, updated.second, worldUUID)
        
        if (updated.first != null && updated.second != null) {
            EffectiveZoneRenderer.startRendering(playerToSelectedCoords[uuid], uuid, false)
        }
        return updated
    }

    /**
     * Получает текущее выделение игрока
     * @param uuid UUID игрока
     * @return Пара координат (может быть null)
     */
    fun getSelection(uuid: UUID): Triple<EffectiveBlockPos?, EffectiveBlockPos?, UUID>? {
        return playerToSelectedCoords[uuid]
    }

    /**
     * Получает UUID мира текущего выделения игрока
     * @param uuid UUID игрока
     * @return UUID мира или null
     */
    fun getSelectionWorldUUID(uuid: UUID): UUID? {
        return playerToSelectedCoords[uuid]?.third
    }
}