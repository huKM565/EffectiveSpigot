package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.events.event

/**
 * Mixin for an [EffectiveEntity] type whose entities stay exactly where they were spawned.
 *
 * Implement it on the entity `object` next to [EffectiveEntity]:
 * ```kotlin
 * object PriestEntity : EffectiveEntity(), EffectiveEntityImmovable { … }
 * ```
 *
 * Gravity is turned off and every position change is vetoed — [EntityMoveEvent], knockback and attack
 * pushes, portal travel, water and piston pushes, boarding a vehicle. Plugin `teleport()` is still
 * allowed. Flags are applied on creation and re-applied whenever the entity is (re)added to the world,
 * so they survive chunk unloads and restarts. Pair with [EffectiveEntityInvulnerable] for an entity
 * that also takes no damage.
 */
interface EffectiveEntityImmovable {
    companion object {
        private fun matches(entity: Entity): Boolean {
            val key = EffectiveEntity.getNamespacedKeyByEntity(entity) ?: return false
            return EffectiveEntity.namespacedKeyToEffectiveEntity[key] is EffectiveEntityImmovable
        }

        internal fun apply(entity: Entity) {
            entity.setGravity(false)
            entity.velocity = entity.velocity.zero()
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<EntityAddToWorldEvent> {
                        if (matches(it.entity)) apply(it.entity)
                    }

                    event<EntityMoveEvent> {
                        if (it.hasChangedPosition() && matches(it.entity)) it.isCancelled = true
                    }

                    event<EntityKnockbackEvent> {
                        if (matches(it.entity)) it.isCancelled = true
                    }

                    event<EntityPortalEvent> {
                        if (matches(it.entity)) it.isCancelled = true
                    }

                    event<VehicleEnterEvent> {
                        if (matches(it.entered)) it.isCancelled = true
                    }
                }
            }
        }
    }
}
