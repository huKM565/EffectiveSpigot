package ru.hukm.effectiveSpigot.minecraft.menu

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
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
import kotlin.collections.set

/**
 * Base class for a chest-style GUI menu laid out with a character pattern.
 *
 * A subclass supplies a title, a row-based [getPattern] and a [getSymbolsToItems] mapping each pattern
 * character to an item plus its click handlers. Optionally, [getFreeSlotSymbol] marks slots the player
 * may place/take items in — changes are reported through [onSlotChanged]. Like the other bases, the menu
 * registers itself on construction; open it with [getMenu].
 *
 * ```kotlin
 * object ExampleMenu : EffectiveMenu() {
 *     override fun getMenuTitle() = "Example"
 *     override fun getPattern() = listOf(
 *         "         ",
 *         "    x    ",
 *         "         ",
 *     )
 *     override fun getSymbolsToItems() = mapOf(
 *         'x' to SlotData(ItemStack(Material.DIAMOND), listOf(
 *             ClickData(ClickType.LEFT) { player -> player.sendMessage("hi") }
 *         ))
 *     )
 *     override fun getNamespacedData() = ExamplePlugin.instance to "example"
 *     override fun getFreeSlotSymbol() = null
 *     override fun getSlotsCount() = 27
 *     override fun onSlotChanged(player: Player, slot: Int, item: ItemStack?, wasPlaced: Boolean) {}
 * }
 * // player.openInventory(ExampleMenu.getMenu())
 * ```
 *
 * A built-in `/emenu <menu>` command opens any registered menu in-game.
 */
abstract class EffectiveMenu {
    /**
     * A click handler for a slot: which [clicks] (Bukkit [ClickType]s, e.g. `LEFT`, `RIGHT`, `MIDDLE`,
     * `SHIFT_LEFT`) trigger it — matched exactly, any one of the set — and the action to run for the
     * clicking player. Register one type via `ClickData(ClickType.LEFT) { … }`, or several via
     * `ClickData(ClickType.LEFT, ClickType.SHIFT_LEFT) { … }`.
     */
    data class ClickData(
        val clicks: Set<ClickType>,
        val callback: (Player) -> Unit
    ) {
        constructor(click: ClickType, callback: (Player) -> Unit) : this(setOf(click), callback)
        constructor(vararg clicks: ClickType, callback: (Player) -> Unit) : this(clicks.toSet(), callback)
    }

    /** The item shown in a slot together with its click handlers. */
    data class SlotData(
        val item: ItemStack,
        val clickHandlers: List<ClickData>
    )

    private val maxSlotIndex = getItemsWithPattern().keys.maxOfOrNull { it } ?: -1

    /** Actual inventory size: [getSlotsCount] if set, else the smallest multiple of 9 that fits the pattern. */
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
        /** Valid chest inventory sizes (multiples of 9, up to a double chest). */
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

                        it.isCancelled = true

                        effectiveMenu.getItemsWithPattern()[slot]?.clickHandlers?.forEach { data ->
                            if (it.click in data.clicks) data.callback.invoke(player)
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

        private val _namespacedNameToMenu = hashMapOf<String, EffectiveMenu>()

        /** Read-only registry of all constructed menus, keyed by [getNamespacedName]. */
        val namespacedNameToMenu: Map<String, EffectiveMenu> get() = _namespacedNameToMenu
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

        _namespacedNameToMenu[namespacedName] = this
    }

    /** Builds a fresh inventory instance for this menu; pass to `player.openInventory(...)`. */
    fun getMenu(): Inventory {
        return inventoryHolder.inventory
    }

    /** Players who currently have this menu open. */
    fun getViewers(): List<Player> {
        return Bukkit.getOnlinePlayers().filter { it.openInventory.topInventory.holder === inventoryHolder }
    }

    /** Inventory title shown at the top. */
    abstract fun getMenuTitle(): String

    /** Row strings (9 chars each) mapping characters to items via [getSymbolsToItems]; null for empty. */
    abstract fun getPattern(): List<String>?

    /** Maps each pattern character to its [SlotData] (item + click handlers). */
    abstract fun getSymbolsToItems(): Map<Char, SlotData>

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Pattern character marking player-editable slots, or null if the menu is read-only. */
    abstract fun getFreeSlotSymbol(): Char?

    /** Explicit inventory size, or null to size automatically from the pattern. */
    abstract fun getSlotsCount(): Int?

    /**
     * Called when a free slot's contents change.
     * @param wasPlaced true if an item was put into the slot, false if taken out
     */
    abstract fun onSlotChanged(player: Player, slot: Int, item: ItemStack?, wasPlaced: Boolean)

    /** Slot indices marked editable by [getFreeSlotSymbol], or null if none. */
    fun getFreeSlots(): List<Int>? {
        val symbol = getFreeSlotSymbol() ?: return null
        val pattern = getPattern() ?: return null
        return pattern.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, char ->
                if (char == symbol) rowIndex * 9 + colIndex else null
            }
        }.takeIf { it.isNotEmpty() }
    }

    /** Unique identity as `"<plugin-name>:<id>"`, lowercased. */
    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
    }

    /** Resolves the pattern into a slot-index → [SlotData] map. */
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