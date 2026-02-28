package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
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
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import java.util.UUID

typealias InteractCallback = (EffectiveClickable.EventsCallOptions) -> EffectiveAbstractInteract.Result
typealias ConditionForSkipCooldown = (EffectiveClickable.EventsCallOptions) -> Boolean

interface EffectiveClickable {
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Item,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownToUseInTicks: Int,
        override val conditionForSkipCooldown: ConditionForSkipCooldown?,
        override val cooldownType: EffectiveAbstractInteract.CooldownType,
        val ifRightClickOpenContainer: Boolean = false,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        val item = target.itemStack
    }

    data class EventsCallOptions (
        override val player: Player,
        override val target: EffectiveAbstractInteract.Target.Item,
        override val click: Click,
        override val hand: EquipmentSlot,
        val clickedBlock: Block?,
        val clickedEntity: Entity?,
    ) : EffectiveAbstractInteract.EventsCallOptions<EffectiveAbstractInteract.Target.Item> {
        val item = target.itemStack
    }

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

        fun addClickHandler(
            item: ItemStack,
            click: Click,
            callback: InteractCallback,
            ifRightClickOpenContainer: Boolean = false,
            cooldownToUseInTicks: Int = 0,
            conditionForSkipCooldown: ConditionForSkipCooldown? = null,
            cooldownType: EffectiveAbstractInteract.CooldownType = EffectiveAbstractInteract.CooldownType.ON_CURRENT_PLAYER
        ) {
            clickableItems.add(Data(
                EffectiveAbstractInteract.Target.Item(item),
                click,
                callback,
                cooldownToUseInTicks,
                conditionForSkipCooldown,
                cooldownType
            ))
        }

        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val item = eventsCallOptions.target.itemStack

            if (item.type == Material.AIR) return false

            var result = false

            for (clickableItem in clickableItems) {
                val isEqual = EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(clickableItem.item, item)

                if (isEqual) {
                    if (clickableItem.click == Click.RIGHT && eventsCallOptions.click == Click.RIGHT) {
                        if (!(eventsCallOptions.clickedBlock is Container && !clickableItem.ifRightClickOpenContainer)) {
                            result = EffectiveAbstractInteract.runCallAndUpdateResult(result, clickableItem, eventsCallOptions)
                        }
                    } else if (clickableItem.click == Click.LEFT && eventsCallOptions.click == Click.LEFT) {
                        result = EffectiveAbstractInteract.runCallAndUpdateResult(result, clickableItem, eventsCallOptions)
                    }
                }
            }

            return result
        }
    }

    //TODO(Добавить эвент разрушения блока)
    class Events() : Listener {
        @EventHandler
        fun onPlayerInteractEvent(event: PlayerInteractEvent) {
            if (playerUUIDInteractedWithEntity.contains(event.player.uniqueId)) return
            val click = if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) Click.LEFT else Click.RIGHT
            if (tryCall(EventsCallOptions(
                    event.player,
                    EffectiveAbstractInteract.Target.Item(event.item ?: ItemStack(Material.AIR)),
                    click,
                    event.hand ?: EquipmentSlot.HAND,
                    event.clickedBlock,
                    null)
            )) {
                event.isCancelled = true
            }

        }

        @EventHandler
        fun onPlayerInteractWithEntity(event: PlayerInteractAtEntityEvent) {
            playerUUIDInteractedWithEntity.add(event.player.uniqueId)
            if (tryCall(EventsCallOptions(
                    event.player,
                    EffectiveAbstractInteract.Target.Item(
                        EffectiveInventoryUtils.getItemFromEquipmentSlot(event.player, event.hand) ?: ItemStack(Material.AIR)
                    ),
                    Click.RIGHT,
                    event.hand,
                    null,
                    event.rightClicked)
            )) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerHitEntity(event: EntityDamageByEntityEvent) {
            if (
                event.damager is Player &&
                event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                ) {
                if (tryCall(EventsCallOptions(
                        event.damager as Player,
                        EffectiveAbstractInteract.Target.Item(
                            EffectiveInventoryUtils.getUsedItemFromHands(event.damager as Player) ?: ItemStack(Material.AIR),
                        ),
                        Click.LEFT,
                        EquipmentSlot.HAND,
                        null,
                        event.entity
                ))) {
                    event.isCancelled = true
                }

            }
        }
    }
}