package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils

typealias InteractCallback = (Player, ItemStack, Block?, Entity?) -> Unit

interface EffectiveClickable {
    enum class Click { LEFT, RIGHT }

    data class Data(
        val item: ItemStack,
        val click: Click,
        val callback: InteractCallback,
        val ifRightClickOpenContainer: Boolean = false
    )

    companion object{
        private val clickableItems = arrayListOf<Data>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun register(item: ItemStack, click: Click, callback: InteractCallback, ifRightClickOpenContainer: Boolean = false) {
            clickableItems.add(Data(
                item,
                click,
                callback,
                ifRightClickOpenContainer
            ))
        }

        fun tryCall(item: ItemStack?, action: Action, clickedBlock: Block?, clickedEntity: Entity?, player: Player): Boolean {
            val item = item ?: return false
            val key = EffectiveItem.getNamespacedKeyByItem(item)

            for (clickableItem in clickableItems) {
                val isEqual = if(key == null) {
                    EffectiveItem.equalByMaterial(clickableItem.item, item)
                }else EffectiveItem.equalByNamespacedKey(clickableItem.item, item)

                if (isEqual) {
                    if (clickableItem.click == Click.RIGHT && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
                        if (clickedBlock is Container && clickableItem.ifRightClickOpenContainer) {
                            return false
                        } else {
                            clickableItem.callback(player, item, clickedBlock, clickedEntity)
                            return true
                        }
                    }else if(clickableItem.click == Click.LEFT && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
                        clickableItem.callback(player, item, clickedBlock, clickedEntity)
                        return true
                    }

                    break
                }
            }

            return false
        }
    }

    class Events() : Listener {
        @EventHandler
        fun onPlayerInteractEvent(event: PlayerInteractEvent) {
            if (tryCall(event.item, event.action, event.clickedBlock, null, event.player)) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerInteractWithEntity(event: PlayerInteractAtEntityEvent) {
            if (tryCall(EffectiveInventoryUtils.getItemFromEquipmentSlot(event.player, event.hand), Action.RIGHT_CLICK_AIR, null , event.rightClicked, event.player)) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerHitEntity(event: EntityDamageByEntityEvent) {
            if (
                event.damager is Player &&
                event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                tryCall(EffectiveInventoryUtils.getUsedItemFromHands(event.damager as Player), Action.RIGHT_CLICK_AIR, null, event.entity, event.damager as Player)) {
                event.isCancelled = true
            }
        }
    }
}