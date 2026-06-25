package ru.hukm.effectiveSpigot.minecraft.items

import net.md_5.bungee.api.ChatColor
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

        fun getGrayLore(lines: List<String>): List<String> {
            return lines.map { ChatColor.GRAY.toString() + it }
        }

    }

    data class AdditionalArgs(
        val instance: JavaPlugin,
        val keys: List<Pair<String, PersistentDataType<*, *>>>
    )

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
        if (additionalArgs.isEmpty()) return createItemStack(amount)

        val namespacedKeysToPersistenceType = getAdditionalArgsNamespacedKeys() ?: return createItemStack(amount)
        if (namespacedKeysToPersistenceType.isEmpty()) return createItemStack(amount)
        if (additionalArgs.size != namespacedKeysToPersistenceType.size) throw IllegalArgumentException(
            Locale.getMessage(
                "errors.items.wrong_additional_args_count",
                namespacedKeysToPersistenceType.size,
                additionalArgs.size
            )
        )

        namespacedKeysToPersistenceType.forEachIndexed { index, pair ->
            val raw = additionalArgs[index]
            val value = parseArg(raw, pair.second)
                ?: throw IllegalArgumentException(
                    Locale.getMessage(
                        "errors.items.cannot_parse_additional_arg",
                        index + 1,
                        raw,
                        pair.first.key
                    )
                )
            @Suppress("UNCHECKED_CAST")
            EffectiveDataContainerUtils.setContainerValue(
                item,
                pair.first,
                pair.second as PersistentDataType<Any, Any>,
                value
            )
        }

        return finalizeItem(item, amount)
    }

    private fun parseArg(raw: String, type: PersistentDataType<*, *>): Any? = when (type) {
        PersistentDataType.STRING       -> raw
        PersistentDataType.BYTE         -> raw.toByteOrNull()
        PersistentDataType.SHORT        -> raw.toShortOrNull()
        PersistentDataType.INTEGER      -> raw.toIntOrNull()
        PersistentDataType.LONG         -> raw.toLongOrNull()
        PersistentDataType.FLOAT        -> raw.toFloatOrNull()
        PersistentDataType.DOUBLE       -> raw.toDoubleOrNull()
        PersistentDataType.BOOLEAN      -> raw.toBooleanStrictOrNull()
        PersistentDataType.BYTE_ARRAY   -> raw.split(",").mapNotNull { it.trim().toByteOrNull() }.toByteArray()
        PersistentDataType.INTEGER_ARRAY -> raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
        PersistentDataType.LONG_ARRAY   -> raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toLongArray()
        else -> null
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

    abstract fun editMeta(meta: ItemMeta)
    abstract fun getMaterial(): Material
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    open fun createItemStackCallback(item: ItemStack) {}
    open fun getAdditionalArgs(): AdditionalArgs? {
        return null
    }

    fun getAdditionalArgsNamespacedKeys(): List<Pair<NamespacedKey, PersistentDataType<*, *>>>? {
        val namespacedKeys = arrayListOf<Pair<NamespacedKey, PersistentDataType<*, *>>>()
        val additionalArgs = getAdditionalArgs() ?: return null

        for (pair in additionalArgs.keys) {
            namespacedKeys.add(
                Pair(
                    NamespacedKey(additionalArgs.instance, pair.first),
                    pair.second
                )
            )
        }

        return namespacedKeys
    }

    fun additionalKey(name: String): NamespacedKey {
        val args = getAdditionalArgs()
            ?: throw IllegalStateException(Locale.getMessage("errors.items.no_additional_args_defined"))
        require(args.keys.any { it.first == name }) {
            Locale.getMessage("errors.items.unknown_additional_arg_key", name)
        }
        return NamespacedKey(args.instance, name)
    }

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}