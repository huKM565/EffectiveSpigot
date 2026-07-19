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

typealias InteractCallback = (EffectiveEntityInteractable.EventsCallOptions) -> EffectiveAbstractInteract.Result

interface EffectiveEntityInteractable {
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Entity,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        val entity = target.entity
    }

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
                                    Click.RIGHT,
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
                                        Click.LEFT,
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