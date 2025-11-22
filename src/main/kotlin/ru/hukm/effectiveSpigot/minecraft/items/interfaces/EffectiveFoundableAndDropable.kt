package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule

interface EffectiveFoundableAndDropable {
    data class Data(
        val item: ItemStack,
        val chance: Double,
        val spawnLootTables: ArrayList<LootTables>,
        val minAmount: Int? = null,
        val maxAmount: Int? = null
    )

    companion object {
        private val foundableItems: ArrayList<Data> = arrayListOf()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun register(data: Data) {
            if (data.chance < 0.0 || data.chance > 1.0) {
                throw IllegalArgumentException("Chance must be between 0.0 and 1.0")
            }
            if ((data.minAmount != null && data.maxAmount == null) || (data.minAmount == null && data.maxAmount != null)) {
                throw IllegalArgumentException("Both minAmount and maxAmount must be set or both must be null")
            }
            foundableItems.add(data)
        }
    }

    private class Events() : Listener {
        @EventHandler
        fun onLootGenerate(event: LootGenerateEvent) {
            val lootTable = event.lootTable
            val loot = event.loot

            for (data in foundableItems) {
                if (data.spawnLootTables.map { it.lootTable } .contains(lootTable)) {
                    if (Math.random() <= data.chance) {
                        val item = data.item.clone()
                        if (data.minAmount != null && data.maxAmount != null) {
                            item.amount = (data.minAmount..data.maxAmount).random()
                        }

                        loot.add(item)
                    }
                }
            }
        }
    }
}