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

/** Callback for a click handler; its returned [EffectiveAbstractInteract.Result] cancels or allows the event. */
typealias InteractCallback = (EffectiveClickable.EventsCallOptions) -> EffectiveAbstractInteract.Result

/**
 * Behaviour that dispatches left/right-click interactions on custom items.
 *
 * A shared module bridges Bukkit interact/attack events into registered [Data] handlers, matching
 * the clicked stack by namespaced key (falling back to material). [EventsCallOptions] carries the click context.
 *
 * Two ways to register:
 * - [EffectiveItem.addClickHandler] — matches only that custom item (by key);
 * - [addClickHandler] directly with a **plain, non-custom stack** — since it has no key, matching
 *   falls back to [org.bukkit.Material], so the handler fires for *every* stack of that material
 *   (e.g. hook all sticks). Handy when you want behaviour on a vanilla material, not a custom item.
 *
 * Click mapping:
 * - [Click.LEFT] — left-click on air/block **and** attacking an entity (the attacked entity is in
 *   [EventsCallOptions.clickedEntity]).
 * - [Click.RIGHT] — right-click on air/block/entity.
 *
 * Callback result:
 * - [EffectiveAbstractInteract.Result.CANCEL_EVENT] cancels the underlying Bukkit event (suppresses
 *   the vanilla action — block placement, container open, attack, …).
 * - [EffectiveAbstractInteract.Result.ALLOW_EVENT] lets the vanilla action proceed.
 *
 * ```kotlin
 * addClickHandler(Click.RIGHT, { e ->
 *     e.player.sendMessage("used ${'$'}{e.item.type}")
 *     EffectiveAbstractInteract.Result.CANCEL_EVENT
 * })
 * ```
 */
interface EffectiveClickable {
    /** A registered click handler: which item, which [Click], the callback and optional cooldown. */
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Item,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>?,
        val ifRightClickOpenContainer: Boolean = false,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        val item = target.itemStack
    }

    /** Context passed to an [InteractCallback]: the player, the clicked item, the click type and hand,
     *  plus the clicked block/face/entity when applicable. */
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

        /** Clears the per-tick guard tracking players who already interacted with an entity. */
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

        /**
         * Registers a click handler for stacks matching [item]: by namespaced key if [item] is a custom
         * item, otherwise by [item]'s [org.bukkit.Material] (so pass a plain vanilla stack to match all
         * stacks of that material).
         *
         * @param click which button triggers [callback]
         * @param ifRightClickOpenContainer only relevant for [Click.RIGHT] on a container block
         *   (chest, barrel, …): when false (default) the handler runs and the container stays closed;
         *   when true the handler is skipped and the container opens as usual
         * @param cooldownData optional per-player cooldown gating the callback; while on cooldown the
         *   callback is not invoked
         */
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

        /**
         * Runs every handler matching the interacted item and returns whether the event should be
         * cancelled. Called by the module's event listeners; rarely needed directly.
         */
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