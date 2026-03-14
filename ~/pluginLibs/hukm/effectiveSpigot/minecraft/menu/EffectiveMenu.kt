package ru.hukm.effectiveSpigot.minecraft.menu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
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
            throw IllegalArgumentException(LanguageModule.getMessage("errors.menu.already_registered", namespacedName))
        }

        if (maxSlotIndex > 53) {
            //TODO()
            throw IllegalArgumentException(LanguageModule.getMessage("errors.menu.already_registered", namespacedName))
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

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
    }

    open fun getSlotsCount(): Int? = null

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
            if (event.isShiftClick) {
                event.isCancelled = true
            }

            if (rawSlot >= effectiveMenu.countSlot || rawSlot < -99) return
            event.isCancelled = true

            var click: EffectiveAbstractInteract.Click? = null;

            if (event.isLeftClick) click = EffectiveAbstractInteract.Click.LEFT
            else if (event.isRightClick) click = EffectiveAbstractInteract.Click.RIGHT

            if (click == null) return

            val slot = event.slot
            effectiveMenu.getItemsWithPattern()[slot]?.clickHandlers?.forEach {
                if (it.click == click) it.callback.invoke(event.whoClicked as Player)
            }
        }
    }
}
