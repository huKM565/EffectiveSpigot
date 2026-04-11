package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
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
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
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

    class Events : Listener {
        @EventHandler
        fun onPlayerInteractWithEntity(event: PlayerInteractAtEntityEvent) {
            if (tryCall(
                    EventsCallOptions(
                        event.player,
                        EffectiveAbstractInteract.Target.Entity(
                            event.rightClicked
                        ),
                        Click.RIGHT,
                        event.hand
                    )
                )
            ) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerHitEntity(event: EntityDamageByEntityEvent) {
            if (
                event.damager is Player &&
                event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            ) {
                if (tryCall(
                        EventsCallOptions(
                            event.damager as Player,
                            EffectiveAbstractInteract.Target.Entity(
                                event.entity
                            ),
                            Click.LEFT,
                            EquipmentSlot.HAND
                        )
                )) {
                    event.isCancelled = true
                }
            }
        }
    }
}