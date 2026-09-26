package ru.hukm.effectiveSpigot.minecraft.blocks

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Instrument
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Note
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockDataMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BoundingBox
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.blocks.interfaces.EffectiveBlockInteractable
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.loottables.CustomLootable
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveTextureAnimation
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld
import java.util.UUID

/** Tool type that mines a block faster and, if the block requires it, is one of those that can drop its item. */
enum class EffectiveToolType(val suffix: String) {
    PICKAXE("_PICKAXE"),
    AXE("_AXE"),
    SHOVEL("_SHOVEL"),
    HOE("_HOE")
}

/** Material tier of a tool, ordered by [level] — a tool harvests a block only if its tier is at least the block's. */
enum class EffectiveToolTier(val level: Int) {
    HAND(0),
    WOOD(1),
    GOLD(1),
    STONE(2),
    IRON(3),
    DIAMOND(4),
    NETHERITE(5)
}

/**
 * Base class for a custom block backed by a note-block state.
 * ⚠️ **Work in progress.** Mining, drops and sounds are less battle-tested than items; [getCustomBlocks]
 * goes through the internal world block cache, which has known bugs. The API may change.
 *
 *
 * A subclass declares identity ([getNamespacedData]), textures ([getResourcePackData]) and the block
 * item's meta ([editItemMeta]); the note-block state is assigned by the framework (see [getNoteBlockData]); mining, sounds, drops and
 * hooks are tuned through the `open` methods. Like the other bases, the `object` must be instantiated
 * (`init()` from `onEnable`) to register.
 *
 * ### Textures
 * The model and note-block blockstates are generated only into a built pack: call
 * `EffectiveResourcepack.addServerResourcepack(this, "", "")` in `onEnable` **after** the blocks'
 * `init()`, otherwise the block places but looks like a plain note block. The server also needs
 * `block-updates.disable-noteblock-updates: true` in `paper-global.yml`.
 */
abstract class EffectiveBlock {

    /**
     * Block textures for the generated cube model (`minecraft:block/cube`). [texture] is the main texture
     * used for every face and the particle; override any single face with [up]/[down]/[north]/[south]/
     * [east]/[west] (or [particle]) — a `null` override falls back to [texture]. When building the model
     * use the resolved `*Texture` accessors.
     *
     * Set [animation] to animate the block (see [EffectiveTextureAnimation]). It is written for every face,
     * so each face texture is its own vertical strip of frames; a face with a plain square image is a single
     * frame and stays static, which lets a block animate only some faces. All animated faces run in sync.
     */
    data class ResourcePackData(
        val texture: String,
        val up: String? = null,
        val down: String? = null,
        val north: String? = null,
        val south: String? = null,
        val east: String? = null,
        val west: String? = null,
        val particle: String? = null,
        val animation: EffectiveTextureAnimation? = null
    ) {
        val upTexture get() = up ?: texture
        val downTexture get() = down ?: texture
        val northTexture get() = north ?: texture
        val southTexture get() = south ?: texture
        val eastTexture get() = east ?: texture
        val westTexture get() = west ?: texture
        val particleTexture get() = particle ?: texture
    }

    private data class BreakingData(
        val block: EffectiveBlock,
        val location: Location,
        val speed: Double,
        var progress: Double = 0.0
    )

    companion object {
        private val _namespacedKeyToBlock = hashMapOf<String, EffectiveBlock>()
        private val stateToBlock = hashMapOf<String, EffectiveBlock>()

        val namespacedKeyToBlock get() = _namespacedKeyToBlock

        private val breakingData = hashMapOf<Player, BreakingData>()

        /**
         * Denies vanilla note-block interaction (right-click cycling the note + playing the sound) and,
         * for a non-sneaking click with a block in hand, places that block manually against the custom
         * block (vanilla can't, since the note block is interactive). On any neighbour change re-applies
         * the affected note block's real state to clients so they don't briefly render the vanilla-
         * mispredicted block; `getState().update(true, true)` re-sends the state synchronously (the physics
         * event fires right after the neighbouring change within the same tick) and recurses up stacked
         * note blocks; pistons that would move a note block are cancelled. The state itself is kept
         * server-side by Paper's `block-updates.disable-noteblock-updates` in `paper-global.yml`.
         */
        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                event<PlayerInteractEvent> {
                    if (it.action != Action.RIGHT_CLICK_BLOCK) return@event
                    val clicked = it.clickedBlock ?: return@event
                    if (getEffectiveBlock(clicked) == null) return@event
                    if (it.player.isSneaking) return@event

                    it.setUseInteractedBlock(Event.Result.DENY)

                    if (it.hand != EquipmentSlot.HAND) return@event

                    val player = it.player
                    val main = player.inventory.itemInMainHand
                    val off = player.inventory.itemInOffHand
                    val (hand, item) = when {
                        main.type.isBlock && !main.type.isAir -> EquipmentSlot.HAND to main
                        off.type.isBlock && !off.type.isAir -> EquipmentSlot.OFF_HAND to off
                        else -> {
                            it.setUseItemInHand(Event.Result.ALLOW)
                            return@event
                        }
                    }
                    it.setUseItemInHand(Event.Result.DENY)
                    placeAgainst(player, clicked, it.blockFace, hand, item)
                }

                event<BlockPhysicsEvent>(EventPriority.HIGHEST) {
                    val block = it.block
                    val below = block.getRelative(BlockFace.DOWN)
                    val above = block.getRelative(BlockFace.UP)
                    if (below.type == Material.NOTE_BLOCK) {
                        it.isCancelled = true
                        updateColumn(below)
                    } else if (above.type == Material.NOTE_BLOCK) {
                        it.isCancelled = true
                        updateColumn(above)
                    }
                    if (block.type == Material.NOTE_BLOCK) {
                        it.isCancelled = true
                        updateColumn(block)
                    }
                }

                event<BlockPistonExtendEvent> {
                    if (it.blocks.any { b -> b.type == Material.NOTE_BLOCK }) it.isCancelled = true
                }
                event<BlockPistonRetractEvent> {
                    if (it.blocks.any { b -> b.type == Material.NOTE_BLOCK }) it.isCancelled = true
                }

                event<BlockDamageEvent>(EventPriority.HIGHEST, ignoreCancelled = true) {
                    val player = it.player
                    if (player.gameMode == GameMode.CREATIVE) return@event
                    val effectiveBlock = getEffectiveBlock(it.block) ?: run { removeBreaking(player); return@event }
                    val hardness = effectiveBlock.getHardness()
                    if (hardness <= 0.0) {
                        removeBreaking(player)
                        it.instaBreak = true
                        return@event
                    }

                    startBreaking(player, it.block, hardness, effectiveBlock)
                }
                event<BlockDamageAbortEvent>(EventPriority.LOWEST) { removeBreaking(it.player) }
                event<PlayerQuitEvent> { removeBreaking(it.player) }
                event<PlayerSwapHandItemsEvent> { removeBreaking(it.player) }
                event<PlayerDropItemEvent> { removeBreaking(it.player) }

                event<BlockBreakEvent> {
                    val block = it.block
                    val effectiveBlock = getEffectiveBlock(block) ?: return@event

                    if (breakingData.containsKey(it.player)) {
                        it.isCancelled = true
                        it.player.sendBlockChange(block.location, block.blockData)
                        return@event
                    }

                    effectiveBlock.onBreak(it)
                    if (!it.isCancelled) {
                        for (handler in effectiveBlock.breakHandlers) handler(it)
                    }

                    val breakSound = block.blockData.soundGroup.breakSound
                    if (breakSound == Sound.BLOCK_WOOD_BREAK) {
                        block.world.playSound(
                            block.location,
                            effectiveBlock.getBreakSound(),
                            1.0f,
                            1.0f
                        )
                    }

                    it.isDropItems = false
                    if (it.player.gameMode != GameMode.CREATIVE && canHarvest(it.player.inventory.itemInMainHand, effectiveBlock)) {
                        effectiveBlock.getDrop()?.let {
                            CustomLootable.spawnLootAtLocation(block.location.toCenterLocation(), it)
                        }
                    }
                }

                event<BlockPlaceEvent>(EventPriority.LOWEST) {
                    val effectiveBlock = getEffectiveBlockByItem(it.itemInHand) ?: return@event
                    val state = effectiveBlock.getNoteBlockData()
                    if (it.blockPlaced.blockData != state) it.blockPlaced.setBlockData(state, false)
                }

                event<BlockPlaceEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
                    val block = it.blockPlaced
                    val effectiveBlock = getEffectiveBlock(block)

                    effectiveBlock?.onPlace(it)
                    if (effectiveBlock != null && !it.isCancelled) {
                        for (handler in effectiveBlock.placeHandlers) handler(it)
                    }

                    val placeSound = block.blockData.soundGroup.placeSound
                    if (placeSound == Sound.BLOCK_WOOD_PLACE) {
                        it.block.world.playSound(
                            it.block.location,
                            effectiveBlock?.getPlaceSound() ?: "minecraft:required.wood.place",
                            1.0f,
                            1.0f
                        )
                    }
                }

                event<GenericGameEvent>(EventPriority.LOWEST) {
                    if (it.event != GameEvent.STEP) return@event
                    val entity = it.entity as? LivingEntity ?: return@event
                    val block = entity.location.block.getRelative(BlockFace.DOWN)
                    if (block.blockData.soundGroup.stepSound != Sound.BLOCK_WOOD_STEP) return@event
                    block.world.playSound(
                        entity.location,
                        getEffectiveBlock(block)?.getStepSound() ?: "minecraft:required.wood.step",
                        SoundCategory.PLAYERS,
                        0.3f,
                        1.0f
                    )
                }

                event<BlockExplodeEvent> { dropFromExplosion(it.blockList()) }
                event<EntityExplodeEvent> { dropFromExplosion(it.blockList()) }

                event<BlockIgniteEvent> {
                    val block = it.block

                    if (block.type != Material.NOTE_BLOCK) return@event
                    val effectiveBlock = getEffectiveBlock(block) ?: return@event

                    it.isCancelled = !effectiveBlock.isIgnitable()
                }

                EffectiveSpigot.instance.launch {
                    while (true) {
                        val finished = mutableListOf<Player>()
                        for ((player, data) in breakingData.toList()) {
                            if (!player.isOnline || getEffectiveBlock(data.location.block) !== data.block) {
                                removeBreaking(player)
                                continue
                            }
                            data.progress += data.speed
                            val stage = data.progress.coerceIn(0.0, 1.0).toFloat()
                            for (viewer in data.location.world.getNearbyPlayers(data.location, 32.0)) {
                                viewer.sendBlockDamage(data.location, stage, breakerId(data.location))
                            }
                            player.sendBlockDamage(data.location, 0.0f, player.entityId)
                            if (data.progress >= 1.0) finished += player
                        }
                        for (player in finished) finishBreaking(player)
                        delay(1.ticks)
                    }
                }
            }
        }

        /**
         * Manually places the held block against a custom note block. Vanilla treats the note block as
         * interactive, so a non-sneaking right-click never sends a place packet — we reproduce it: resolve
         * the target cell (replacing a click straight into grass/snow), take the block data (a custom block
         * item places its block's current note-block state, other blocks keep the item's own data), fire a
         * [BlockPlaceEvent] for protection plugins, then consume the item and play the place sound.
         */
        private fun placeAgainst(player: Player, clicked: Block, face: BlockFace, hand: EquipmentSlot, item: ItemStack) {
            val type = item.type
            if (!type.isBlock || type.isAir) return

            val target = if (clicked.isReplaceable) clicked else clicked.getRelative(face)
            if (!target.isReplaceable) return

            val blockData = getEffectiveBlockByItem(item)?.getNoteBlockData()
                ?: (item.itemMeta as? BlockDataMeta)
                    ?.takeIf { it.hasBlockData() }
                    ?.getBlockData(type)
                ?: type.createBlockData()

            val box = BoundingBox(
                target.x.toDouble(), target.y.toDouble(), target.z.toDouble(),
                target.x + 1.0, target.y + 1.0, target.z + 1.0
            )
            if (target.world.getNearbyEntities(box).any { it is LivingEntity }) return

            val replaced = target.state
            target.setBlockData(blockData, false)

            val placeEvent = BlockPlaceEvent(target, replaced, clicked, item, player, true, hand)
            Bukkit.getPluginManager().callEvent(placeEvent)
            if (placeEvent.isCancelled || !placeEvent.canBuild()) {
                replaced.update(true, false)
                return
            }

            if (player.gameMode != GameMode.CREATIVE) item.amount -= 1

            if (getEffectiveBlock(target) == null) {
                target.world.playSound(target.location, target.blockData.soundGroup.placeSound, 1.0f, 1.0f)
            }
            if (hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
        }

        private fun dropFromExplosion(blocks: MutableList<Block>) {
            val customs = blocks.filter { getEffectiveBlock(it) != null }
            if (customs.isEmpty()) return
            blocks.removeAll(customs.toSet())
             for (block in customs) {
                val effectiveBlock = getEffectiveBlock(block) ?: continue
                effectiveBlock.getDrop()?.let {
                    CustomLootable.spawnLootAtLocation(block.location.toCenterLocation(), it)
                }
                block.type = Material.AIR
            }
        }

        /** The registered block whose note-block state matches [block], or null if it isn't a custom block. */
        fun getEffectiveBlock(block: Block) = getEffectiveBlock(block.blockData)
        fun getEffectiveBlock(blockData: BlockData): EffectiveBlock? {
            if (blockData.material != Material.NOTE_BLOCK) return null
            return stateToBlock[blockData.asString]
        }

        /**
         * The registered block whose placeable [EffectiveBlock.item] [item] is, or null. Resolved by the item's
         * identity, not by the note-block state baked into it, so an old stack still maps to its block.
         */
        fun getEffectiveBlockByItem(item: ItemStack?): EffectiveBlock? {
            val key = EffectiveItem.getNamespacedKeyByItem(item) ?: return null
            return _namespacedKeyToBlock.values.firstOrNull { it.item.getNamespacedName() == key }
        }

        private fun updateColumn(block: Block) {
            val above = block.getRelative(BlockFace.UP)
            if (above.type == Material.NOTE_BLOCK) above.state.update(true, true)
            val next = above.getRelative(BlockFace.UP)
            if (next.type == Material.NOTE_BLOCK) updateColumn(above)
        }

        /** Transient `BLOCK_BREAK_SPEED` modifiers applied per player to freeze vanilla client digging. */
        private val breakSpeedModifiers = hashMapOf<UUID, AttributeModifier>()

        /**
         * A stable, negative break-animation source id for a block position, kept distinct from real entity
         * ids so our sent [Player.sendBlockDamage] animation occupies its own slot instead of fighting the
         * digger's client-side prediction (which would flicker).
         */
        private fun breakerId(location: Location): Int {
            val hash = (location.blockX * 31 + location.blockY) * 31 + location.blockZ
            return hash or Int.MIN_VALUE
        }

        /**
         * Begins plugin-driven mining of [block] by [player]: records the per-tick progress (vanilla
         * mining formula `toolSpeed / hardness / (canHarvest ? 30 : 100)`) so the timer loop can advance
         * the break animation and break the block itself, and near-freezes the client's own digging with a
         * `BLOCK_BREAK_SPEED` modifier so it can't destroy the note block on its own before the timer does.
         */
        private fun startBreaking(player: Player, block: Block, hardness: Double, effectiveBlock: EffectiveBlock) {
            removeBreaking(player)

            val tool = player.inventory.itemInMainHand
            val multiplier = if (canHarvest(tool, effectiveBlock)) 30.0 else 100.0
            val speed = toolSpeed(tool, effectiveBlock) / hardness / multiplier
            breakingData[player] = BreakingData(effectiveBlock, block.location, speed)

            val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return
            val modifier = AttributeModifier(
                NamespacedKey(EffectiveSpigot.instance, "break_speed"),
                -1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.HAND
            )
            breakSpeedModifiers[player.uniqueId] = modifier
            attribute.addTransientModifier(modifier)
        }

        private fun removeBreaking(player: Player) {
            breakingData.remove(player)?.let { data ->
                for (viewer in data.location.world.getNearbyPlayers(data.location, 32.0)) {
                    viewer.sendBlockDamage(data.location, 0.0f, breakerId(data.location))
                }
            }
            val modifier = breakSpeedModifiers.remove(player.uniqueId) ?: return
            player.getAttribute(Attribute.BLOCK_BREAK_SPEED)?.removeModifier(modifier)
        }

        /**
         * Timer-driven break: removes the mining record first (so the guard in the [BlockBreakEvent] listener
         * doesn't cancel our own event), fires a [BlockBreakEvent] for protection plugins, and on success
         * clears the block — the listener handles the drop and sound.
         */
        private fun finishBreaking(player: Player) {
            val data = breakingData[player] ?: return
            removeBreaking(player)
            val block = data.location.block
            if (getEffectiveBlock(block) !== data.block) return
            val breakEvent = BlockBreakEvent(block, player)
            Bukkit.getPluginManager().callEvent(breakEvent)
            if (breakEvent.isCancelled) return
            block.type = Material.AIR
        }

        /** Mining-speed multiplier of [tool] against [block]: the tool's tier speed if its type is correct, else 1. */
        private fun toolSpeed(tool: ItemStack, block: EffectiveBlock): Double {
            val tools = block.getCorrectTools()
            if (tools.isEmpty()) return 1.0
            val name = tool.type.name
            if (tools.none { name.endsWith(it.suffix) }) return 1.0
            return when {
                name.startsWith("NETHERITE_") -> 9.0
                name.startsWith("DIAMOND_") -> 8.0
                name.startsWith("IRON_") -> 6.0
                name.startsWith("STONE_") -> 4.0
                name.startsWith("GOLDEN_") -> 12.0
                name.startsWith("WOODEN_") -> 2.0
                else -> 1.0
            }
        }

        /** The tool's material tier from its [Material] name, or [EffectiveToolTier.HAND] for a bare hand / non-tool. */
        private fun toolTier(name: String): EffectiveToolTier = when {
            name.startsWith("NETHERITE_") -> EffectiveToolTier.NETHERITE
            name.startsWith("DIAMOND_") -> EffectiveToolTier.DIAMOND
            name.startsWith("IRON_") -> EffectiveToolTier.IRON
            name.startsWith("STONE_") -> EffectiveToolTier.STONE
            name.startsWith("GOLDEN_") -> EffectiveToolTier.GOLD
            name.startsWith("WOODEN_") -> EffectiveToolTier.WOOD
            else -> EffectiveToolTier.HAND
        }

        /**
         * Whether [tool] may drop [block]'s item — only checked when the block requires a correct tool:
         * the tool must be one of [EffectiveBlock.getCorrectTools] (if any) and at least [EffectiveBlock.getMinTier].
         */
        private fun canHarvest(tool: ItemStack, block: EffectiveBlock): Boolean {
            if (!block.requiresCorrectTool()) return true
            val name = tool.type.name
            val tools = block.getCorrectTools()
            if (tools.isNotEmpty() && tools.none { name.endsWith(it.suffix) }) return false
            return toolTier(name).level >= block.getMinTier().level
        }
    }

    private val assignedVariation: Int

    init {
        val namespacedName = getNamespacedName()
        if (_namespacedKeyToBlock.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.already_registered", namespacedName))
        }
        assignedVariation = EffectiveBlockVariations.resolve(namespacedName)
        _namespacedKeyToBlock[namespacedName] = this
        stateToBlock[getNoteBlockData().asString] = this
    }

    /**
     * The placeable item: a note block carrying the block's model and force-placing this block's
     * [getNoteBlockData] state (via the item's block-data / `block_state` component), so it shows the
     * custom block right away.
     */
    val item = object : EffectiveItem() {
        override fun getResourcePackData(): EffectiveItem.ResourcePackData {
            val (plugin, blockName) = this@EffectiveBlock.getNamespacedData()
            val namespace = EffectiveMinecraftUtils.getNamespace(plugin)
            return ResourcePackData(
                modelJson = """{ "parent": "$namespace:block/$blockName" }"""
            )
        }

        override fun editMeta(meta: ItemMeta) {
            this@EffectiveBlock.editItemMeta(meta)
            (meta as BlockDataMeta).setBlockData(getNoteBlockData())
        }

        override fun getMaterial() = Material.NOTE_BLOCK

        override fun getNamespacedData() = this@EffectiveBlock.getNamespacedData()
    }

    /**
     * The note-block state this block is stored as in the world. The framework assigns every block a variation
     * (`1..799`) itself: the first time a block is registered it gets the smallest free number, saved in the
     * save-root registry (`<level>/data/effectivespigot/block_variations.json`) and reused on every start,
     * whatever plugins are added, removed or reordered; numbers are never handed to another block.
     *
     * The variation maps to instrument (0..15), note (0..24) and powered: `powered = variation >= 400`, then
     * instrument = `(variation % 400) / 25`, note = `% 25`.
     */
    fun getNoteBlockData(): NoteBlock {
        val data = Material.NOTE_BLOCK.createBlockData() as NoteBlock
        val variation = assignedVariation
        data.instrument = Instrument.entries[variation % 400 / 25]
        data.note = Note(variation % 400 % 25)
        data.isPowered = variation % 800 >= 400
        return data
    }

    fun getCustomBlocks(): List<Block> {
        val customBlocks = arrayListOf<Block>()

        for (world in Bukkit.getWorlds()) {
            for (data in EffectiveWorld.findBlocksByMaterial(Material.NOTE_BLOCK, world)) {
                val block = world.getBlockAt(data.x, data.y, data.z)
                if (getEffectiveBlock(block) === this) customBlocks += block
            }
        }

        return customBlocks
    }

    /**
     * Registers an interact handler for blocks of *this* custom type (matched by note-block state).
     * `RIGHT` = right-click on the block, `LEFT` = left-click (attack) it.
     *
     * @param click left/right interaction that triggers [callback]
     */
    fun addInteractHandler(
        click: Click,
        callback: (EffectiveBlockInteractable.EventsCallOptions) -> EffectiveAbstractInteract.Result
    ) {
        EffectiveBlockInteractable.addInteractHandler(this, click, callback)
    }

    private val placeHandlers = arrayListOf<(BlockPlaceEvent) -> Unit>()
    private val breakHandlers = arrayListOf<(BlockBreakEvent) -> Unit>()

    /** Registers a callback run after [onPlace]; skipped if [onPlace] left the event cancelled. */
    internal fun addPlaceHandler(handler: (BlockPlaceEvent) -> Unit) {
        placeHandlers.add(handler)
    }

    /** Registers a callback run after [onBreak]; skipped if [onBreak] left the event cancelled. */
    internal fun addBreakHandler(handler: (BlockBreakEvent) -> Unit) {
        breakHandlers.add(handler)
    }

    /** Called right after this block is placed (at `BlockPlaceEvent` MONITOR — the placement is already committed). */
    open fun onPlace(event: BlockPlaceEvent) {}

    /** Called when this block is broken, just before its sound and drops are handled. */
    open fun onBreak(event: BlockBreakEvent) {}

    /** Sound played when this block is placed. Defaults to the vanilla wood place sound (via `required.wood.place`). */
    open fun getPlaceSound() = "minecraft:required.wood.place"

    /** Sound played when this block is broken. Defaults to the vanilla wood break sound (via `required.wood.break`). */
    open fun getBreakSound() = "minecraft:required.wood.break"

    /** Sound played when an entity walks on this block. Defaults to the vanilla wood step sound (via `required.wood.step`). */
    open fun getStepSound() = "minecraft:required.wood.step"

    /** Block hardness — controls how long it takes to mine (vanilla note block is `0.8`). `<= 0` breaks instantly. */
    open fun getHardness() = 0.8

    /** Tool types that mine this block faster; empty (default) means no tool preference (base speed for any). */
    open fun getCorrectTools(): Set<EffectiveToolType> = emptySet()

    /** Minimum tool tier that can harvest this block, checked when [requiresCorrectTool]. Default [EffectiveToolTier.HAND]. */
    open fun getMinTier() = EffectiveToolTier.HAND

    /** Whether this block only drops its item when mined with a correct tool ([getCorrectTools] + [getMinTier]). */
    open fun requiresCorrectTool() = false

    open fun isIgnitable() = false

    open fun getDrop(): ArrayList<CustomLootable.ItemCellData>? = null

    abstract fun editItemMeta(meta: ItemMeta)
    abstract fun getResourcePackData(): ResourcePackData
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName() = getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
