package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils

interface EffectiveWearable {
    companion object {
        private val wearableItems = arrayListOf<EffectiveItem>()

        fun makeWearable(effectiveItem: EffectiveItem) {
            if (wearableItems.none { it.equalByNamespacedKey(effectiveItem) }) {
                wearableItems.add(effectiveItem)

                //TODO(При ПКМ по шлему, если был предмет на голове, предмет моментально возращается обратно)
                effectiveItem.addClickHandler(EffectiveAbstractInteract.Click.RIGHT, { e ->
                    equipToHead(e.player, e.item)
                    EffectiveAbstractInteract.Result.CANCEL_EVENT
                })
            }
        }

        fun isWearable(item: ItemStack?): Boolean {
            if (item == null || item.type == Material.AIR) return false
            return wearableItems.any { it.equalByNamespacedKey(item) }
        }

        private fun equipToHead(player: Player, item: ItemStack) {
            val currentHelmet = player.inventory.helmet

            player.inventory.helmet = item.clone().apply { amount = 1 }

            item.amount -= 1

            if (currentHelmet != null && currentHelmet.type != Material.AIR) {
                EffectiveInventoryUtils.giveItem(currentHelmet, player)
            }
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }
    }

    class Events : Listener {
        //TODO(Не работает надевание предмета для Creative)
        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return

            val type = event.view.type
            if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE) return

            if (event.rawSlot == 5) {
                val cursorItem = event.cursor
                if (isWearable(cursorItem)) {
                    val currentHelmet = event.currentItem
                    
                    event.currentItem = cursorItem.clone().apply { amount = 1 }
                    
                    if (cursorItem.amount > 1) {
                        cursorItem.amount -= 1
                    } else {
                        event.view.setCursor(null)
                    }
                    
                    if (currentHelmet != null && currentHelmet.type != Material.AIR) {
                        if (cursorItem.amount <= 1) {
                            event.view.setCursor(currentHelmet)
                        } else {
                            EffectiveInventoryUtils.giveItem(currentHelmet, player)
                        }
                    }
                    event.isCancelled = true
                }
            }
        }
    }
}
