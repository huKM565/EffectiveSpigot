package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

enum class Items(val item: EffectiveItem) {
    EMPTY(object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            meta.setCustomModelData(1)
        }
        override fun getMaterial() = Material.FIREWORK_STAR
        override fun getNamespacedName() = "empty"
    });

    operator fun invoke() = item.createItemStack()
}