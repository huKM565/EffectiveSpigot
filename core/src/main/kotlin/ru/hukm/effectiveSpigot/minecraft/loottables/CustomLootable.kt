package ru.hukm.effectiveSpigot.minecraft.loottables

import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack
import ru.hukm.effectiveSpigot.Locale

object CustomLootable {
    data class ItemCellData(
        val item: ItemStack,
        val chanceSpawn: Double
    )

    private val customLootTables: ArrayList<ArrayList<ItemCellData>> by lazy {
        arrayListOf()
    }

    fun create(lootTable: ArrayList<ItemCellData>) {
        customLootTables.add(lootTable)
    }

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

    fun spawnLootAtLocation(location: Location, lootTable: ArrayList<ItemCellData>) {
        val world = location.world ?: return
        for (cell in lootTable) {
            if (Math.random() > cell.chanceSpawn) continue
            world.dropItemNaturally(location, cell.item)
        }
    }
}