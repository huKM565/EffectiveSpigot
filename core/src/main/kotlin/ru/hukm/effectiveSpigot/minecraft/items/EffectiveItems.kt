package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZoneSelection

enum class EffectiveItems(val item: EffectiveItem) {
    EMPTY(object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            meta.setCustomModelData(1)
        }
        override fun getMaterial() = Material.FIREWORK_STAR
        override fun getNamespacedData() = EffectiveSpigot.instance to "empty"
    }),
    ZONE_SELECTOR(object : EffectiveItem() {
        init {
            addClickHandler(EffectiveAbstractInteract.Click.LEFT, { handleSelection(it, "1") })
            addClickHandler(EffectiveAbstractInteract.Click.RIGHT, { handleSelection(it, "2") })
        }

        override fun editMeta(meta: ItemMeta) {
            meta.setDisplayName(Locale.getMessage("items.zone_selector.name"))
        }

        override fun getMaterial() = Material.BLAZE_ROD
        override fun getNamespacedData() = EffectiveSpigot.instance to "zone_selector"

        private fun handleSelection(
            options: EffectiveClickable.EventsCallOptions,
            posNum: String
        ): EffectiveAbstractInteract.Result {
            val block = options.clickedBlock ?: return EffectiveAbstractInteract.Result.ALLOW_EVENT
            val loc = block.location

            val pos = EffectiveBlockPos(loc.blockX, loc.blockY, loc.blockZ)
            val uuid = options.player.uniqueId
            EffectiveZoneSelection.setSelection(uuid, pos, posNum, loc.world.uid)
            options.player.sendMessage(Locale.getMessage("items.zone_selector.pos$posNum", loc.blockX, loc.blockY, loc.blockZ))
            return EffectiveAbstractInteract.Result.CANCEL_EVENT
        }
    });

    operator fun invoke() = item.createItemStack()
}