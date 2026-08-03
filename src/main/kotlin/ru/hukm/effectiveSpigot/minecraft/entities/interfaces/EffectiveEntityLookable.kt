package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity

/** Predicate deciding whether a custom entity should turn to look at a given nearby entity. */
typealias EntityPredicate = (Entity) -> Boolean

/**
 * Behaviour that makes custom entities rotate to face nearby targets as those targets move.
 * Register via [EffectiveEntity.doEntityNearLookable].
 */
interface EffectiveEntityLookable {
    /** Ready-made look predicates. */
    enum class Look : EntityPredicate {
        /** Look only at nearby players. */
        TO_NEAR_PLAYER {
            override fun invoke(it: Entity) = it is Player
        },
        /** Look at any nearby entity. */
        TO_NEAR_ENTITY {
            override fun invoke(it: Entity) = true
        }
    }

    /**
     * Look configuration for one entity.
     * @property whoToLook predicate choosing which nearby entities to face
     * @property lookDistance max distance to react to, in blocks
     */
    data class Data(
        val entity: Entity,
        val whoToLook: (Entity) -> Boolean = Look.TO_NEAR_PLAYER,
        val lookDistance: Float = 5.0f
    )

    companion object {
        /** Hard cap on look distance, in blocks. */
        const val MAX_LOOK_DISTANCE = 32.0

        val lookableEntities = arrayListOf<Data>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<EntityMoveEvent> {
                        trySetLook(it.entity)
                    }

                    event<PlayerMoveEvent> {
                        trySetLook(it.player)
                    }
                }
            }
        }

        /**
         * Registers look behaviour for entities like [entity].
         * @throws IllegalArgumentException if [lookDistance] exceeds [MAX_LOOK_DISTANCE]
         */
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
}