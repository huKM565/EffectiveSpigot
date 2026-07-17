package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable

abstract class ZoneRegisteringSelector : EffectiveItem() {

    companion object {
        val registeredItemKeys = mutableSetOf<String>()
    }

    init {
        registeredItemKeys.add(getNamespacedName())
        addClickHandler(EffectiveAbstractInteract.Click.LEFT, { handleClick(it, "1") })
        addClickHandler(EffectiveAbstractInteract.Click.RIGHT, { handleClick(it, "2") })
    }

    abstract fun getZoneNamespacedKey(): String

    private fun handleClick(
        options: EffectiveClickable.EventsCallOptions,
        posNum: String,
    ): EffectiveAbstractInteract.Result {
        if (!ZoneSelectionInput.applyClick(options, posNum)) {
            return EffectiveAbstractInteract.Result.ALLOW_EVENT
        }

        val player = options.player
        val uuid = player.uniqueId
        val selection = EffectiveZoneSelection.getSelection(uuid)
        val p1 = selection?.first
        val p2 = selection?.second
        if (p1 != null && p2 != null) {
            val zoneKey = getZoneNamespacedKey()
            if (EffectiveZone.getZoneByNamespacedKey(zoneKey) != null) {
                val box = EffectiveZone.registerSelection(
                    Triple(p1, p2, selection.third), zoneKey, uuid
                )
                EffectiveZoneSelection.playerToSelectedCoords.remove(uuid)
                EffectiveZoneRenderer.stopRendering(uuid)
                player.sendMessage(Locale.getComponent("items.zone_registering_selector.registered", zoneKey, box.id))
            } else {
                player.sendMessage(Locale.getComponent("items.zone_registering_selector.zone_not_registered", zoneKey))
            }
        }

        return EffectiveAbstractInteract.Result.CANCEL_EVENT
    }
}
