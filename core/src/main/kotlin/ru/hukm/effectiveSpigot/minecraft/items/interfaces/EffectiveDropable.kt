package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule

interface EffectiveDropable {
    data class Data(
        val item: ItemStack,
        val chance: (Player?) -> Double,
        val lootTables: List<LootTables>? = arrayListOf(),
        val blocks: List<Material>? = arrayListOf(),
        val entities: List<EntityType>? = arrayListOf(),
        val amount: ((Player?) -> IntRange)? = null
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

        fun addLoot(data: Data) {
            foundableItems.add(data)
        }

        fun getLuckChance(item: ItemStack?): Int {
            return item?.getEnchantmentLevel(Enchantment.FORTUNE)?.takeIf { it > 0 }
                ?: item?.getEnchantmentLevel(Enchantment.LOOTING) ?: 0
        }

        fun chanceDependencyLuck(baseChance: Double, modifier: Double): (Player?) -> Double {
            return { player ->
                val item = player?.inventory?.itemInMainHand
                baseChance + (getLuckChance(item) * modifier)
            }
        }

        fun amountDependencyLuck(baseRange: IntRange, modifier: Int): (Player?) -> IntRange {
            return { player ->
                val item = player?.inventory?.itemInMainHand
                baseRange.first..(baseRange.last + (getLuckChance(item) * modifier))
            }
        }
    }

    private class Events : Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        fun onLootGenerate(event: LootGenerateEvent) {
            val lootTable = event.lootTable
            val loot = event.loot
            val entity = event.entity

            for (data in foundableItems) {
                if (data.lootTables != null && data.lootTables.map { it.lootTable } .contains(lootTable)) {
                    val player = entity as? Player

                    if (Math.random() <= data.chance.invoke(player)) {
                        val item = data.item.clone()
                        data.amount?.let {
                            item.amount = it.invoke(player).random()
                        }

                        loot.add(item)
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onBlockBreak(event: BlockBreakEvent) =
            dropFromBlock(event.block, event.player)

        @EventHandler(priority = EventPriority.MONITOR)
        fun onBlockDestroy(event: BlockDestroyEvent) =
            dropFromBlock(event.block, null)

        private fun dropFromBlock(block: org.bukkit.block.Block, player: Player?) {
            for (data in foundableItems) {
                if (data.blocks != null && data.blocks.contains(block.type)) {
                    if (Math.random() <= data.chance.invoke(player)) {
                        val item = data.item.clone()
                        data.amount?.let {
                            println(it.invoke(player).random())
                            item.amount = it.invoke(player).random()
                        }
                        block.world.dropItemNaturally(block.location.add(0.5, 0.5, 0.5), item)
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onEntityDeath(event: EntityDeathEvent) {
            val entity = event.entity
            val player = entity.killer ?: return

            for (data in foundableItems) {
                if (data.entities != null && data.entities.contains(entity.type)) {
                    if (Math.random() <= data.chance.invoke(player)) {
                        val item = data.item.clone()
                        data.amount?.let {
                            item.amount = it.invoke(player).random()
                        }

                        event.drops.add(item)
                    }
                }
            }
        }
    }
}
