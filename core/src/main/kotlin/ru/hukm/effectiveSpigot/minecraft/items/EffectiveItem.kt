package ru.hukm.effectiveSpigot.minecraft.items

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.*
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import kotlin.text.lowercase

abstract class EffectiveItem {
    companion object {
        val ITEM_KEY = NamespacedKey(EffectiveSpigot.instance, "item")
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
            return if (item != null) {
                EffectiveDataContainerUtils.getContainerValue(item, ITEM_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        fun getNamespacedKeyByItemElseMaterial(item: ItemStack?): String? {
            return getNamespacedKeyByItem(item) ?: item?.type?.name
        }

        fun getGrayLore(lines: List<String>): List<String> {
            return lines.map { ChatColor.GRAY.toString() + it }
        }
    }

    init {
        //TODO(Сделать, чтобы нельзя было использовать названия обычных предметов)
        val namespacedName = getNamespacedName()
        if (namespacedKeyToItem.containsKey(namespacedName)) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.items.already_registered", namespacedName))
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

    fun equalByNamespacedKey(effectiveItem: EffectiveItem) = getNamespacedName() == effectiveItem.getNamespacedName()
    fun equalByNamespacedKey(item: ItemStack) = getNamespacedName() == getNamespacedKeyByItem(item)

    fun addClickHandler(
        click: Click,
        callback: InteractCallback,
        ifRightClickOpenContainer: Boolean = false,
        cooldownData: EffectiveAbstractInteract.CooldownData<EffectiveClickable.EventsCallOptions>? = null
    ) {
        EffectiveClickable.addClickHandler(createItemStack(), click, callback, ifRightClickOpenContainer, cooldownData)
    }

    fun addShapelessCraft(ingredients: List<Any>) {
        EffectiveCraftable.addShapelessCraft(createItemStack(), ingredients, EffectiveSpigot.instance, getNamespacedName())
    }

    fun addToLoot(
        dropChance: (Player?) -> Double,
        lootTables: List<LootTables>?,
        blocks: List<Material>?,
        entities: List<EntityType>?,
        amount: ((Player?) -> IntRange)? = null
    ) {
        EffectiveDropable.addLoot(
            EffectiveDropable.Data(
                createItemStack(),
                dropChance,
                lootTables,
                blocks,
                entities,
                amount
            )
        )
    }

    fun makeWearable() {
        EffectiveWearable.makeWearable(this)
    }

    abstract fun editMeta(meta: ItemMeta)
    abstract fun getMaterial(): Material
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}