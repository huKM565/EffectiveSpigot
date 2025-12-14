package ru.hukm.effectiveSpigot.minecraft.items

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveCraftable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveFoundableAndDropable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.InteractCallback

abstract class EffectiveItem {
    companion object {
        private val ITEM_KEY = NamespacedKey(EffectiveSpigot.instance, "item")
        val namespacedKeyToItem = hashMapOf<String, EffectiveItem>()

        fun equalByMaterial(item1: ItemStack?, item2: ItemStack?): Boolean {
            return item1?.type == item2?.type
        }

        fun equalByNamespacedKey(item1: ItemStack?, item2: ItemStack?): Boolean {
            val itemValue1 = getNamespacedKeyByItem(item1) ?: return false
            val itemValue2 = getNamespacedKeyByItem(item2) ?: return false

            return itemValue1 == itemValue2
        }

        fun equalByNamespacedKeyIfExistElseByMaterial(item1: ItemStack?, item2: ItemStack?): Boolean {
            val key1 = getNamespacedKeyByItem(item1)
            val key2 = getNamespacedKeyByItem(item2)

            if (key1 != null && key2 != null){
                return equalByNamespacedKey(item1, item2)
            }

            if (key1 == key2) {
                return equalByMaterial(item1, item2)
            }

            return false
        }

        fun getItemByNamespacedKey(namespacedKey: String): ItemStack? {
            return namespacedKeyToItem[namespacedKey]?.createItemStack()
        }

        fun getNamespacedKeyByItem(item: ItemStack?): String? {
            for (key in namespacedKeyToItem.keys) {
                val effectiveItem = namespacedKeyToItem[key]!!

                if(equalByMaterial(effectiveItem.createItemStack(), item)) {
                    return key
                }
            }

            return null
        }

        fun getGrayLore(lines: List<String>): List<String> {
            return lines.map { ChatColor.GRAY.toString() + it }
        }

        fun makeShapelessCraftable(item: ItemStack, ingredients: List<Material>, namespacedKey: NamespacedKey, useVariants: Boolean = true) {
            EffectiveCraftable.registerShapelessCraft(item, ingredients, EffectiveSpigot.instance, namespacedKey.toString(), useVariants)
        }

        fun makeShapedCraftable(item: ItemStack, shape: ArrayList<String>, ingredients: Map<Char, Material>, namespacedKey: NamespacedKey, useVariants: Boolean = true) {
            EffectiveCraftable.registerShapedCraft(item, shape, ingredients, EffectiveSpigot.instance, namespacedKey.toString(), useVariants)
        }

        fun makeFoundableAndDropable(item: ItemStack, dropChance: Double, lootTables: List<LootTables>) {
            EffectiveFoundableAndDropable.register(EffectiveFoundableAndDropable.Data(item, dropChance, lootTables as ArrayList<LootTables>))
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedKeyToItem.containsKey(namespacedName)) {
            throw IllegalArgumentException("Item with namespaced name '$namespacedName' is already registered")
        }
        namespacedKeyToItem[namespacedName] = this
    }

    fun createItemStack() = createItemStack(1)

    fun createItemStack(amount: Int): ItemStack {
        val item = ItemStack(getMaterial())
        item.amount = amount
        val meta = item.itemMeta ?: return item
        editMeta(meta)
        item.itemMeta = meta
        EffectiveDataContainerUtils.setContainerValue(item, ITEM_KEY, PersistentDataType.STRING, getNamespacedName())
        return item
    }

    fun equalByNamespacedKey(item: ItemStack) = equalByNamespacedKey(createItemStack(), item)

    fun makeClickable(click: EffectiveClickable.Click, callback: InteractCallback, ifRightClickOpenContainer: Boolean = false) {
        EffectiveClickable.register(createItemStack(), click, callback, ifRightClickOpenContainer)
    }

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