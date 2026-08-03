package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

/**
 * Inventory helpers. Item matching uses [EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial], so
 * custom items match by their key and vanilla items by material.
 */
object EffectiveInventoryUtils {
    /** Outcome of [giveItem]: fully added, or overflow dropped on the ground. */
    enum class GiveResult {
        SUCCESS,
        DROPPED
    }

    /** Whether the inventory's storage slots are all occupied (ignores armor/offhand for player inventories). */
    fun isFullInventory(inventory: Inventory): Boolean {
        var countDeleteInventoryContains = 0
        if (inventory.size >= 41) countDeleteInventoryContains = inventory.size - 36
        for (i in 0..<inventory.size - countDeleteInventoryContains) if (inventory.contents[i] == null) return false
        return true
    }

    /** Adds [item] to [player]'s inventory; returns the leftover that did not fit (empty if all fit). */
    fun tryGiveItem(item: ItemStack, player: Player): HashMap<Int, ItemStack> {
        val inventory: Inventory = player.inventory
        return inventory.addItem(item)
    }

    /** Gives [item] to [player], dropping any overflow at their feet. Reports whether anything dropped. */
    fun giveItem(item: ItemStack, player: Player): GiveResult {
        val leftOver = tryGiveItem(item, player)
        leftOver.values.forEach { player.world.dropItem(player.location, it) }

        return if (leftOver.isEmpty()) GiveResult.SUCCESS else GiveResult.DROPPED
    }

    /** The item in [player]'s given equipment [slot] (hands or armor), or null for unsupported slots. */
    fun getItemFromEquipmentSlot(player: Player, slot: EquipmentSlot): ItemStack? {
        return when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            EquipmentSlot.HEAD -> player.inventory.helmet
            EquipmentSlot.CHEST -> player.inventory.chestplate
            EquipmentSlot.LEGS -> player.inventory.leggings
            EquipmentSlot.FEET -> player.inventory.boots
            else -> null
        }
    }

    /** The main-hand item if not empty, otherwise the off-hand item. */
    fun getUsedItemFromHands(player: Player): ItemStack {
        val inventory = player.inventory

        val mainItem = inventory.itemInMainHand
        val offItem = inventory.itemInOffHand

        if (mainItem.type != Material.AIR) return mainItem

        return offItem
    }

    /** Given that one hand holds [item], returns the item in the *other* hand, or null if neither matches. */
    fun getItemFromAnotherHandByItemInHand(player: Player, item: ItemStack): ItemStack? {
        val inventory = player.inventory

        val mainItem = inventory.itemInMainHand
        val offItem = inventory.itemInOffHand

        if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(mainItem, item)) {
            return offItem
        }else if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(offItem, item)) {
            return mainItem
        }

        return null
    }

    /** Which hand holds [item] ([EquipmentSlot.HAND]/[EquipmentSlot.OFF_HAND]), or null if neither. */
    fun getHandThatHoldItem(player: Player, item: ItemStack): EquipmentSlot? {
        return if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(player.inventory.itemInMainHand, item)) {
            EquipmentSlot.HAND
        } else if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(player.inventory.itemInOffHand, item)) {
            EquipmentSlot.OFF_HAND
        } else {
            null
        }
    }

    /** Outcome of [removeItems]: all removed, or not enough were present (nothing removed). */
    enum class RemoveResult {
        SUCCESS,
        NOT_ENOUGH
    }

    /** Whether [inventory] holds at least [count] of [item] (summed across matching stacks). */
    fun hasItems(inventory: Inventory, item: ItemStack, count: Int): Boolean {
        var found = 0
        for (i in 0 until inventory.size) {
            val current = inventory.getItem(i) ?: continue
            if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(current, item)) {
                found += current.amount
            }
            if (found >= count) return true
        }
        return false
    }

    /** Removes [count] of [item] from [inventory]; if fewer are present, removes nothing and returns [RemoveResult.NOT_ENOUGH]. */
    fun removeItems(inventory: Inventory, item: ItemStack, count: Int): RemoveResult {
        val inv = inventory as? PlayerInventory ?: inventory
        if (!hasItems(inv, item, count)) return RemoveResult.NOT_ENOUGH

        var remaining = count
        for (i in 0 until inv.size) {
            val current = inv.getItem(i) ?: continue
            if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(current, item)) {
                val amount = current.amount
                if (amount > remaining) {
                    current.amount = amount - remaining
                    remaining = 0
                } else {
                    inv.setItem(i, null)
                    remaining -= amount
                }
            }
            if (remaining <= 0) break
        }
        return RemoveResult.SUCCESS
    }
}