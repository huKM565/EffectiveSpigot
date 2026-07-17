package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity

typealias EntityPredicate = (Entity) -> Boolean

interface EffectiveEntityLookable {
    enum class Look : EntityPredicate {
        TO_NEAR_PLAYER {
            override fun invoke(it: Entity) = it is Player
        },
        TO_NEAR_ENTITY {
            override fun invoke(it: Entity) = true
        }
    }

    data class Data(
        val entity: Entity,
        val whoToLook: (Entity) -> Boolean = Look.TO_NEAR_PLAYER,
        val lookDistance: Float = 5.0f
    )

    companion object {
        const val MAX_LOOK_DISTANCE = 32.0

        val lookableEntities = arrayListOf<Data>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun doEntityLookable(
            entity: Entity,
            whoToLook: (Entity) -> Boolean = Look.TO_NEAR_PLAYER,
            lookDistance: Float = 5.0f
        ) {
            //TODO()
            if (lookDistance > MAX_LOOK_DISTANCE) {
                throw IllegalArgumentException(Locale.getMessage("errors.entities.look_distance_exceeded"))
            }

            lookableEntities.add(
                Data(
                    entity,
                    whoToLook,
                    lookDistance
                )
            )
        }

        private fun lookAt(observer: Entity, target: Entity) {
            val from = observer.location.clone()
            val to = target.location.clone()

            val direction = to.toVector().subtract(from.toVector())

            val locWithLook = from.setDirection(direction)

            observer.teleport(locWithLook)
        }

        private fun trySetLook(entity: Entity) {
            if (lookableEntities.isEmpty()) return

            entity.getNearbyEntities(
                MAX_LOOK_DISTANCE,
                MAX_LOOK_DISTANCE,
                MAX_LOOK_DISTANCE
            ).forEach {
                for (data in lookableEntities) {
                    if (
                        EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(data.entity, it) &&
                        entity.location.distance(it.location) <= data.lookDistance
                    ) {
                        if (data.whoToLook(entity)) {
                            lookAt(it, entity)
                        }
                    }
                }
            }
        }
    }

    class Events : Listener {
        @EventHandler
        fun onEntityMove(event: EntityMoveEvent) {
            trySetLook(event.entity)
        }

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            trySetLook(event.player)
        }
    }
}