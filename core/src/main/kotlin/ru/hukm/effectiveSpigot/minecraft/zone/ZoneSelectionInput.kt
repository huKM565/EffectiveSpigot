package ru.hukm.effectiveSpigot.minecraft.zone

import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos

object ZoneSelectionInput {

    /**
     * Записывает одну из точек выделения по клику игрока: достаёт блок из события,
     * пишет позицию в [EffectiveZoneSelection], уведомляет игрока.
     *
     * @return true, если был кликнут блок; false если клик был в воздух.
     */
    fun applyClick(
        options: EffectiveClickable.EventsCallOptions,
        posNum: String,
    ): Boolean {
        val block = options.clickedBlock ?: return false
        val loc = block.location
        val pos = EffectiveBlockPos(loc.blockX, loc.blockY, loc.blockZ)
        val uuid = options.player.uniqueId
        EffectiveZoneSelection.setSelection(uuid, pos, posNum, loc.world.uid)
        options.player.sendMessage(Locale.getMessage("items.zone_selector.pos$posNum", loc.blockX, loc.blockY, loc.blockZ))
        return true
    }
}
