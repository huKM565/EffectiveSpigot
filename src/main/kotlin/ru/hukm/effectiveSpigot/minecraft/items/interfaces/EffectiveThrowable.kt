package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Sound
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

/**
 * Behaviour that lets an [EffectiveItem] be thrown as a snowball-backed projectile.
 *
 * Registration adds a right-click handler that launches the projectile carrying the item; a shared
 * module listens for [ProjectileHitEvent] and dispatches to the matching item's `onHit`. Register
 * via [EffectiveItem.makeThrowable].
 *
 * Notes:
 * - The projectile is always a [Snowball]; the thrown item is stored **inside** it and used to
 *   identify it on landing, so [EffectiveItem.createItemStack] must yield the item's namespaced key
 *   (the default does).
 * - `onHit` is already filtered: it fires only for this item's projectiles, never for vanilla
 *   snowballs or other throwables.
 *
 * ```kotlin
 * fun init() {
 *     makeThrowable(velocity = 1.5) { event ->
 *         val target = event.hitEntity as? LivingEntity ?: return@makeThrowable
 *         target.addPotionEffect(PotionEffect(PotionEffectType.POISON, 40, 0))
 *     }
 * }
 * ```
 */
interface EffectiveThrowable {
    /**
     * Throw configuration for one item.
     *
     * @property item the item made throwable
     * @property velocity speed multiplier along the thrower's look direction
     * @property consumeOnThrow whether one is removed from the stack per throw
     * @property throwSound sound played on throw, or null for silence
     * @property onHit callback invoked with the projectile's [ProjectileHitEvent]
     */
    data class Data(
        val item: EffectiveItem,
        val velocity: Double = 1.5,
        val consumeOnThrow: Boolean = true,
        val throwSound: Sound? = Sound.ENTITY_SNOWBALL_THROW,
        val onHit: (ProjectileHitEvent) -> Unit,
    )

    companion object {
        private val throwableItems = arrayListOf<Data>()

        /** Registers throw behaviour from a prebuilt [Data]. No-op if the item is already throwable. */
        fun makeThrowable(data: Data) {
            if (throwableItems.any { it.item.equalByNamespacedKey(data.item) }) return
            throwableItems.add(data)

            data.item.addClickHandler(EffectiveAbstractInteract.Click.RIGHT, { e ->
                val player = e.player

                if (data.consumeOnThrow) e.item.amount -= 1

                data.throwSound?.let { player.world.playSound(player.location, it, 1.0f, 1.0f) }
                player.swingMainHand()

                val snowball = player.launchProjectile(Snowball::class.java)
                snowball.velocity = player.eyeLocation.direction.multiply(data.velocity)
                snowball.item = data.item.createItemStack()

                EffectiveAbstractInteract.Result.CANCEL_EVENT
            })
        }

        /** Convenience overload that builds the [Data] from individual arguments. */
        fun makeThrowable(
            item: EffectiveItem,
            velocity: Double = 1.5,
            consumeOnThrow: Boolean = true,
            throwSound: Sound? = Sound.ENTITY_SNOWBALL_THROW,
            onHit: (ProjectileHitEvent) -> Unit,
        ) = makeThrowable(Data(item, velocity, consumeOnThrow, throwSound, onHit))

        /** Whether [item] belongs to a registered throwable item. */
        fun isThrowable(item: ItemStack?): Boolean {
            val key = EffectiveItem.getNamespacedKeyByItem(item) ?: return false
            return throwableItems.any { it.item.getNamespacedName() == key }
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<ProjectileHitEvent> {
                        val projectile = it.entity as? Snowball ?: return@event
                        val key = EffectiveItem.getNamespacedKeyByItem(projectile.item) ?: return@event
                        val data = throwableItems.firstOrNull { d -> d.item.getNamespacedName() == key } ?: return@event

                        data.onHit(it)
                    }
                }
            }
        }
    }
}
