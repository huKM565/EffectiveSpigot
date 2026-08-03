package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

/**
 * Behaviour that turns an [EffectiveItem]'s durability bar into a limited-use counter.
 *
 * A registered item is forced to `maxStackSize = 1` and gets `maxDamage = maxUses`; every use is
 * charged with [consumeUse], and [getUsesLeft] reports the remaining charges. The bar is applied
 * automatically whenever the item is created.
 *
 * Trade-off vs. a PDC counter: durability is free to render but can be reset by an anvil/grindstone
 * and prevents stacking. Prefer this only when repair stations are unavailable. Register via
 * [EffectiveItem.makeDurable].
 */
interface EffectiveDurability {
    companion object {
        private val durableItems = hashMapOf<String, Int>()

        /**
         * Registers [effectiveItem] as durable with [maxUses] charges.
         *
         * The bar is applied by [apply] at creation time, so this must run **before** the first
         * [EffectiveItem.createItemStack] — otherwise already-built stacks have no durability. Call
         * it from the item's `init()` in `onEnable`:
         * ```kotlin
         * fun init() {
         *     makeDurable(3) // via EffectiveItem, or EffectiveDurability.makeDurable(this, 3)
         * }
         * ```
         */
        fun makeDurable(effectiveItem: EffectiveItem, maxUses: Int) {
            durableItems[effectiveItem.getNamespacedName()] = maxUses
        }

        /** Whether [item] belongs to a registered durable item. */
        fun isDurable(item: ItemStack?): Boolean {
            val key = EffectiveItem.getNamespacedKeyByItem(item) ?: return false
            return durableItems.containsKey(key)
        }

        internal fun apply(effectiveItem: EffectiveItem, item: ItemStack) {
            val maxUses = durableItems[effectiveItem.getNamespacedName()] ?: return
            val meta = item.itemMeta ?: return

            meta.setMaxStackSize(1)
            if (meta is Damageable) {
                meta.setMaxDamage(maxUses)
            }

            item.itemMeta = meta
        }

        /** Remaining charges of [item], or null if it has no durability data. */
        fun getUsesLeft(item: ItemStack?): Int? {
            val meta = item?.itemMeta as? Damageable ?: return null
            val maxDamage = meta.maxDamage ?: return null
            return maxDamage - meta.damage
        }

        /**
         * Spends one charge of [item], increasing its damage by one.
         *
         * ⚠️ Pass the **actual stack from the player's inventory** (e.g. the one in hand), never a
         * `createItemStack()` copy — a fresh stack has `damage = 0`, so consuming it does nothing and
         * silently resets the counter. When passed an inventory stack, the mutation is written back
         * to that stack.
         *
         * On the final charge the stack is emptied (`amount = 0`) so it disappears.
         *
         * ```kotlin
         * val bag = player.inventory.itemInMainHand
         * if (EffectiveDurability.consumeUse(bag)) {
         *     // charges left — bag still usable
         * } else {
         *     // was the last charge — bag consumed
         * }
         * ```
         *
         * @return true if charges remain, false if this was the last one (the stack is emptied).
         */
        fun consumeUse(item: ItemStack): Boolean {
            val meta = item.itemMeta as? Damageable ?: return false
            val maxDamage = meta.maxDamage ?: return false

            val next = meta.damage + 1
            if (next >= maxDamage) {
                item.amount = 0
                return false
            }

            meta.damage = next
            item.itemMeta = meta
            return true
        }
    }
}
