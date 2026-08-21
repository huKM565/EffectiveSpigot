package ru.hukm.effectiveSpigot.minecraft.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.loot.LootTables
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.*
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils

/**
 * Base class for every custom item in the framework.
 *
 * A subclass defines an item declaratively by overriding [editMeta], [getMaterial] and
 * [getNamespacedData]. On construction the item registers itself under its unique
 * namespaced name (see [getNamespacedName]); creating a duplicate name throws.
 *
 * Every stack produced by [createItemStack] carries the item's namespaced name in its
 * persistent data (under [ITEM_KEY]), so a plain [ItemStack] can later be matched back to
 * its [EffectiveItem] via [getNamespacedKeyByItem] / [equalByNamespacedKey].
 *
 * Behaviours are opt-in through the `make*` / `add*` methods, each backed by a dedicated
 * `Effective*` interface (clickable, throwable, durable, wearable, craftable, droppable, …).
 *
 * ### Declaring an item
 * ```kotlin
 * object ExampleItem : EffectiveItem() {
 *     override fun editMeta(meta: ItemMeta) {
 *         meta.setDisplayName("§fExample item")
 *     }
 *     override fun getMaterial() = Material.FIREWORK_STAR
 *     override fun getNamespacedData() = ExamplePlugin.instance to "example"
 *
 *     // Register behaviours here; call this from onEnable().
 *     fun init() {
 *         // makeThrowable { event -> ... } / makeDurable(3) / addClickHandler(...) / ...
 *     }
 * }
 * ```
 *
 * ### Picking a base material
 * If the item uses a custom texture from a resource pack (via a custom model/item model), prefer
 * [Material.FIREWORK_STAR] as the base: it has virtually no vanilla use or behaviour, so overriding
 * its model won't collide with a commonly-used item or trigger unwanted vanilla mechanics. Use a
 * "real" material only when you want that material's own look/behaviour.
 *
 * A Kotlin `object` is lazy — its code runs only when something first references it. Call the item's
 * `init()` from `onEnable` so the class is actually loaded at startup; otherwise it is never constructed and its
 * behaviours (click/throw/…) are never wired up. A telltale sign of a forgotten `init()` is the item
 * simply not showing up (e.g. missing from `/egive`).
 * ```kotlin
 * override fun onEnable() {
 *     ExampleItem.init()
 * }
 * ```
 * (Loading also registers it under its [getNamespacedName]; two items with the same name throw.)
 *
 * A built-in `/egive <item> <player>` command can hand out any registered item in-game.
 */
abstract class EffectiveItem {
    /**
     * Resource-pack data for the item. Provide a model via [modelPath] (path to a model JSON inside the
     * jar) or [modelJson] (the model JSON content itself); if both are null a default `item/generated`
     * model is built from the texture. The texture comes from [texturePath] (a file inside the jar) or
     * [textureBytes] (raw PNG bytes, e.g. generated at runtime); both are optional — omit them when the
     * supplied model brings its own textures. [modelJson] takes precedence over [modelPath], and
     * [textureBytes] over [texturePath].
     */
    data class ResourcePackData(
        val texturePath: String? = null,
        val textureBytes: ByteArray? = null,
        val modelPath: String? = null,
        val modelJson: String? = null
    ) {
        val isEmpty get() = texturePath == null && modelPath == null && modelJson == null && textureBytes == null
    }

    companion object {
        /** Persistent-data key under which every generated stack stores its namespaced name. */
        val ITEM_KEY = NamespacedKey(EffectiveSpigot.instance, "item")

        private val _namespacedKeyToItem = hashMapOf<String, EffectiveItem>()

        /** Read-only registry of all constructed items, keyed by their [getNamespacedName]. */
        val namespacedKeyToItem: Map<String, EffectiveItem> get() = _namespacedKeyToItem

        /** Whether two stacks share the same [Material] (ignores custom identity). */
        fun equalByMaterial(item1: ItemStack?, item2: ItemStack?): Boolean {
            return item1?.type == item2?.type
        }

        /** Whether both stacks carry the same framework namespaced key; false if either lacks one. */
        fun equalByNamespacedKey(item1: ItemStack?, item2: ItemStack?): Boolean {
            val itemValue1 = getNamespacedKeyByItem(item1) ?: return false
            val itemValue2 = getNamespacedKeyByItem(item2) ?: return false

            return itemValue1 == itemValue2
        }

        /**
         * Compares by namespaced key when both stacks have one, otherwise falls back to
         * [equalByMaterial]. Lets custom items and vanilla items be matched uniformly.
         */
        fun equalByNamespacedKeyIfExistElseByMaterial(item1: ItemStack?, item2: ItemStack?): Boolean {
            val key1 = getNamespacedKeyByItem(item1)
            val key2 = getNamespacedKeyByItem(item2)

            if (key1 != null && key2 != null){
                return equalByNamespacedKey(item1, item2)
            }

            if (key1 == key2) {
                return equalByMaterial(item1, item2)
            }

            return false
        }

        /** Looks up a registered [EffectiveItem] by its namespaced name, or null if unknown. */
        fun getEffectiveItemByNamespacedKey(namespacedKey: String): EffectiveItem? {
            return namespacedKeyToItem[namespacedKey]
        }

        /** Builds a fresh stack for the item registered under [namespacedKey], or null if unknown. */
        fun getItemByNamespacedKey(namespacedKey: String): ItemStack? {
            return getEffectiveItemByNamespacedKey(namespacedKey)?.createItemStack()
        }

        /** Reads the framework namespaced name stored on [item], or null for vanilla/empty stacks. */
        fun getNamespacedKeyByItem(item: ItemStack?): String? {
            return if (item != null) {
                EffectiveDataContainerUtils.getContainerValue(item, ITEM_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        /** Namespaced name of [item] if it is a custom item, otherwise its [Material] name. */
        fun getNamespacedKeyByItemElseMaterial(item: ItemStack?): String? {
            return getNamespacedKeyByItem(item) ?: item?.type?.name
        }

        /** Wraps each line as a non-italic gray lore [Component]. */
        fun getGrayLore(lines: List<String>): List<Component> {
            return lines.map { Component.text(it, NamedTextColor.GRAY) }
        }

    }

    init {
        //TODO(Сделать, чтобы нельзя было использовать названия обычных предметов)
        val namespacedName = getNamespacedName()
        if (namespacedKeyToItem.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.items.already_registered", namespacedName))
        }
        _namespacedKeyToItem[namespacedName] = this
    }

    /**
     * Creates a single stack of this item, fully finalized (meta, identity, behaviours).
     *
     * Every call builds a **fresh** stack from scratch, so any per-stack state (durability charges,
     * custom persistent data written after creation) starts at its default. To mutate an existing
     * item — e.g. spend a durability charge — operate on the stack already in the inventory, not on
     * a new one from here.
     */
    fun createItemStack() = createItemStack(1)

    /** Creates a stack of [amount] copies of this item. */
    fun createItemStack(amount: Int): ItemStack {
        val item = ItemStack(getMaterial())
        return finalizeItem(item, amount)
    }

    private fun finalizeItem(item: ItemStack, amount: Int): ItemStack {
        item.amount = amount
        val meta = item.itemMeta ?: return item
        editMeta(meta)

        val resourcePackData = getResourcePackData()
        if (resourcePackData != null && !resourcePackData.isEmpty && !meta.hasItemModel()) {
            meta.itemModel = NamespacedKey(
                EffectiveMinecraftUtils.getNamespace(getNamespacedData().first),
                getNamespacedData().second
            )
        }

        item.itemMeta = meta
        EffectiveDataContainerUtils.setContainerValue(item, ITEM_KEY, PersistentDataType.STRING, getNamespacedName())
        EffectiveDurability.apply(this, item)
        createItemStackCallback(item)
        return item
    }

    fun createItemStack(additionalArgs: List<String>): ItemStack {
        return createItemStack(1, additionalArgs)
    }

    fun createItemStack(amount: Int, additionalArgs: List<String>): ItemStack {
        val item = ItemStack(getMaterial())
        val meta = item.itemMeta

        val args = getAdditionalArgs()

        AdditionalArgsSupport.applyToHolder(meta, args, additionalArgs, "items")

        if (showAdditionArgsInLore() && args != null) {
            val tempLore = meta.lore?.toMutableList() ?: mutableListOf()

            val keys = args.keys.map { it.first }
            val values = AdditionalArgsSupport.readFromHolder(meta, args)

            values.forEachIndexed { index, value ->
                tempLore.add("${keys[index]}: ${value[index]}")
            }

            meta.lore = tempLore
        }

        item.itemMeta = meta

        return finalizeItem(item, amount)
    }

    /** Whether [effectiveItem] is the same registered item as this one. */
    fun equalByNamespacedKey(effectiveItem: EffectiveItem) = getNamespacedName() == effectiveItem.getNamespacedName()

    /** Whether [item] is a stack of this item (matched by namespaced key). */
    fun equalByNamespacedKey(item: ItemStack) = getNamespacedName() == getNamespacedKeyByItem(item)

    /**
     * Registers a click handler for stacks of *this* custom item (matched by key). To instead handle a
     * plain vanilla [Material], register through [EffectiveClickable.addClickHandler] with a non-custom
     * stack.
     *
     * @param click which mouse button triggers the callback
     * @param callback invoked on click; its [EffectiveAbstractInteract.Result] decides whether the
     *   underlying Bukkit event is cancelled
     * @param ifRightClickOpenContainer allow the vanilla container to open on right-click instead of
     *   swallowing the interaction
     * @param cooldownData optional per-player cooldown for the handler
     */
    fun addClickHandler(
        click: Click,
        callback: InteractCallback,
        ifRightClickOpenContainer: Boolean = false,
        cooldownData: EffectiveAbstractInteract.CooldownData<EffectiveClickable.EventsCallOptions>? = null
    ) {
        EffectiveClickable.addClickHandler(createItemStack(), click, callback, ifRightClickOpenContainer, cooldownData)
    }

    /**
     * Registers a shapeless recipe that yields this item.
     *
     * @param ingredients each entry may be a [Material], an [ItemStack], a [org.bukkit.Tag] of
     *   materials, or a list of alternatives; all combinations are registered as separate recipes.
     */
    fun addShapelessCraft(ingredients: List<Any>) {
        EffectiveCraftable.addShapelessCraft(createItemStack(), ingredients, EffectiveSpigot.instance, getNamespacedName())
    }

    /**
     * Makes this item drop from the given sources.
     *
     * @param dropChance chance in `0.0..1.0`, computed per (nullable) player
     * @param lootTables vanilla loot tables this item may be added to
     * @param blocks blocks that may drop this item when broken
     * @param entities entities that may drop this item when killed
     * @param amount optional stack-size range per drop
     */
    fun addToLoot(
        dropChance: (Player?) -> Double,
        lootTables: List<LootTables>?,
        blocks: List<Material>?,
        entities: List<EntityType>?,
        amount: ((Player?) -> IntRange)? = null
    ) {
        EffectiveDropable.addLoot(
            EffectiveDropable.Data(
                createItemStack(),
                dropChance,
                lootTables,
                blocks,
                entities,
                amount
            )
        )
    }

    /**
     * Registers a brewing-stand recipe that yields this item.
     *
     * @param inputIngredient the ingredient placed in the top slot
     * @param inputBasePotionMeta the base potion required in the bottom slots
     * @param fuelUse blaze-powder fuel consumed
     * @param cookingTime brewing duration in ticks
     */
    fun addBrewRecipe(inputIngredient: ItemStack, inputBasePotionMeta: PotionMeta, fuelUse: Int, cookingTime: Int) {
        EffectiveBrewable.registerRecipe(EffectiveBrewable.Data(
            result = createItemStack(),
            inputIngredient = inputIngredient,
            inputBasePotionMeta = inputBasePotionMeta,
            fuelUse = fuelUse,
            cookingTime = cookingTime
        ))
    }

    /** Allows this item to be equipped to the head slot (right-click or helmet-slot drag). */
    fun makeWearable() {
        EffectiveWearable.makeWearable(this)
    }

    /**
     * Gives this item a durability bar acting as a limited-use counter.
     *
     * Stacks are forced to `maxStackSize = 1` and get `maxDamage = maxUses`; spend a charge with
     * [EffectiveDurability.consumeUse]. See [EffectiveDurability] for the trade-offs vs. a PDC counter.
     *
     * @param maxUses number of uses before the item breaks
     */
    fun makeDurable(maxUses: Int) {
        EffectiveDurability.makeDurable(this, maxUses)
    }

    /**
     * Makes this item throwable: right-clicking launches a snowball carrying the item, and [onHit]
     * runs when it lands. The projectile is identified by the item stored inside it.
     *
     * @param velocity speed multiplier along the player's look direction
     * @param consumeOnThrow whether to remove one from the stack on throw
     * @param throwSound sound played on throw, or null for silence
     * @param onHit callback receiving the [ProjectileHitEvent] of the framework's projectile
     */
    fun makeThrowable(
        velocity: Double = 1.5,
        consumeOnThrow: Boolean = true,
        throwSound: Sound? = Sound.ENTITY_SNOWBALL_THROW,
        onHit: (ProjectileHitEvent) -> Unit,
    ) {
        EffectiveThrowable.makeThrowable(this, velocity, consumeOnThrow, throwSound, onHit)
    }

    /** Prevents this item from being dropped, and keeps it in the inventory on death. */
    fun makeUndropable() {
        EffectiveUndropable.makeUndropable(this)
    }

    /** Configures the stack's meta (name, lore, model, …). Called during every [createItemStack]. */
    abstract fun editMeta(meta: ItemMeta)

    /** The base [Material] the item is built from. */
    abstract fun getMaterial(): Material

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Hook run on the finished stack after meta, identity and behaviours are applied. */
    open fun createItemStackCallback(item: ItemStack) {}

    /**
     * Declares extra, per-stack parameters this item accepts — values baked into an individual stack's
     * persistent data at creation time, so two stacks of the same item can carry different settings
     * (e.g. a "radius" or "power" tuned per stack).
     *
     * Override this to declare the parameters (each an ordered `name → PDC type` pair). They are then:
     * - supplied positionally when the stack is created — `createItemStack(amount, listOf("5", "true"))`,
     *   or via the built-in `/egive <item> <arg1> <arg2> …` command (parsed in declared order);
     * - read back with [additionalKey] + [EffectiveDataContainerUtils], typically inside a behaviour;
     * - optionally shown in the lore when [showAdditionArgsInLore] returns true.
     *
     * ```kotlin
     * override fun getAdditionalArgs() = AdditionalArgs(
     *     ExamplePlugin.instance,
     *     listOf("radius" to PersistentDataType.INTEGER)
     * )
     * override fun showAdditionArgsInLore() = true
     *
     * // reading the value from a created stack:
     * val radius = EffectiveDataContainerUtils.getContainerValue(
     *     stack, additionalKey("radius"), PersistentDataType.INTEGER
     * )
     * ```
     *
     * Returns null (the default) for items without extra parameters. Supported types are the primitive
     * PDC types and their comma-separated arrays.
     */
    open fun getAdditionalArgs(): AdditionalArgs? {
        return null
    }

    /** Whether the [getAdditionalArgs] values are appended to the item's lore automatically. */
    open fun showAdditionArgsInLore() : Boolean = false

    /**
     * Optional resource-pack texture/model info for this item. When non-null, the framework generates
     * the pack model/texture for it, and [createItemStack] automatically sets the stack's `item_model`
     * to `<plugin-namespace>:<id>` so the custom texture shows — unless [editMeta] already set one.
     */
    open fun getResourcePackData(): ResourcePackData? = null

    /** Namespaced keys backing this item's [getAdditionalArgs] (key → PDC type), or null if none. */
    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    /**
     * Resolves the persistent-data key for the declared additional arg [name] — use it to read the
     * per-stack value via [EffectiveDataContainerUtils]. See [getAdditionalArgs] for the full flow.
     * @throws IllegalArgumentException if [name] isn't a declared arg
     */
    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "items")

    /** Unique identity of the item as `"<plugin-name>/<id>"`, both lowercased. */
    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}