package ru.hukm.effectiveSpigot.minecraft.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.*
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils

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

        fun getEffectiveItemByNamespacedKey(namespacedKey: String): EffectiveItem? {
            return namespacedKeyToItem[namespacedKey]
        }

        fun getItemByNamespacedKey(namespacedKey: String): ItemStack? {
            return getEffectiveItemByNamespacedKey(namespacedKey)?.createItemStack()
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

        fun getGrayLore(lines: List<String>): List<Component> {
            return lines.map { Component.text(it, NamedTextColor.GRAY) }
        }

    }

    init {
        //TODO(Сделать, чтобы нельзя было использовать названия обычных предметов)
        val namespacedName = getNamespacedName()
        if (namespacedKeyToItem.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.items.already_registered", namespacedName))
        }
        namespacedKeyToItem[namespacedName] = this
    }

    fun createItemStack() = createItemStack(1)

    fun createItemStack(amount: Int): ItemStack {
        val item = ItemStack(getMaterial())
        return finalizeItem(item, amount)
    }

    private fun finalizeItem(item: ItemStack, amount: Int): ItemStack {
        item.amount = amount
        val meta = item.itemMeta ?: return item
        editMeta(meta)
        item.itemMeta = meta
        EffectiveDataContainerUtils.setContainerValue(item, ITEM_KEY, PersistentDataType.STRING, getNamespacedName())
        createItemStackCallback(item)
        return item
    }

    fun createItemStack(additionalArgs: List<String>): ItemStack {
        return createItemStack(1, additionalArgs)
    }

    fun createItemStack(amount: Int, additionalArgs: List<String>): ItemStack {
        val item = ItemStack(getMaterial())
        val meta = item.itemMeta

        val args = getAdditionalArgs()

        AdditionalArgsSupport.applyToHolder(meta, args, additionalArgs, "items")

        if (showAdditionArgsInLore() && args != null) {
            val tempLore = meta.lore?.toMutableList() ?: mutableListOf()

            val keys = args.keys.map { it.first }
            val values = AdditionalArgsSupport.readFromHolder(meta, args)

            values.forEachIndexed { index, value ->
                tempLore.add("${keys[index]}: ${value[index]}")
            }

            meta.lore = tempLore
        }

        item.itemMeta = meta

        return finalizeItem(item, amount)
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

    fun addBrewRecipe(inputIngredient: ItemStack, inputBasePotionMeta: PotionMeta, fuelUse: Int, cookingTime: Int) {
        EffectiveBrewable.registerRecipe(EffectiveBrewable.Data(
            result = createItemStack(),
            inputIngredient = inputIngredient,
            inputBasePotionMeta = inputBasePotionMeta,
            fuelUse = fuelUse,
            cookingTime = cookingTime
        ))
    }

    fun makeWearable() {
        EffectiveWearable.makeWearable(this)
    }

    fun makeUndropable() {
        EffectiveUndropable.makeUndropable(this)
    }

    abstract fun editMeta(meta: ItemMeta)
    abstract fun getMaterial(): Material
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    open fun createItemStackCallback(item: ItemStack) {}
    open fun getAdditionalArgs(): AdditionalArgs? {
        return null
    }
    open fun showAdditionArgsInLore() : Boolean = false

    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "items")

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}