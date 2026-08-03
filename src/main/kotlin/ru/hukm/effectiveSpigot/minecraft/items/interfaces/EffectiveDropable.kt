package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event

/**
 * Behaviour that makes a custom item drop from loot tables, broken blocks or killed entities.
 *
 * Usually reached via [EffectiveItem.addToLoot]. Chance and amount are computed per (nullable)
 * player, so drops can scale with Fortune/Looting via the helper functions here.
 */
interface EffectiveDropable {
    /**
     * One drop rule.
     *
     * @property item the stack template to drop (cloned per drop)
     * @property chance drop probability in `0.0..1.0`, evaluated per player
     * @property lootTables vanilla loot tables this item is injected into
     * @property blocks blocks that may drop this item
     * @property entities entities that may drop this item
     * @property amount optional per-drop stack-size range
     */
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
                    event<LootGenerateEvent>(EventPriority.MONITOR) {
                        val lootTable = it.lootTable
                        val loot = it.loot
                        val entity = it.entity

                        for (data in foundableItems) {
                            if (data.lootTables != null && data.lootTables.map { tables -> tables.lootTable }.contains(lootTable)) {
                                val player = entity as? Player

                                if (Math.random() <= data.chance.invoke(player)) {
                                    val item = data.item.clone()
                                    data.amount?.let { amount ->
                                        item.amount = amount.invoke(player).random()
                                    }

                                    loot.add(item)
                                }
                            }
                        }
                    }

                    event<BlockBreakEvent>(EventPriority.MONITOR) {
                        dropFromBlock(it.block, it.player)
                    }

                    event<BlockDestroyEvent>(EventPriority.MONITOR) {
                        dropFromBlock(it.block, null)
                    }

                    event<EntityExplodeEvent>(EventPriority.MONITOR) {
                        it.blockList().forEach { block -> dropFromBlock(block, null) }
                    }

                    event<BlockExplodeEvent>(EventPriority.MONITOR) {
                        it.blockList().forEach { block -> dropFromBlock(block, null) }
                    }

                    event<EntityDeathEvent>(EventPriority.MONITOR) {
                        val entity = it.entity
                        val player = entity.killer ?: return@event

                        for (data in foundableItems) {
                            if (data.entities != null && data.entities.contains(entity.type)) {
                                if (Math.random() <= data.chance.invoke(player)) {
                                    val item = data.item.clone()
                                    data.amount?.let { amount ->
                                        item.amount = amount.invoke(player).random()
                                    }

                                    it.drops.add(item)
                                }
                            }
                        }
                    }
                }
            }
        }

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

        /** Registers a drop rule. */
        fun addLoot(data: Data) {
            foundableItems.add(data)
        }

        /** Fortune level of [item], or Looting if it has no Fortune; 0 if neither. */
        fun getLuckChance(item: ItemStack?): Int {
            return item?.getEnchantmentLevel(Enchantment.FORTUNE)?.takeIf { it > 0 }
                ?: item?.getEnchantmentLevel(Enchantment.LOOTING) ?: 0
        }

        /**
         * Chance function that adds `luck * modifier` to [baseChance], using the held tool's luck
         * ([getLuckChance]). Feed the result to [Data.chance]; keep values in `0.0..1.0`.
         *
         * ```kotlin
         * // 10% base, +5% per Fortune/Looting level
         * effectiveItem.addToLoot(
         *     dropChance = EffectiveDropable.chanceDependencyLuck(0.10, 0.05),
         *     lootTables = null,
         *     blocks = listOf(Material.DIAMOND_ORE),
         *     entities = null,
         * )
         * ```
         */
        fun chanceDependencyLuck(baseChance: Double, modifier: Double): (Player?) -> Double {
            return { player ->
                val item = player?.inventory?.itemInMainHand
                baseChance + (getLuckChance(item) * modifier)
            }
        }

        /**
         * Amount function that extends [baseRange]'s upper bound by `luck * modifier`. Feed the
         * result to [Data.amount].
         *
         * ```kotlin
         * // 1..2 base, +1 max per luck level → 1..(2 + luck)
         * amount = EffectiveDropable.amountDependencyLuck(1..2, 1)
         * ```
         */
        fun amountDependencyLuck(baseRange: IntRange, modifier: Int): (Player?) -> IntRange {
            return { player ->
                val item = player?.inventory?.itemInMainHand
                baseRange.first..(baseRange.last + (getLuckChance(item) * modifier))
            }
        }
    }
}