package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.items.SummoningEggItem

/**
 * An [EffectiveEntity] that also gets a matching spawn-egg item.
 *
 * The framework auto-creates a [SummoningEggItem] (namespaced `<id>_spawn_egg`) which spawns this
 * entity where used. Override [getSpawnEggMaterial], [getSpawnPlacement] or [editSpawnEggMeta] to
 * customize the egg; hand it out with [getSpawnEggItem].
 */
abstract class EffectiveEntityWithSpawnEgg : EffectiveEntity() {

    /** The spawn-egg item bound to this entity type. */
    val spawnEgg: SummoningEggItem = createSpawnEgg()

    private fun createSpawnEgg(): SummoningEggItem {
        val self = this
        return object : SummoningEggItem() {
            override fun getSpawnEffectiveEntity(): EffectiveEntity = self

            override fun getSpawnPlacement(): SpawnPlacement = self.getSpawnPlacement()

            override fun getMaterial(): Material = self.getSpawnEggMaterial()

            override fun getNamespacedData(): Pair<JavaPlugin, String> {
                val (plugin, name) = self.getNamespacedData()
                return plugin to "${name}_spawn_egg"
            }

            override fun editMeta(meta: ItemMeta) = self.editSpawnEggMeta(meta)
        }
    }

    /** Material used for the spawn egg (default: pig spawn egg). */
    open fun getSpawnEggMaterial(): Material = Material.PIG_SPAWN_EGG

    /** How the entity is placed when the egg is used (default: vanilla placement). */
    open fun getSpawnPlacement(): SummoningEggItem.SpawnPlacement =
        SummoningEggItem.SpawnPlacement.VANILLA

    /** Customizes the spawn egg's meta (name, lore, model). */
    open fun editSpawnEggMeta(meta: ItemMeta) {
        meta.displayName(Locale.getComponent("items.summoning_egg.name", getNamespacedKey()))
    }

    /** A single spawn-egg stack for this entity. */
    fun getSpawnEggItem(): ItemStack = spawnEgg.createItemStack()

    /** A spawn-egg stack of [amount] for this entity. */
    fun getSpawnEggItem(amount: Int): ItemStack = spawnEgg.createItemStack(amount)
}
