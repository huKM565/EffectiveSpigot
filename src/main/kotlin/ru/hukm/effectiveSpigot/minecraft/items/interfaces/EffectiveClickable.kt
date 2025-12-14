package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import java.util.UUID

typealias InteractCallback = (EffectiveClickable.EventsCallOptions) -> Boolean

interface EffectiveClickable {
    enum class Click { LEFT, RIGHT }

    data class Data(
        val item: ItemStack,
        val click: Click,
        val callback: InteractCallback,
        val ifRightClickOpenContainer: Boolean = false
    )

    data class EventsCallOptions(
        val player: Player,
        val item: ItemStack,
        val click: Click,
        val clickedBlock: Block?,
        val clickedEntity: Entity?,
    )

    companion object{
        private val clickableItems = arrayListOf<Data>()
        private val playerUUIDInteractedWithEntity = arrayListOf<UUID>()

        fun resetPlayerUUIDInteractedWithEntity() {
            playerUUIDInteractedWithEntity.clear()
        }

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

        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val item = eventsCallOptions.item

            if (item.type == Material.AIR) return false

            for (clickableItem in clickableItems) {
                val isEqual = EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(clickableItem.item, item)

                if (isEqual) {
                    if (clickableItem.click == Click.RIGHT) {
                        return if (eventsCallOptions.clickedBlock is Container && !clickableItem.ifRightClickOpenContainer) {
                            false
                        } else {
                            clickableItem.callback(eventsCallOptions)
                        }
                    }else if(clickableItem.click == Click.LEFT) {
                        return clickableItem.callback(eventsCallOptions)
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
            if (playerUUIDInteractedWithEntity.contains(event.player.uniqueId)) return
            if (tryCall(EventsCallOptions(event.player, event.item ?: ItemStack(Material.AIR), Click.RIGHT, event.clickedBlock, null))) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerInteractWithEntity(event: PlayerInteractAtEntityEvent) {
            playerUUIDInteractedWithEntity.add(event.player.uniqueId)
            if (tryCall(EventsCallOptions(event.player, EffectiveInventoryUtils.getItemFromEquipmentSlot(event.player, event.hand) ?: ItemStack(Material.AIR), Click.RIGHT, null , event.rightClicked))) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerHitEntity(event: EntityDamageByEntityEvent) {
            if (
                event.damager is Player &&
                event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                ) {
                tryCall(EventsCallOptions(event.damager as Player, EffectiveInventoryUtils.getUsedItemFromHands(event.damager as Player) ?: ItemStack(Material.AIR), Click.LEFT, null, event.entity))
            }
        }
    }
}