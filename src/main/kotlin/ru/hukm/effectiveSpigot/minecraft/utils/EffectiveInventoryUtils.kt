package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

object EffectiveInventoryUtils {
    fun isFullInventory(inventory: Inventory): Boolean {
        var countDeleteInventoryContains = 0
        if (inventory.size >= 41) countDeleteInventoryContains = inventory.size - 36
        for (i in 0..<inventory.size - countDeleteInventoryContains) if (inventory.contents[i] == null) return false
        return true
    }

    fun tryGiveItem(item: ItemStack, player: Player): Boolean {
        val inventory: Inventory = player.inventory

        if (!isFullInventory(inventory)) {
            inventory.addItem(item)
            return true
        }

        return false
    }

    fun giveItem(item: ItemStack, player: Player): Boolean {
        if (!tryGiveItem(item, player)) {
            player.world.dropItem(player.location, item)
            return false
        }

        return true
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
}