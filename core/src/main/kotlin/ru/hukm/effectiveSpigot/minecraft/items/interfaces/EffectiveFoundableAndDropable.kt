package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
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

        /**
         * Adds the item to the loot generation system.
         *
         * @param data Item drop data (chance, loot tables, amount).
         * @throws IllegalArgumentException If chance is out of range 0.0-1.0 or only one of minAmount/maxAmount is specified.
         */
        fun addLoot(data: Data) {
            if (data.chance < 0.0 || data.chance > 1.0) {
                throw IllegalArgumentException(LanguageModule.getMessage("errors.chance_out_of_range"))
            }
            if ((data.minAmount != null && data.maxAmount == null) || (data.minAmount == null && data.maxAmount != null)) {
                throw IllegalArgumentException(LanguageModule.getMessage("errors.amounts_both_required"))
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