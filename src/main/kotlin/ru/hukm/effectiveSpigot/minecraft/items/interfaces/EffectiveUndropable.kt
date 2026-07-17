package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

interface EffectiveUndropable {
    companion object {
        private val undropableItems = arrayListOf<EffectiveItem>()

        fun makeUndropable(effectiveItem: EffectiveItem) {
            if (undropableItems.none { it.equalByNamespacedKey(effectiveItem) }) {
                undropableItems.add(effectiveItem)
            }
        }

        fun isUndropable(item: ItemStack?): Boolean {
            if (item == null || item.type == Material.AIR) return false
            return undropableItems.any { it.equalByNamespacedKey(item) }
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }
    }

    private class Events : Listener {
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onPlayerDrop(event: PlayerDropItemEvent) {
            if (isUndropable(event.itemDrop.itemStack)) {
                event.isCancelled = true
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onInventoryClick(event: InventoryClickEvent) {
            when (event.action) {
                InventoryAction.DROP_ONE_SLOT,
                InventoryAction.DROP_ALL_SLOT -> {
                    if (isUndropable(event.currentItem)) event.isCancelled = true
                }
                InventoryAction.DROP_ONE_CURSOR,
                InventoryAction.DROP_ALL_CURSOR -> {
                    if (isUndropable(event.cursor)) event.isCancelled = true
                }
                else -> Unit
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onPlayerDeath(event: PlayerDeathEvent) {
            val iterator = event.drops.iterator()
            while (iterator.hasNext()) {
                val drop = iterator.next()
                if (isUndropable(drop)) {
                    event.itemsToKeep.add(drop)
                    iterator.remove()
                }
            }
        }
    }
}
