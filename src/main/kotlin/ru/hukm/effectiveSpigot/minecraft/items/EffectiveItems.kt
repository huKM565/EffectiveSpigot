package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.zone.ZoneSelectionInput

/**
 * Built-in items shipped by the framework. Invoke an entry (`EffectiveItems.ZONE_SELECTOR()`) to get a
 * fresh `ItemStack`.
 *
 * - [EMPTY] — a `FIREWORK_STAR` with a single-space name and a transparent texture; useful as a
 *   placeholder slot in [EffectiveMenu] layouts.
 * - [ZONE_SELECTOR] — a `BLAZE_ROD` given out by `/ezone`. Left-click sets corner 1, right-click sets
 *   corner 2 of the pending [EffectiveZone] box.
 */
enum class EffectiveItems(val item: EffectiveItem) {
    EMPTY(object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            meta.setDisplayName(" ")
        }
        override fun getMaterial() = Material.FIREWORK_STAR
        override fun getNamespacedData() = EffectiveSpigot.instance to "empty"
        override fun getResourcePackData() = ResourcePackData(
            "textures/empty.png"
        )
    }),
    ZONE_SELECTOR(object : EffectiveItem() {
        init {
            addClickHandler(EffectiveAbstractInteract.Click.LEFT, { handleSelection(it, "1") })
            addClickHandler(EffectiveAbstractInteract.Click.RIGHT, { handleSelection(it, "2") })
        }

        override fun editMeta(meta: ItemMeta) {
            meta.displayName(Locale.getComponent("items.zone_selector.name"))
        }

        override fun getMaterial() = Material.BLAZE_ROD
        override fun getNamespacedData() = EffectiveSpigot.instance to "zone_selector"

        private fun handleSelection(
            options: EffectiveClickable.EventsCallOptions,
            posNum: String
        ): EffectiveAbstractInteract.Result {
            return if (ZoneSelectionInput.applyClick(options, posNum))
                EffectiveAbstractInteract.Result.CANCEL_EVENT
            else
                EffectiveAbstractInteract.Result.ALLOW_EVENT
        }
    });

    operator fun invoke() = item.createItemStack()
}