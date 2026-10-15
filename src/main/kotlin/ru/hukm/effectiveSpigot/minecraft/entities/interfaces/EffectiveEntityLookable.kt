package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.papermc.paper.event.entity.EntityMoveEvent
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.bukkit.event.player.PlayerMoveEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sign

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
     * @property turnSpeed how fast the entity turns toward the target, in degrees per tick.
     *   `0` (the default) snaps instantly; a positive value turns smoothly, capped at that many degrees each tick.
     * @property lookAtSpectators whether players in spectator mode are looked at. `false` by default, like vanilla
     *   mobs; checked before [whoToLook], so a predicate such as `it is Player` does not need to exclude spectators.
     */
    data class Data(
        val entity: Entity,
        val whoToLook: (Entity) -> Boolean = Look.TO_NEAR_PLAYER,
        val lookDistance: Float = 5.0f,
        val turnSpeed: Float = 0.0f,
        val lookAtSpectators: Boolean = false
    )

    companion object {
        /** Hard cap on look distance, in blocks. */
        const val MAX_LOOK_DISTANCE = 32.0

        val lookableEntities = arrayListOf<Data>()

        /** Per-observer state: which target it currently faces and the look settings it follows. */
        private data class Turn(val target: UUID, val data: Data)

        private val turns = HashMap<UUID, Turn>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<EntityMoveEvent> {
                        trySetLook(it.entity)
                    }

                    event<PlayerMoveEvent> {
                        trySetLook(it.player)
                    }

                    EffectiveSpigot.instance.launch {
                        while (true) {
                            tickTurns()
                            delay(1.ticks)
                        }
                    }
                }
            }
        }

        /**
         * Registers look behaviour for entities like [entity].
         * @param turnSpeed degrees per tick the entity turns toward its target; `0` snaps instantly.
         * @param lookAtSpectators whether players in spectator mode are looked at; `false` ignores them like vanilla mobs.
         * @throws IllegalArgumentException if [lookDistance] exceeds [MAX_LOOK_DISTANCE]
         */
        @JvmOverloads
        fun doEntityLookable(
            entity: Entity,
            whoToLook: (Entity) -> Boolean = Look.TO_NEAR_PLAYER,
            lookDistance: Float = 5.0f,
            turnSpeed: Float = 0.0f,
            lookAtSpectators: Boolean = false
        ) {
            if (lookDistance > MAX_LOOK_DISTANCE) {
                throw IllegalArgumentException(Locale.getMessage("errors.entities.look_distance_exceeded"))
            }

            lookableEntities.add(
                Data(
                    entity,
                    whoToLook,
                    lookDistance,
                    turnSpeed,
                    lookAtSpectators
                )
            )
        }

        /** Whether [target] may be looked at under [data]: spectators are skipped unless [Data.lookAtSpectators]. */
        private fun canLookAt(data: Data, target: Entity): Boolean =
            data.lookAtSpectators || (target as? Player)?.gameMode != GameMode.SPECTATOR

        /**
         * The point an entity looks from / is looked at: its eye position. Uses the real, scaled eye height
         * ([LivingEntity.getEyeHeight] tracks the `SCALE` attribute and pose), so a one-block target is faced at
         * its actual height instead of a fixed two-block assumption.
         */
        private fun eyePoint(entity: Entity): Vector {
            val base = entity.location.toVector()
            val eyeHeight = if (entity is LivingEntity) entity.eyeHeight else entity.height / 2.0

            return base.setY(base.y + eyeHeight)
        }

        /** Shortest-path step of [current] toward [target] yaw by at most [maxStep] degrees; `maxStep <= 0` snaps. */
        private fun stepYaw(current: Float, target: Float, maxStep: Float): Float {
            if (maxStep <= 0f) return target

            var delta = (target - current) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f

            return if (abs(delta) <= maxStep) target else current + sign(delta) * maxStep
        }

        /** Step of [current] toward [target] pitch by at most [maxStep] degrees; `maxStep <= 0` snaps. */
        private fun stepPitch(current: Float, target: Float, maxStep: Float): Float {
            if (maxStep <= 0f) return target

            val delta = target - current
            return if (abs(delta) <= maxStep) target else current + sign(delta) * maxStep
        }

        private fun tickTurns() {
            if (turns.isEmpty()) return

            val iterator = turns.entries.iterator()
            while (iterator.hasNext()) {
                val (observerId, turn) = iterator.next()
                val observer = Bukkit.getEntity(observerId)
                val target = Bukkit.getEntity(turn.target)

                if (observer == null || target == null || observer.world != target.world ||
                    observer.location.distance(target.location) > turn.data.lookDistance ||
                    !canLookAt(turn.data, target)
                ) {
                    iterator.remove()
                    continue
                }

                val direction = eyePoint(target).subtract(eyePoint(observer))
                if (direction.lengthSquared() < 1.0E-6) continue

                val desired = observer.location.clone().setDirection(direction)
                val current = observer.location
                val newYaw = stepYaw(current.yaw, desired.yaw, turn.data.turnSpeed)
                val newPitch = stepPitch(current.pitch, desired.pitch, turn.data.turnSpeed)

                if (newYaw == current.yaw && newPitch == current.pitch) continue

                observer.teleport(current.clone().apply {
                    yaw = newYaw
                    pitch = newPitch
                })
            }
        }

        private fun trySetLook(entity: Entity) {
            if (lookableEntities.isEmpty()) return

            entity.getNearbyEntities(
                MAX_LOOK_DISTANCE,
                MAX_LOOK_DISTANCE,
                MAX_LOOK_DISTANCE
            ).forEach { observer ->
                for (data in lookableEntities) {
                    if (
                        EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(data.entity, observer) &&
                        entity.location.distance(observer.location) <= data.lookDistance &&
                        canLookAt(data, entity) &&
                        data.whoToLook(entity)
                    ) {
                        turns[observer.uniqueId] = Turn(entity.uniqueId, data)
                    }
                }
            }
        }
    }
}
