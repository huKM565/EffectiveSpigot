package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click

/** Callback for an entity interact handler; its [EffectiveAbstractInteract.Result] cancels or allows the event. */
typealias InteractCallback = (EffectiveEntityInteractable.EventsCallOptions) -> EffectiveAbstractInteract.Result

/**
 * Behaviour that dispatches left/right interactions on custom entities, mirroring
 * [ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable] for entities.
 *
 * `RIGHT` = right-click on the entity, `LEFT` = attacking it. Entities are matched by custom-entity key
 * (falling back to [org.bukkit.entity.EntityType]).
 *
 * Two ways to register:
 * - [EffectiveEntity.addInteractHandler] — matches only that custom entity type (by key);
 * - [addInteractHandler] directly with a **plain, non-custom entity** — since it has no key, matching
 *   falls back to [org.bukkit.entity.EntityType], so the handler fires for *every* vanilla entity of
 *   that type (e.g. hook all cows). Handy when you want behaviour on a vanilla type, not a custom one.
 */
interface EffectiveEntityInteractable {
    /** A registered entity interact handler: target entity, [Click], callback and optional cooldown. */
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Entity,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        val entity = target.entity
    }

    /** Context passed to an [InteractCallback]: the player, the clicked entity, the click type and hand. */
    data class EventsCallOptions(
        override val player: Player,
        override val target: EffectiveAbstractInteract.Target.Entity,
        override val click: Click,
        override val hand: EquipmentSlot,
    ) : EffectiveAbstractInteract.EventsCallOptions<EffectiveAbstractInteract.Target.Entity> {
        val clickedEntity = target.entity
    }

    companion object {
        val interactableEntities = arrayListOf<Data>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<PlayerInteractAtEntityEvent> {
                        if (tryCall(
                                EventsCallOptions(
                                    it.player,
                                    EffectiveAbstractInteract.Target.Entity(
                                        it.rightClicked
                                    ),
                                    EffectiveAbstractInteract.resolveClick(it.player, isRight = true),
                                    it.hand
                                )
                            )
                        ) {
                            it.isCancelled = true
                        }
                    }

                    event<EntityDamageByEntityEvent> {
                        if (
                            it.damager is Player &&
                            it.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                        ) {
                            if (tryCall(
                                    EventsCallOptions(
                                        it.damager as Player,
                                        EffectiveAbstractInteract.Target.Entity(
                                            it.entity
                                        ),
                                        EffectiveAbstractInteract.resolveClick(it.damager as Player, isRight = false),
                                        EquipmentSlot.HAND
                                    )
                            )) {
                                it.isCancelled = true
                            }
                        }
                    }
                }
            }
        }

        /**
         * Registers an interact handler matching entities like [entity]: by custom-entity key if
         * [entity] has one, otherwise by [entity]'s [org.bukkit.entity.EntityType] (so pass a plain
         * vanilla entity to match all entities of that type).
         */
        fun addInteractHandler(
            entity: Entity,
            click: Click,
            callback: InteractCallback,
            cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null
        ) {
            interactableEntities.add(
                Data(
                    EffectiveAbstractInteract.Target.Entity(entity),
                    click,
                    callback,
                    cooldownData
                )
            )
        }

        /** Runs matching handlers for the interacted entity; returns whether the event should be cancelled. */
        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val entity = eventsCallOptions.clickedEntity

            var result = false

            for (interactableEntity in interactableEntities) {
                val isEqual = EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(interactableEntity.target.entity, entity)

                if (isEqual) {
                    result = EffectiveAbstractInteract.runCallAndUpdateResult(
                        result,
                        interactableEntity,
                        eventsCallOptions
                    )
                }
            }

            return result
        }
    }
}