package ru.hukm.effectiveSpigot.minecraft.item

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.EffectivePersistentDataContainer
import ru.hukm.effectiveSpigot.minecraft.item.interfaces.EffectiveCraftable
import ru.hukm.effectiveSpigot.minecraft.item.interfaces.EffectiveFoundableAndDropable

abstract class EffectiveItem {
    companion object {
        val ITEM_KEY = NamespacedKey(EffectiveSpigot.instance, "item")
        val NAMESPACED_KEY_TO_ITEM = hashMapOf<String, EffectiveItem>()

        fun equalByMaterial(item1: ItemStack, item2: ItemStack): Boolean {
            return item1.type == item2.type
        }

        fun equalByNamespacedKey(item1: ItemStack, item2: ItemStack): Boolean {
            val itemValue1 = EffectivePersistentDataContainer.getContainerValue(item1, ITEM_KEY, PersistentDataType.STRING) ?: return false
            val itemValue2 = EffectivePersistentDataContainer.getContainerValue(item2, ITEM_KEY, PersistentDataType.STRING) ?: return false

            return itemValue1 == itemValue2
        }

        fun getGrayLore(lines: List<String>): List<String> {
            return lines.map { ChatColor.GRAY.toString() + it }
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (NAMESPACED_KEY_TO_ITEM.containsKey(namespacedName)) {
            throw IllegalArgumentException("Item with namespaced name '$namespacedName' is already registered")
        }
        NAMESPACED_KEY_TO_ITEM[namespacedName] = this
    }

    fun createItemStack() = createItemStack(1)

    fun createItemStack(amount: Int): ItemStack {
        val item = ItemStack(getMaterial())
        item.amount = amount
        val meta = item.itemMeta ?: return item
        editMeta(meta)
        item.itemMeta = meta
        EffectivePersistentDataContainer.setContainerValue(item, ITEM_KEY, PersistentDataType.STRING, getNamespacedName())
        return item
    }

    fun equalByNamespacedKey(item: ItemStack) = equalByNamespacedKey(createItemStack(), item)

    fun makeShapelessCraftable(ingredients: List<Material>, useVariants: Boolean = true) {
        EffectiveCraftable.registerShapelessCraft(createItemStack(), ingredients, EffectiveSpigot.instance, getNamespacedName(), useVariants)
    }

    fun makeShapedCraftable(shape: ArrayList<String>, ingredients: Map<Char, Material>, useVariants: Boolean = true) {
        EffectiveCraftable.registerShapedCraft(createItemStack(), shape, ingredients, EffectiveSpigot.instance, getNamespacedName(), useVariants)
    }

    fun makeFoundableAndDropable(dropChance: Double, lootTables: List<LootTables>) {
        EffectiveFoundableAndDropable.register(EffectiveFoundableAndDropable.Data(createItemStack(), dropChance, lootTables as ArrayList<LootTables>))
    }

    abstract fun editMeta(meta: ItemMeta)
    abstract fun getMaterial(): Material
    abstract fun getNamespacedName(): String
}