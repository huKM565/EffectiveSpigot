package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import ru.hukm.effectiveSpigot.EffectiveSpigot

enum class EffectiveItems(val item: EffectiveItem) {
    EMPTY(object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            meta.setCustomModelData(1)
        }
        override fun getMaterial() = Material.FIREWORK_STAR
        override fun getNamespacedData() = EffectiveSpigot.instance to "empty"
    });

    operator fun invoke() = item.createItemStack()
}