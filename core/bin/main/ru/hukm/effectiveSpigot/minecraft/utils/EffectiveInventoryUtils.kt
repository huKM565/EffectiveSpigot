package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

object EffectiveInventoryUtils {
    enum class GiveResult {
        SUCCESS,
        DROPPED
    }

    fun isFullInventory(inventory: Inventory): Boolean {
        var countDeleteInventoryContains = 0
        if (inventory.size >= 41) countDeleteInventoryContains = inventory.size - 36
        for (i in 0..<inventory.size - countDeleteInventoryContains) if (inventory.contents[i] == null) return false
        return true
    }

    fun tryGiveItem(item: ItemStack, player: Player): HashMap<Int, ItemStack> {
        val inventory: Inventory = player.inventory
        return inventory.addItem(item)
    }

    fun giveItem(item: ItemStack, player: Player): GiveResult {
        val leftOver = tryGiveItem(item, player)
        leftOver.values.forEach { player.world.dropItem(player.location, it) }

        return if (leftOver.isEmpty()) GiveResult.SUCCESS else GiveResult.DROPPED
    }

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

    fun getUsedItemFromHands(player: Player): ItemStack? {
        val inventory = player.inventory

        val mainItem = inventory.itemInMainHand
        val offItem = inventory.itemInOffHand

        if (mainItem.type != Material.AIR) return mainItem

        return offItem
    }

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

    fun getHandThatHoldItem(player: Player, item: ItemStack): EquipmentSlot? {
        return if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(player.inventory.itemInMainHand, item)) {
            EquipmentSlot.HAND
        } else if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(player.inventory.itemInOffHand, item)) {
            EquipmentSlot.OFF_HAND
        } else {
            null
        }
    }

    enum class RemoveResult {
        SUCCESS,
        NOT_ENOUGH
    }

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

    fun removeItems(inventory: Inventory, item: ItemStack, count: Int): RemoveResult {
        if (!hasItems(inventory, item, count)) return RemoveResult.NOT_ENOUGH

        var remaining = count
        for (i in 0 until inventory.size) {
            val current = inventory.getItem(i) ?: continue
            if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(current, item)) {
                val amount = current.amount
                if (amount > remaining) {
                    current.amount = amount - remaining
                    remaining = 0
                } else {
                    inventory.setItem(i, null)
                    remaining -= amount
                }
            }
            if (remaining <= 0) break
        }
        return RemoveResult.SUCCESS
    }
}