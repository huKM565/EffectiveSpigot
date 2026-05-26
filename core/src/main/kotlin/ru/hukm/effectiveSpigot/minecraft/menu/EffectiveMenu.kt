package ru.hukm.effectiveSpigot.minecraft.menu

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import kotlin.collections.set

abstract class EffectiveMenu {
    data class ClickData(
        val click: EffectiveAbstractInteract.Click,
        val callback: (Player) -> Unit
    )

    data class SlotData(
        val item: ItemStack,
        val clickHandlers: List<ClickData>
    )

    private val maxSlotIndex = getItemsWithPattern().keys.maxOfOrNull { it } ?: -1

    val countSlot: Int = getSlotsCount() ?: (POSSIBLE_COUNT_SLOTS.find { it >= maxSlotIndex + 1 } ?: 54)

    private val inventoryHolder = object : InventoryHolder {
        override fun getInventory(): Inventory {
            val inventory = Bukkit.createInventory(this, countSlot, getMenuTitle())

            for ((slotIndex, itemData) in getItemsWithPattern()) {
                inventory.setItem(slotIndex, itemData.item)
            }

            return inventory
        }
    }

    companion object {
        val POSSIBLE_COUNT_SLOTS = intArrayOf(9, 18, 27, 36, 45, 54)

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        val namespacedNameToMenu = hashMapOf<String, EffectiveMenu>()
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedNameToMenu.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.menu.already_registered", namespacedName))
        }

        if (maxSlotIndex > 53) {
            //TODO()
            throw IllegalArgumentException(Locale.getMessage("errors.menu.already_registered", namespacedName))
        }

        namespacedNameToMenu[namespacedName] = this
    }

    fun getMenu(): Inventory {
        return inventoryHolder.inventory
    }

    abstract fun getMenuTitle(): String
    abstract fun getPattern(): List<String>?
    abstract fun getSymbolsToItems(): Map<Char, SlotData>
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    abstract fun getFreeSlotSymbol(): Char?
    abstract fun getSlotsCount(): Int?
    abstract fun onSlotChanged(player: Player, slot: Int, item: ItemStack?, wasPlaced: Boolean)

    fun getFreeSlots(): List<Int>? {
        val symbol = getFreeSlotSymbol() ?: return null
        val pattern = getPattern() ?: return null
        return pattern.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == symbol) rowIndex * 9 + colIndex else null
            }
        }.takeIf { it.isNotEmpty() }
    }

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
    }

    fun getItemsWithPattern(): Map<Int, SlotData> {
        val items = mutableMapOf<Int, SlotData>()

        getPattern()?.let { pattern ->
            val patternItems = getSymbolsToItems()
            pattern.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { colIndex, char ->
                    val slot = rowIndex * 9 + colIndex
                    patternItems[char]?.let { items[slot] = it }
                }
            }
        }

        return items
    }

    class Events : Listener {
        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            val inventory = event.inventory
            val inventoryHolder = inventory.holder
            val effectiveMenu = namespacedNameToMenu.values.find { inventoryHolder == it.inventoryHolder }
            if (effectiveMenu == null) return

            val rawSlot = event.rawSlot

            if (rawSlot >= effectiveMenu.countSlot || rawSlot < -99) {
                if (event.isShiftClick) {
                    val freeSlots = effectiveMenu.getFreeSlots()
                    if (freeSlots.isNullOrEmpty()) {
                        event.isCancelled = true
                    } else {
                        val snapshot = freeSlots.associateWith { inventory.getItem(it)?.clone() }
                        Bukkit.getScheduler().runTaskLater(EffectiveSpigot.instance, Runnable {
                            freeSlots.forEach { slot ->
                                val oldItem = snapshot[slot]?.takeIf { it.type != Material.AIR }
                                val newItem = inventory.getItem(slot)?.takeIf { it.type != Material.AIR }
                                if (oldItem == null && newItem != null) {
                                    effectiveMenu.onSlotChanged(event.whoClicked as Player, slot, newItem, true)
                                }
                            }
                        }, 1L)
                    }
                }
                return
            }

            val slot = event.slot

            if (effectiveMenu.getFreeSlots()?.contains(rawSlot) == true) {
                val player = event.whoClicked as Player
                val oldItem = event.currentItem?.takeIf { it.type != Material.AIR }
                val cursorItem = event.cursor.takeIf { it.type != Material.AIR }

                when {
                    oldItem != null && cursorItem != null -> {
                        effectiveMenu.onSlotChanged(player, slot, oldItem, false)
                        effectiveMenu.onSlotChanged(player, slot, cursorItem, true)
                    }
                    cursorItem != null -> {
                        val placed = if (event.isRightClick) cursorItem.clone().apply { amount = 1 } else cursorItem
                        effectiveMenu.onSlotChanged(player, slot, placed, true)
                    }
                    oldItem != null -> effectiveMenu.onSlotChanged(player, slot, null, false)
                }
                return
            }

            if (event.isShiftClick) {
                event.isCancelled = true
                return
            }

            event.isCancelled = true

            var click: EffectiveAbstractInteract.Click? = null
            if (event.isLeftClick) click = EffectiveAbstractInteract.Click.LEFT
            else if (event.isRightClick) click = EffectiveAbstractInteract.Click.RIGHT
            if (click == null) return

            effectiveMenu.getItemsWithPattern()[slot]?.clickHandlers?.forEach {
                if (it.click == click) it.callback.invoke(event.whoClicked as Player)
            }
        }

        @EventHandler
        fun onInventoryClose(event: InventoryCloseEvent) {
            val inventory = event.inventory
            val holder = inventory.holder ?: return
            val effectiveMenu = namespacedNameToMenu.values.find { holder == it.inventoryHolder } ?: return
            val player = event.player as? Player ?: return

            effectiveMenu.getFreeSlots()?.forEach { slot ->
                val item = inventory.getItem(slot)?.takeIf { it.type != Material.AIR } ?: return@forEach
                inventory.setItem(slot, null)
                val leftover = player.inventory.addItem(item)
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            }
        }

        @EventHandler
        fun onInventoryDrag(event: InventoryDragEvent) {
            val inventory = event.inventory
            val holder = inventory.holder ?: return
            val effectiveMenu = namespacedNameToMenu.values.find { holder == it.inventoryHolder } ?: return

            val menuSlots = event.rawSlots.filter { it < effectiveMenu.countSlot }
            if (menuSlots.isEmpty()) return

            if (menuSlots.any { effectiveMenu.getFreeSlots()?.contains(it) == false }) {
                event.isCancelled = true
                return
            }

            Bukkit.getScheduler().runTaskLater(EffectiveSpigot.instance, Runnable {
                menuSlots.forEach { slot ->
                    val item = inventory.getItem(slot)?.takeIf { it.type != Material.AIR }
                    if (item != null) {
                        effectiveMenu.onSlotChanged(event.whoClicked as Player, slot, item, true)
                    }
                }
            }, 1L)
        }
    }
}
