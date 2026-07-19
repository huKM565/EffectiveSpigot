package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.items.SummoningEggItem

abstract class EffectiveEntityWithSpawnEgg : EffectiveEntity() {

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

    open fun getSpawnEggMaterial(): Material = Material.PIG_SPAWN_EGG

    open fun getSpawnPlacement(): SummoningEggItem.SpawnPlacement =
        SummoningEggItem.SpawnPlacement.VANILLA

    open fun editSpawnEggMeta(meta: ItemMeta) {
        meta.displayName(Locale.getComponent("items.summoning_egg.name", getNamespacedKey()))
    }

    fun getSpawnEggItem(): ItemStack = spawnEgg.createItemStack()

    fun getSpawnEggItem(amount: Int): ItemStack = spawnEgg.createItemStack(amount)
}
