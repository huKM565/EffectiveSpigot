package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import java.util.UUID

typealias InteractCallback = (EffectiveClickable.EventsCallOptions) -> EffectiveAbstractInteract.Result

interface EffectiveClickable {
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Item,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>?,
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
        val blockFace: BlockFace?,
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
                    //TODO(Добавить эвент разрушения блока)
                    event<PlayerInteractEvent>(EventPriority.HIGHEST) {
                        if (playerUUIDInteractedWithEntity.contains(it.player.uniqueId)) return@event
                        val click = if (it.action != Action.RIGHT_CLICK_BLOCK && it.action != Action.RIGHT_CLICK_AIR) Click.LEFT else Click.RIGHT
                        if (tryCall(EventsCallOptions(
                                it.player,
                                EffectiveAbstractInteract.Target.Item(it.item ?: ItemStack(Material.AIR)),
                                click,
                                it.hand ?: EquipmentSlot.HAND,
                                it.clickedBlock,
                                it.blockFace,
                                null)
                        )) {
                            it.isCancelled = true
                        }
                    }

                    event<PlayerInteractAtEntityEvent>(EventPriority.HIGHEST) {
                        playerUUIDInteractedWithEntity.add(it.player.uniqueId)
                        if (tryCall(EventsCallOptions(
                                it.player,
                                EffectiveAbstractInteract.Target.Item(
                                    EffectiveInventoryUtils.getItemFromEquipmentSlot(it.player, it.hand) ?: ItemStack(Material.AIR)
                                ),
                                Click.RIGHT,
                                it.hand,
                                null,
                                null,
                                it.rightClicked)
                        )) {
                            it.isCancelled = true
                        }
                    }

                    event<EntityDamageByEntityEvent>(EventPriority.HIGHEST) {
                        if (
                            it.damager is Player &&
                            it.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                            ) {
                            if (tryCall(EventsCallOptions(
                                    it.damager as Player,
                                    EffectiveAbstractInteract.Target.Item(
                                        EffectiveInventoryUtils.getUsedItemFromHands(it.damager as Player) ?: ItemStack(Material.AIR),
                                    ),
                                    Click.LEFT,
                                    EquipmentSlot.HAND,
                                    null,
                                    null,
                                    it.entity
                            ))) {
                                it.isCancelled = true
                            }
                        }
                    }
                }
            }
        }

        fun addClickHandler(
            item: ItemStack,
            click: Click,
            callback: InteractCallback,
            ifRightClickOpenContainer: Boolean = false,
            cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null
        ) {
            clickableItems.add(Data(
                EffectiveAbstractInteract.Target.Item(item),
                click,
                callback,
                cooldownData,
                ifRightClickOpenContainer
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
}