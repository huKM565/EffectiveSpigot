package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
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
                    //TODO(Не работает надевание предмета для Creative)
                    event<InventoryClickEvent> {
                        val player = it.whoClicked as? Player ?: return@event

                        val type = it.view.type
                        if (type != InventoryType.CRAFTING && type != InventoryType.CREATIVE) return@event

                        if (it.rawSlot == 5) {
                            val cursorItem = it.cursor
                            if (isWearable(cursorItem)) {
                                val currentHelmet = it.currentItem

                                it.currentItem = cursorItem.clone().apply { amount = 1 }

                                if (cursorItem.amount > 1) {
                                    cursorItem.amount -= 1
                                } else {
                                    it.view.setCursor(null)
                                }

                                if (currentHelmet != null && currentHelmet.type != Material.AIR) {
                                    if (cursorItem.amount <= 1) {
                                        it.view.setCursor(currentHelmet)
                                    } else {
                                        EffectiveInventoryUtils.giveItem(currentHelmet, player)
                                    }
                                }
                                it.isCancelled = true
                            }
                        }
                    }
                }
            }
        }
    }
}
