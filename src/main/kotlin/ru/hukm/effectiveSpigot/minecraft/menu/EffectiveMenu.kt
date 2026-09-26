package ru.hukm.effectiveSpigot.minecraft.menu

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
 * ### Per-viewer layout
 * [getPattern], [getSymbolsToItems] and [getFreeSlots] all receive `whoOpen` — the player the menu is
 * being built/handled for (nullable: null means the default, no-viewer layout) — so the layout can
 * differ per viewer (e.g. slots unlocked by rank/purchase). A single menu object serves every player;
 * there is no shared mutable "current player" field, so this is race-free even when several players open
 * it at once. Ignore the parameter if your menu is static.
 *
 * ```kotlin
 * object ExampleMenu : EffectiveMenu() {
 *     override fun getMenuTitle() = "Example"
 *     override fun getPattern(whoOpen: Player?) = listOf(
 *         "         ",
 *         "    x    ",
 *         "         ",
 *     )
 *     override fun getSymbolsToItems(whoOpen: Player?) = mapOf(
 *         'x' to SlotData(ItemStack(Material.DIAMOND), listOf(
 *             ClickData(ClickType.LEFT) { player -> player.sendMessage("hi") }
 *         ))
 *     )
 *     override fun getNamespacedData() = ExamplePlugin.instance to "example"
 *     override fun getFreeSlotSymbol() = null
 *     override fun getSlotsCount() = 27
 *     override fun onSlotChanged(player: Player, slot: Int, item: ItemStack, wasPlaced: Boolean) = SlotChangeResult.ALLOW
 * }
 * // player.openInventory(ExampleMenu.getMenu(player))
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

    /**
     * What the framework does with the free-slot contents after [onClose] runs.
     *
     * - [RETURN_TO_PLAYER] — the framework moves every free-slot item into the player's inventory
     *   (dropping the overflow). This is the default "input tray" behaviour.
     * - [NO_RETURN] — the framework leaves the free slots untouched. **You are then responsible for
     *   those items**: the closed inventory is a throwaway snapshot rebuilt on every open, so anything
     *   you don't read out inside [onClose] is simply discarded. Use this for persistent containers
     *   that save their contents elsewhere (e.g. to an item's persistent data).
     */
    enum class CloseAction { RETURN_TO_PLAYER, NO_RETURN }

    /**
     * Whether a pending free-slot change is allowed. Returned from [onSlotChanged] before the change is
     * applied: [ALLOW] lets it happen, [CANCEL] vetoes it (the underlying Bukkit event is cancelled, so
     * the item stays where it was).
     */
    enum class SlotChangeResult { ALLOW, CANCEL }

    /**
     * The stable [InventoryHolder] shared by every inventory this menu opens. Read-only: it can't be
     * replaced, but it is exposed so external code can identify "is this open/closed inventory mine?"
     * via identity — `inventory.holder === someMenu.inventoryHolder` (every [getMenu] call builds a
     * fresh [Inventory] but reuses this same holder).
     *
     * Its own [InventoryHolder.getInventory] is only a fallback — it builds the menu with no viewer
     * (`getMenu(null)`), so any per-viewer layout falls back to its default; real opens go through
     * [getMenu] / [openWithFreeSlots] with the actual player.
     */
    val inventoryHolder: InventoryHolder = object : InventoryHolder {
        override fun getInventory(): Inventory = getMenu(null)
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

                        if (rawSlot >= inventory.size || rawSlot < -99) {
                            if (it.isShiftClick) {
                                val freeSlots = effectiveMenu.getFreeSlots(player)
                                if (freeSlots.isNullOrEmpty()) {
                                    it.isCancelled = true
                                } else {
                                    val incoming = it.currentItem?.takeIf { item -> item.type != Material.AIR }
                                    if (incoming != null &&
                                        effectiveMenu.onSlotChanged(player, -1, incoming, true) == SlotChangeResult.CANCEL
                                    ) {
                                        it.isCancelled = true
                                    }
                                }
                            }
                            return@event
                        }

                        val slot = it.slot

                        if (effectiveMenu.getFreeSlots(player)?.contains(rawSlot) == true) {
                            val oldItem = it.currentItem?.takeIf { item -> item.type != Material.AIR }
                            val cursorItem = it.cursor.takeIf { item -> item.type != Material.AIR }
                            val numberKeyItem = if (it.click == ClickType.NUMBER_KEY)
                                player.inventory.getItem(it.hotbarButton)?.takeIf { item -> item.type != Material.AIR }
                            else null

                            val incoming = cursorItem ?: numberKeyItem
                            if (incoming != null &&
                                effectiveMenu.onSlotChanged(player, slot, incoming, true) == SlotChangeResult.CANCEL
                            ) {
                                it.isCancelled = true
                                return@event
                            }
                            if (oldItem != null &&
                                effectiveMenu.onSlotChanged(player, slot, oldItem, false) == SlotChangeResult.CANCEL
                            ) {
                                it.isCancelled = true
                                return@event
                            }
                            return@event
                        }

                        it.isCancelled = true

                        effectiveMenu.getItemsWithPattern(player)[slot]?.clickHandlers?.forEach { data ->
                            if (it.click in data.clicks) data.callback.invoke(player)
                        }
                    }

                    event<InventoryCloseEvent> {
                        val inventory = it.inventory
                        val holder = inventory.holder ?: return@event
                        val effectiveMenu = namespacedNameToMenu.values.find { menu -> holder == menu.inventoryHolder }
                            ?: return@event
                        val player = it.player as? Player ?: return@event

                        if (effectiveMenu.onClose(player, inventory) == CloseAction.NO_RETURN) return@event

                        effectiveMenu.getFreeSlots(player)?.forEach { slot ->
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

                        val menuSlots = it.rawSlots.filter { slot -> slot < inventory.size }
                        if (menuSlots.isEmpty()) return@event

                        if (menuSlots.any { slot -> effectiveMenu.getFreeSlots(player)?.contains(slot) == false }) {
                            it.isCancelled = true
                            return@event
                        }

                        val incoming = it.oldCursor.takeIf { item -> item.type != Material.AIR }
                        if (incoming != null &&
                            effectiveMenu.onSlotChanged(player, -1, incoming, true) == SlotChangeResult.CANCEL
                        ) {
                            it.isCancelled = true
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

        _namespacedNameToMenu[namespacedName] = this
    }

    /** Inventory size for [whoOpen]: [getSlotsCount] if set, else the smallest multiple of 9 that fits the pattern. */
    private fun sizeFor(whoOpen: Player?): Int {
        val explicit = getSlotsCount()
        if (explicit != null) return explicit
        val maxSlotIndex = getItemsWithPattern(whoOpen).keys.maxOfOrNull { it } ?: -1
        return POSSIBLE_COUNT_SLOTS.find { it >= maxSlotIndex + 1 } ?: 54
    }

    /**
     * Builds a fresh inventory instance of this menu laid out for [whoOpen]; pass to `player.openInventory(...)`.
     * [whoOpen] may be null to build the default (no-viewer) layout — pass the real player for per-viewer menus.
     */
    fun getMenu(whoOpen: Player? = null): Inventory {
        val inventory = Bukkit.createInventory(inventoryHolder, sizeFor(whoOpen), getMenuTitle())
        for ((slotIndex, itemData) in getItemsWithPattern(whoOpen)) {
            inventory.setItem(slotIndex, itemData.item)
        }
        return inventory
    }

    /**
     * Opens the menu for [whoOpen] with [contents] pre-loaded into the free slots (in [getFreeSlots]
     * order): the i-th non-null entry is placed into the i-th free slot; extras beyond the free-slot
     * count are ignored.
     *
     * This is the load-side counterpart to [onClose]: use it for persistent containers to restore the
     * saved contents on open. It only touches free slots — pattern (button/decoration) slots keep the
     * items from [getSymbolsToItems]. A no-op on the free slots if the menu has none.
     */
    fun openWithFreeSlots(whoOpen: Player, contents: List<ItemStack?>) {
        val inventory = getMenu(whoOpen)
        val freeSlots = getFreeSlots(whoOpen) ?: emptyList()
        contents.forEachIndexed { index, stack ->
            val slot = freeSlots.getOrNull(index) ?: return@forEachIndexed
            if (stack != null) inventory.setItem(slot, stack)
        }
        whoOpen.openInventory(inventory)
    }

    /** Players who currently have this menu open. */
    fun getViewers(): List<Player> {
        return Bukkit.getOnlinePlayers().filter { it.openInventory.topInventory.holder === inventoryHolder }
    }

    /** Inventory title shown at the top. */
    abstract fun getMenuTitle(): String

    /**
     * Row strings (9 chars each) mapping characters to items via [getSymbolsToItems]; null for empty.
     * @param whoOpen the player the menu is being built for, or null for the default layout — use it for
     *   per-viewer layouts, or ignore it for static menus
     */
    abstract fun getPattern(whoOpen: Player? = null): List<String>?

    /**
     * Maps each pattern character to its [SlotData] (item + click handlers).
     * @param whoOpen the player the menu is being built for, or null for the default layout — use it for
     *   per-viewer items, or ignore it for static menus
     */
    abstract fun getSymbolsToItems(whoOpen: Player? = null): Map<Char, SlotData>

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Pattern character marking player-editable slots, or null if the menu is read-only. */
    abstract fun getFreeSlotSymbol(): Char?

    /** Explicit inventory size, or null to size automatically from the pattern. */
    abstract fun getSlotsCount(): Int?

    /**
     * Called **before** a free-slot change is applied. React to it and/or veto it: return
     * [SlotChangeResult.CANCEL] to block the change (the Bukkit interaction is cancelled, so the item
     * stays put), or [SlotChangeResult.ALLOW] to let it through. Use it as a slot filter (reject certain
     * items), a read-only guard, or just to persist/update on change.
     *
     * Fired on every free-slot mutation path — direct click, number-key swap, shift-click from the
     * player inventory, and drag. On a swap it's called twice: once for the outgoing item
     * ([wasPlaced] = false) and once for the incoming one ([wasPlaced] = true). For bulk paths
     * (shift-click / drag) the target slot isn't resolved yet, so [slot] is `-1` — filter by [item].
     *
     * @param slot the affected free slot, or `-1` for shift-click / drag
     * @param item the item being placed ([wasPlaced] = true) or removed ([wasPlaced] = false)
     * @param wasPlaced true if the item is going into the slot, false if being taken out
     */
    abstract fun onSlotChanged(player: Player, slot: Int, item: ItemStack, wasPlaced: Boolean): SlotChangeResult

    /**
     * Called when [player] closes this menu, before the framework decides what to do with the free-slot
     * items. Read the final [inventory] state here if you need it (e.g. persist the contents), then
     * return a [CloseAction] telling the framework how to dispose of the free slots.
     *
     * The default returns [CloseAction.RETURN_TO_PLAYER] — the "input tray" behaviour that hands the
     * free-slot items back to the player. Override and return [CloseAction.NO_RETURN] for a persistent
     * container: save the contents yourself here, and the framework will leave the slots alone (the
     * inventory is a throwaway snapshot, so unread items are discarded — see [CloseAction.NO_RETURN]).
     */
    open fun onClose(player: Player, inventory: Inventory): CloseAction = CloseAction.RETURN_TO_PLAYER

    /**
     * Slot indices marked editable by [getFreeSlotSymbol] in [whoOpen]'s layout, or null if none.
     * @param whoOpen the player the menu is built for (or null for the default layout); pattern is
     *   resolved via [getPattern]
     */
    fun getFreeSlots(whoOpen: Player? = null): List<Int>? {
        val symbol = getFreeSlotSymbol() ?: return null
        val pattern = getPattern(whoOpen) ?: return null
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

    /** Resolves [whoOpen]'s pattern (or the default when null) into a slot-index → [SlotData] map. */
    fun getItemsWithPattern(whoOpen: Player? = null): Map<Int, SlotData> {
        val items = mutableMapOf<Int, SlotData>()

        getPattern(whoOpen)?.let { pattern ->
            val patternItems = getSymbolsToItems(whoOpen)
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
