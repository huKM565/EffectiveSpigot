package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
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
                    event<PlayerDropItemEvent>(EventPriority.HIGH, ignoreCancelled = true) {
                        if (isUndropable(it.itemDrop.itemStack)) {
                            it.isCancelled = true
                        }
                    }

                    event<InventoryClickEvent>(EventPriority.HIGH, ignoreCancelled = true) {
                        when (it.action) {
                            InventoryAction.DROP_ONE_SLOT,
                            InventoryAction.DROP_ALL_SLOT -> {
                                if (isUndropable(it.currentItem)) it.isCancelled = true
                            }
                            InventoryAction.DROP_ONE_CURSOR,
                            InventoryAction.DROP_ALL_CURSOR -> {
                                if (isUndropable(it.cursor)) it.isCancelled = true
                            }
                            else -> Unit
                        }
                    }

                    event<PlayerDeathEvent>(EventPriority.HIGH, ignoreCancelled = true) {
                        val iterator = it.drops.iterator()
                        while (iterator.hasNext()) {
                            val drop = iterator.next()
                            if (isUndropable(drop)) {
                                it.itemsToKeep.add(drop)
                                iterator.remove()
                            }
                        }
                    }
                }
            }
        }
    }
}
