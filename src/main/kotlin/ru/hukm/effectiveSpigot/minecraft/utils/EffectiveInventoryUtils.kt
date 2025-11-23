package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

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
}