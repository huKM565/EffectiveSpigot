package ru.hukm.effectiveSpigot.minecraft.menu

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
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
                    event<InventoryClickEvent> {
                        val inventory = it.inventory
                        val inventoryHolder = inventory.holder
                        val effectiveMenu = namespacedNameToMenu.values.find { menu -> inventoryHolder == menu.inventoryHolder }
                            ?: return@event

                        val rawSlot = it.rawSlot
                        val player = it.whoClicked as Player

                        if (rawSlot >= effectiveMenu.countSlot || rawSlot < -99) {
                            if (it.isShiftClick) {
                                val freeSlots = effectiveMenu.getFreeSlots()
                                if (freeSlots.isNullOrEmpty()) {
                                    it.isCancelled = true
                                } else {
                                    val snapshot = freeSlots.associateWith { slot -> inventory.getItem(slot)?.clone() }
                                    EffectiveSpigot.instance.launch {
                                        delay(1.ticks)
                                        freeSlots.forEach { slot ->
                                            val oldItem = snapshot[slot]?.takeIf { item -> item.type != Material.AIR }
                                            val newItem = inventory.getItem(slot)?.takeIf { item -> item.type != Material.AIR }
                                            if (oldItem == null && newItem != null) {
                                                effectiveMenu.onSlotChanged(player, slot, newItem, true)
                                            }
                                        }
                                    }
                                }
                            }
                            return@event
                        }

                        val slot = it.slot

                        if (effectiveMenu.getFreeSlots()?.contains(rawSlot) == true) {
                            val oldItem = it.currentItem?.takeIf { item -> item.type != Material.AIR }
                            val cursorItem = it.cursor.takeIf { item -> item.type != Material.AIR }

                            when {
                                oldItem != null && cursorItem != null -> {
                                    effectiveMenu.onSlotChanged(player, slot, oldItem, false)
                                    effectiveMenu.onSlotChanged(player, slot, cursorItem, true)
                                }
                                cursorItem != null -> {
                                    val placed = if (it.isRightClick) cursorItem.clone().apply { amount = 1 } else cursorItem
                                    effectiveMenu.onSlotChanged(player, slot, placed, true)
                                }
                                oldItem != null -> effectiveMenu.onSlotChanged(player, slot, null, false)
                            }
                            return@event
                        }

                        if (it.isShiftClick) {
                            it.isCancelled = true
                            return@event
                        }

                        it.isCancelled = true

                        var click: EffectiveAbstractInteract.Click? = null
                        if (it.isLeftClick) click = EffectiveAbstractInteract.Click.LEFT
                        else if (it.isRightClick) click = EffectiveAbstractInteract.Click.RIGHT
                        if (click == null) return@event

                        effectiveMenu.getItemsWithPattern()[slot]?.clickHandlers?.forEach { data ->
                            if (data.click == click) data.callback.invoke(player)
                        }
                    }

                    event<InventoryCloseEvent> {
                        val inventory = it.inventory
                        val holder = inventory.holder ?: return@event
                        val effectiveMenu = namespacedNameToMenu.values.find { menu -> holder == menu.inventoryHolder }
                            ?: return@event
                        val player = it.player as? Player ?: return@event

                        effectiveMenu.getFreeSlots()?.forEach { slot ->
                            val item = inventory.getItem(slot)?.takeIf { item -> item.type != Material.AIR } ?: return@forEach
                            inventory.setItem(slot, null)
                            val leftover = player.inventory.addItem(item)
                            leftover.values.forEach { left -> player.world.dropItemNaturally(player.location, left) }
                        }
                    }

                    event<InventoryDragEvent> {
                        val inventory = it.inventory
                        val holder = inventory.holder ?: return@event
                        val effectiveMenu = namespacedNameToMenu.values.find { menu -> holder == menu.inventoryHolder }
                            ?: return@event
                        val player = it.whoClicked as Player

                        val menuSlots = it.rawSlots.filter { slot -> slot < effectiveMenu.countSlot }
                        if (menuSlots.isEmpty()) return@event

                        if (menuSlots.any { slot -> effectiveMenu.getFreeSlots()?.contains(slot) == false }) {
                            it.isCancelled = true
                            return@event
                        }

                        EffectiveSpigot.instance.launch {
                            delay(1.ticks)
                            menuSlots.forEach { slot ->
                                val item = inventory.getItem(slot)?.takeIf { item -> item.type != Material.AIR }
                                if (item != null) {
                                    effectiveMenu.onSlotChanged(player, slot, item, true)
                                }
                            }
                        }
                    }
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

    fun getViewers(): List<Player> {
        return Bukkit.getOnlinePlayers().filter { it.openInventory.topInventory.holder === inventoryHolder }
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
}