package ru.hukm.effectiveSpigot.minecraft.entities.interfaces

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityDamageEvent
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.events.event

/**
 * Mixin for an [EffectiveEntity] type whose entities take no damage at all.
 *
 * Implement it on the entity `object` next to [EffectiveEntity]:
 * ```kotlin
 * object PriestEntity : EffectiveEntity(), EffectiveEntityInvulnerable { … }
 * ```
 *
 * Every [EntityDamageEvent] on the entity is cancelled and the entity is flagged invulnerable, so
 * nothing — attacks, fire, lava, explosions, void — hurts it. The flag is applied on creation and
 * re-applied whenever the entity is (re)added to the world, so it survives chunk unloads and restarts.
 * Pair with [EffectiveEntityImmovable] for an entity that also can't be moved.
 */
interface EffectiveEntityInvulnerable {
    companion object {
        private fun matches(entity: Entity): Boolean {
            val key = EffectiveEntity.getNamespacedKeyByEntity(entity) ?: return false
            return EffectiveEntity.namespacedKeyToEffectiveEntity[key] is EffectiveEntityInvulnerable
        }

        internal fun apply(entity: Entity) {
            entity.isInvulnerable = true
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<EntityAddToWorldEvent> {
                        if (matches(it.entity)) apply(it.entity)
                    }

                    event<EntityDamageEvent> {
                        if (matches(it.entity)) it.isCancelled = true
                    }
                }
            }
        }
    }
}
