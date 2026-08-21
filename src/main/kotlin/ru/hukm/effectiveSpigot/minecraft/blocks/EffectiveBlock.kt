package ru.hukm.effectiveSpigot.minecraft.blocks

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Note
import org.bukkit.Sound
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
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
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
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
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

abstract class EffectiveBlock {

    /**
     * Block textures for the generated cube model (`minecraft:block/cube`). [texture] is the main texture
     * used for every face and the particle; override any single face with [up]/[down]/[north]/[south]/
     * [east]/[west] (or [particle]) — a `null` override falls back to [texture]. When building the model
     * use the resolved `*Texture` accessors.
     */
    data class ResourcePackData(
        val texture: String,
        val up: String? = null,
        val down: String? = null,
        val north: String? = null,
        val south: String? = null,
        val east: String? = null,
        val west: String? = null,
        val particle: String? = null
    ) {
        val upTexture get() = up ?: texture
        val downTexture get() = down ?: texture
        val northTexture get() = north ?: texture
        val southTexture get() = south ?: texture
        val eastTexture get() = east ?: texture
        val westTexture get() = west ?: texture
        val particleTexture get() = particle ?: texture
    }

    companion object {
        private val _namespacedKeyToBlock = hashMapOf<String, EffectiveBlock>()

        val namespacedKeyToBlock get() = _namespacedKeyToBlock

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
                    if (it.action != Action.RIGHT_CLICK_BLOCK || it.hand != EquipmentSlot.HAND) return@event
                    val clicked = it.clickedBlock ?: return@event
                    if (clicked.type != Material.NOTE_BLOCK) return@event
                    if (it.player.isSneaking) return@event
                    it.setUseInteractedBlock(Event.Result.DENY)
                    val item = it.item ?: return@event
                    if (!item.type.isBlock || item.type.isAir) return@event
                    it.setUseItemInHand(Event.Result.DENY)
                    placeAgainst(it, clicked)
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
                    val effectiveBlock = getByState(it.block) ?: run { removeBreakSpeed(player); return@event }
                    val hardness = effectiveBlock.getHardness()
                    if (hardness <= 0.0) {
                        removeBreakSpeed(player)
                        it.instaBreak = true
                        return@event
                    }
                    val speedFactor = (0.24 / hardness * toolSpeed(player.inventory.itemInMainHand, effectiveBlock)).coerceAtLeast(0.01)
                    applyBreakSpeed(player, speedFactor - 1.0)
                }
                event<BlockDamageAbortEvent>(EventPriority.LOWEST) { removeBreakSpeed(it.player) }
                event<PlayerQuitEvent> { removeBreakSpeed(it.player) }
                event<PlayerSwapHandItemsEvent> { removeBreakSpeed(it.player) }
                event<PlayerDropItemEvent> { removeBreakSpeed(it.player) }

                event<BlockBreakEvent> {
                    val block = it.block

                    val breakSound = block.blockData.soundGroup.breakSound
                    if (breakSound == Sound.BLOCK_WOOD_BREAK) {
                        it.block.world.playSound(
                            it.block.location,
                            getByState(block)?.getBreakSound() ?: "minecraft:required.wood.break",
                            1.0f,
                            1.0f
                        )
                    }

                    val effectiveBlock = getByState(it.block) ?: return@event

                    removeBreakSpeed(it.player)
                    it.isDropItems = false
                    if (it.player.gameMode != GameMode.CREATIVE && canHarvest(it.player.inventory.itemInMainHand, effectiveBlock)) {
                        it.block.world.dropItemNaturally(it.block.location.toCenterLocation(), effectiveBlock.item.createItemStack())
                    }
                }

                event<BlockPlaceEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
                    val block = it.blockPlaced

                    val placeSound = block.blockData.soundGroup.placeSound
                    if (placeSound == Sound.BLOCK_WOOD_PLACE) {
                        it.block.world.playSound(
                            it.block.location,
                            getByState(block)?.getPlaceSound() ?: "minecraft:required.wood.place",
                            1.0f,
                            1.0f
                        )
                    }
                }

                event<BlockExplodeEvent> { dropFromExplosion(it.blockList()) }
                event<EntityExplodeEvent> { dropFromExplosion(it.blockList()) }
            }
        }

        /**
         * Manually places the held block against a custom note block. Vanilla treats the note block as
         * interactive, so a non-sneaking right-click never sends a place packet — we reproduce it: resolve
         * the target cell (replacing a click straight into grass/snow), keep the item's own block data (a
         * custom block keeps its note-block state), fire a [BlockPlaceEvent] for protection plugins, then
         * consume the item and play the place sound.
         */
        private fun placeAgainst(event: PlayerInteractEvent, clicked: Block) {
            val hand = event.hand ?: return
            val item = event.item ?: return
            val type = item.type
            if (!type.isBlock || type.isAir) return

            val target = if (clicked.isReplaceable) clicked else clicked.getRelative(event.blockFace)
            if (!target.isReplaceable) return

            val blockData = (item.itemMeta as? BlockDataMeta)
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

            val player = event.player
            val placeEvent = BlockPlaceEvent(target, replaced, clicked, item, player, true, hand)
            Bukkit.getPluginManager().callEvent(placeEvent)
            if (placeEvent.isCancelled || !placeEvent.canBuild()) {
                replaced.update(true, false)
                return
            }

            if (player.gameMode != GameMode.CREATIVE) item.amount -= 1

            if (getByState(target) == null) {
                target.world.playSound(target.location, target.blockData.soundGroup.placeSound, 1.0f, 1.0f)
            }
            if (hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
        }

        private fun dropFromExplosion(blocks: MutableList<Block>) {
            val customs = blocks.filter { getByState(it) != null }
            if (customs.isEmpty()) return
            blocks.removeAll(customs.toSet())
            for (block in customs) {
                val effectiveBlock = getByState(block) ?: continue
                block.world.dropItemNaturally(block.location.toCenterLocation(), effectiveBlock.item.createItemStack())
                block.type = Material.AIR
            }
        }

        /** The registered block whose note-block state matches [block], or null if it isn't a custom block. */
        private fun getByState(block: Block) = getByState(block.blockData)
        private fun getByState(blockData: BlockData): EffectiveBlock? {
            if (blockData.material != Material.NOTE_BLOCK) return null
            val state = blockData.asString
            return _namespacedKeyToBlock.values.firstOrNull { it.getNoteBlockData().asString == state }
        }

        private fun updateColumn(block: Block) {
            val above = block.getRelative(BlockFace.UP)
            if (above.type == Material.NOTE_BLOCK) above.state.update(true, true)
            val next = above.getRelative(BlockFace.UP)
            if (next.type == Material.NOTE_BLOCK) updateColumn(above)
        }

        /** Transient `BLOCK_BREAK_SPEED` modifiers applied per player while mining a custom block. */
        private val breakSpeedModifiers = hashMapOf<UUID, AttributeModifier>()

        private fun applyBreakSpeed(player: Player, amount: Double) {
            removeBreakSpeed(player)
            val attribute = player.getAttribute(Attribute.BLOCK_BREAK_SPEED) ?: return
            val modifier = AttributeModifier(
                NamespacedKey(EffectiveSpigot.instance, "break_speed"),
                amount,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.HAND
            )
            breakSpeedModifiers[player.uniqueId] = modifier
            attribute.addTransientModifier(modifier)
        }

        private fun removeBreakSpeed(player: Player) {
            val modifier = breakSpeedModifiers.remove(player.uniqueId) ?: return
            player.getAttribute(Attribute.BLOCK_BREAK_SPEED)?.removeModifier(modifier)
        }

        /** Mining-speed multiplier of [tool] against [block]: the tool's tier speed if its type is correct, else 1. */
        private fun toolSpeed(tool: ItemStack, block: EffectiveBlock): Double {
            val tools = block.getCorrectTools()
            if (tools.isEmpty()) return 1.0
            val name = tool.type.name
            println(tools.none { name.endsWith(it.suffix) })
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

    init {
        val namespacedName = getNamespacedName()
        if (_namespacedKeyToBlock.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.block.already_registered", namespacedName))
        }
        _namespacedKeyToBlock[namespacedName] = this
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
     * Maps [getVariation] (0..799) to a note-block state: instrument (0..15), note (0..24) and powered.
     * Layout: `powered = variation >= 400`, then instrument = `(variation % 400) / 25`, note = `% 25`.
     */
    fun getNoteBlockData(): NoteBlock {
        val data = Material.NOTE_BLOCK.createBlockData() as NoteBlock
        val variation = getVariation()
        data.instrument = Instrument.entries[variation % 400 / 25]
        data.note = Note(variation % 400 % 25)
        data.isPowered = variation % 800 >= 400
        return data
    }

    /** Sound played when this block is placed. Defaults to the vanilla wood place sound (via `required.wood.place`). */
    open fun getPlaceSound() = "minecraft:required.wood.place"

    /** Sound played when this block is broken. Defaults to the vanilla wood break sound (via `required.wood.break`). */
    open fun getBreakSound() = "minecraft:required.wood.break"

    /** Block hardness — controls how long it takes to mine (vanilla note block is `0.8`). `<= 0` breaks instantly. */
    open fun getHardness() = 0.8

    /** Tool types that mine this block faster; empty (default) means no tool preference (base speed for any). */
    open fun getCorrectTools(): Set<EffectiveToolType> = emptySet()

    /** Minimum tool tier that can harvest this block, checked when [requiresCorrectTool]. Default [EffectiveToolTier.HAND]. */
    open fun getMinTier() = EffectiveToolTier.HAND

    /** Whether this block only drops its item when mined with a correct tool ([getCorrectTools] + [getMinTier]). */
    open fun requiresCorrectTool() = false

    abstract fun editItemMeta(meta: ItemMeta)
    abstract fun getVariation(): Int
    abstract fun getResourcePackData(): ResourcePackData
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName() = getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
