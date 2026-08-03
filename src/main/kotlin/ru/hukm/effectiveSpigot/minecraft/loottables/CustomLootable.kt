package ru.hukm.effectiveSpigot.minecraft.loottables

import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.Locale

/** Simple weighted loot tables: lists of items each with an independent spawn chance. */
object CustomLootable {
    /** One loot entry: an [item] and its independent [chanceSpawn] in `0.0..1.0`. */
    data class ItemCellData(
        val item: ItemStack,
        val chanceSpawn: Double
    )

    private val customLootTables: ArrayList<ArrayList<ItemCellData>> by lazy {
        arrayListOf()
    }

    /** Registers a loot table for later reference. */
    fun create(lootTable: ArrayList<ItemCellData>) {
        customLootTables.add(lootTable)
    }

    /**
     * Rolls [lootTable] and places the winning items into random free slots of [container].
     * @throws IllegalStateException if there aren't enough free slots
     */
    fun putLootToContainer(container: Container, lootTable: ArrayList<ItemCellData>) {
        val inventory = container.inventory
        val inventorySize = inventory.size

        val freeSlots = (0 until inventorySize).filter { inventory.getItem(it) == null }.toMutableList()
        freeSlots.shuffle()

        var slotIndex = 0
        for (cell in lootTable) {
            if (Math.random() > cell.chanceSpawn) continue

            if (slotIndex >= freeSlots.size) {
                throw IllegalStateException(Locale.getMessage("errors.loot.container_full"))
            }

            inventory.setItem(freeSlots[slotIndex], cell.item)
            slotIndex++
        }
    }

    /** Rolls [lootTable] and drops the winning items naturally at [location]. */
    fun spawnLootAtLocation(location: Location, lootTable: ArrayList<ItemCellData>) {
        val world = location.world ?: return
        for (cell in lootTable) {
            if (Math.random() > cell.chanceSpawn) continue
            world.dropItemNaturally(location, cell.item)
        }
    }
}