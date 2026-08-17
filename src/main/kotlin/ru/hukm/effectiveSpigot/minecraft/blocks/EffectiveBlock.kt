package ru.hukm.effectiveSpigot.minecraft.blocks

import org.bukkit.GameMode
import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Note
import org.bukkit.block.Block
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils

abstract class EffectiveBlock {
    /**
     * Resource-pack data for a custom-textured block. [texture] is required and acts as the fallback
     * for any face left unset ([top], [bottom], [north], [south], [east], [west]). The framework
     * generates a `minecraft:block/cube` model with these six faces at
     * `assets/<child_ns>/models/block/<name>.json`.
     */
    data class ResourcePackData(
        val texture: String,
        val top: String? = null,
        val bottom: String? = null,
        val north: String? = null,
        val south: String? = null,
        val east: String? = null,
        val west: String? = null,
    ) {
        val effectiveTop get() = top ?: texture
        val effectiveBottom get() = bottom ?: texture
        val effectiveNorth get() = north ?: texture
        val effectiveSouth get() = south ?: texture
        val effectiveEast get() = east ?: texture
        val effectiveWest get() = west ?: texture
    }

    /** Why an [EffectiveBlock] was destroyed. Each variant carries the original Bukkit event. */
    sealed class BreakCause {
        data class Player(val event: BlockBreakEvent) : BreakCause()
        data class BlockExplosion(val event: BlockExplodeEvent) : BreakCause()
        data class EntityExplosion(val event: EntityExplodeEvent) : BreakCause()
        data class Piston(val event: BlockPistonEvent) : BreakCause()
        data class Burn(val event: BlockBurnEvent) : BreakCause()
        data class Fade(val event: BlockFadeEvent) : BreakCause()
        data class LeavesDecay(val event: LeavesDecayEvent) : BreakCause()
        data class LiquidFlow(val event: BlockFromToEvent) : BreakCause()
        data class EntityChange(val event: EntityChangeBlockEvent) : BreakCause()
    }

    companion object {
        /**
         * 16 playable Bukkit noteblock instruments in the order they map to `custom_variation`
         * indices. The other `Instrument` enum values (mob-head instruments) can't be set on
         * a noteblock's blockdata directly, so they're excluded — total 16 × 25 × 2 = 800.
         */
        val PLAYABLE_INSTRUMENTS: List<Instrument> = listOf(
            Instrument.PIANO, Instrument.BASS_DRUM, Instrument.SNARE_DRUM, Instrument.STICKS,
            Instrument.BASS_GUITAR, Instrument.FLUTE, Instrument.BELL, Instrument.GUITAR,
            Instrument.CHIME, Instrument.XYLOPHONE, Instrument.IRON_XYLOPHONE, Instrument.COW_BELL,
            Instrument.DIDGERIDOO, Instrument.BIT, Instrument.BANJO, Instrument.PLING,
        )

        private val _namespacedKeyToBlock = hashMapOf<String, EffectiveBlock>()
        val namespacedKeyToBlock: Map<String, EffectiveBlock> get() = _namespacedKeyToBlock

        private val _blockDataToBlock = hashMapOf<Triple<Instrument, Int, Boolean>, EffectiveBlock>()
        /** Read-only reverse lookup keyed by (instrument, noteId, powered). */
        val blockDataToBlock: Map<Triple<Instrument, Int, Boolean>, EffectiveBlock> get() = _blockDataToBlock

        /**
         * Encodes a `custom_variation` (0..799) into `(instrument, noteId, powered)`.
         * `powered = variation / 400`; `instrumentIdx = (variation % 400) / 25`; `noteId = variation % 25`.
         *
         * Assumes [variation] is in range — callers should validate first (init does this).
         */
        fun encodeVariation(variation: Int): Triple<Instrument, Int, Boolean> {
            val powered = variation / 400 == 1
            val instrumentIdx = (variation % 400) / 25
            val noteId = variation % 25
            return Triple(PLAYABLE_INSTRUMENTS[instrumentIdx], noteId, powered)
        }

        /** O(1) lookup for the [EffectiveBlock] behind [block], or null if it isn't one of ours. */
        fun getEffectiveBlock(block: Block): EffectiveBlock? {
            if (block.type != Material.NOTE_BLOCK) return null
            val nb = block.blockData as? NoteBlock ?: return null
            return _blockDataToBlock[Triple(nb.instrument, nb.note.id.toInt(), nb.isPowered)]
        }

        /** Namespaced name of [block] if it resolves to a custom block, otherwise null. */
        fun getNamespacedKeyByBlock(block: Block): String? {
            return getEffectiveBlock(block)?.getNamespacedName()
        }

        /**
         * Compares by custom-block identity when both blocks resolve to one, otherwise falls back
         * to [Material] equality. Lets custom and vanilla blocks be matched uniformly.
         */
        fun equalByNamespacedKeyIfExistElseByMaterial(block1: Block, block2: Block): Boolean {
            val e1 = getEffectiveBlock(block1)
            val e2 = getEffectiveBlock(block2)

            if (e1 != null && e2 != null) return e1 === e2
            if (e1 == null && e2 == null) return block1.type == block2.type
            return false
        }

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                event<BlockPlaceEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    val effectiveBlock = _namespacedKeyToBlock.values.firstOrNull {
                        it.item.equalByNamespacedKey(event.itemInHand)
                    } ?: return@event
                    effectiveBlock.handlePlaced(event.block)
                }

                event<BlockBreakEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.block, BreakCause.Player(event))
                }

                event<BlockExplodeEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    event.blockList().toList().forEach { dispatchBreak(it, BreakCause.BlockExplosion(event)) }
                }

                event<EntityExplodeEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    event.blockList().toList().forEach { dispatchBreak(it, BreakCause.EntityExplosion(event)) }
                }

                event<BlockPistonExtendEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    event.blocks.forEach { dispatchBreak(it, BreakCause.Piston(event)) }
                }

                event<BlockPistonRetractEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    event.blocks.forEach { dispatchBreak(it, BreakCause.Piston(event)) }
                }

                event<BlockBurnEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.block, BreakCause.Burn(event))
                }

                event<BlockFadeEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.block, BreakCause.Fade(event))
                }

                event<LeavesDecayEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.block, BreakCause.LeavesDecay(event))
                }

                event<BlockFromToEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.toBlock, BreakCause.LiquidFlow(event))
                }

                event<EntityChangeBlockEvent>(EventPriority.HIGHEST, ignoreCancelled = true) { event ->
                    dispatchBreak(event.block, BreakCause.EntityChange(event))
                }

                // Suppress vanilla note-cycling on right-click.
                event<PlayerInteractEvent>(EventPriority.HIGH, ignoreCancelled = true) { event ->
                    if (event.action != Action.RIGHT_CLICK_BLOCK) return@event
                    val block = event.clickedBlock ?: return@event
                    if (getEffectiveBlock(block) == null) return@event
                    event.setUseInteractedBlock(Event.Result.DENY)
                }

                // Suppress vanilla noteblock sound (fires when redstone-powered / hit from above).
                event<NotePlayEvent>(EventPriority.HIGH, ignoreCancelled = true) { event ->
                    if (getEffectiveBlock(event.block) == null) return@event
                    event.isCancelled = true
                }
            }
        }

        private fun dispatchBreak(block: Block, cause: BreakCause) {
            val effectiveBlock = getEffectiveBlock(block) ?: return
            effectiveBlock.handleBroken(block, cause)
        }
    }

    val item = object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            this@EffectiveBlock.editItem(meta)
            // When the block declares its own ResourcePackData, point the item at the cube model
            // generated by EffectiveResourcepack. Skip if the user already set an item_model manually.
            if (this@EffectiveBlock.getResourcePackData() != null && !meta.hasItemModel()) {
                meta.itemModel = NamespacedKey(
                    EffectiveMinecraftUtils.getNamespace(this@EffectiveBlock.getNamespacedData().first),
                    this@EffectiveBlock.getNamespacedData().second
                )
            }
        }

        override fun getMaterial(): Material = Material.NOTE_BLOCK

        override fun getNamespacedData() = this@EffectiveBlock.getNamespacedData().first to (this@EffectiveBlock.getNamespacedData().second + "/item")
    }

    init {
        val namespacedName = getNamespacedName()
        val variation = getCustomVariation()
        if (variation !in 0..799) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.invalid_variation", variation, namespacedName))
        }
        val triple = encodeVariation(variation)
        _blockDataToBlock[triple]?.let { existing ->
            throw IllegalStateException(
                Locale.getMessage("errors.blocks.variation_collision", namespacedName, existing.getNamespacedName(), variation)
            )
        }
        if (_namespacedKeyToBlock.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.already_registered", namespacedName))
        }
        _namespacedKeyToBlock[namespacedName] = this
        _blockDataToBlock[triple] = this
    }

    private fun handlePlaced(block: Block) {
        val (instrument, noteId, powered) = encodeVariation(getCustomVariation())
        val nb = block.blockData as NoteBlock
        nb.instrument = instrument
        nb.note = Note(noteId)
        nb.isPowered = powered
        block.blockData = nb
        editBlock(block)
        onPlaced(block)
    }

    private fun handleBroken(block: Block, cause: BreakCause) {
        onBroken(block, cause)

        // For explosion causes: if the subclass set isCancelled=true, translate it to "remove just
        // this block from the blockList" so the rest of the explosion still resolves normally.
        when (cause) {
            is BreakCause.BlockExplosion -> if (cause.event.isCancelled) {
                cause.event.isCancelled = false
                cause.event.blockList().remove(block)
                return
            }
            is BreakCause.EntityExplosion -> if (cause.event.isCancelled) {
                cause.event.isCancelled = false
                cause.event.blockList().remove(block)
                return
            }
            else -> {}
        }

        if (!isStillDestroyed(cause)) return

        if (cause is BreakCause.Player && cause.event.player.gameMode != GameMode.CREATIVE) {
            cause.event.isDropItems = false
            block.world.dropItemNaturally(block.location, createBlock())
        }
    }

    private fun isStillDestroyed(cause: BreakCause): Boolean = when (cause) {
        is BreakCause.Player -> !cause.event.isCancelled
        is BreakCause.BlockExplosion -> !cause.event.isCancelled
        is BreakCause.EntityExplosion -> !cause.event.isCancelled
        is BreakCause.Piston -> !cause.event.isCancelled
        is BreakCause.Burn -> !cause.event.isCancelled
        is BreakCause.Fade -> !cause.event.isCancelled
        is BreakCause.LeavesDecay -> !cause.event.isCancelled
        is BreakCause.LiquidFlow -> !cause.event.isCancelled
        is BreakCause.EntityChange -> !cause.event.isCancelled
    }

    fun createBlock(): ItemStack = createBlock(1)
    fun createBlock(amount: Int): ItemStack = item.createItemStack(amount)

    open fun editItem(meta: ItemMeta) {}
    open fun editBlock(block: Block) {}

    /**
     * Optional resource-pack data for this block. When non-null, the framework generates a cube
     * model from [ResourcePackData]'s six textures and wires it up through
     * [ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack], mapping the block's
     * `(instrument, note, powered)` blockstate to that model.
     */
    open fun getResourcePackData(): ResourcePackData? = null

    /** Called after the placed noteblock has been re-stamped with this block's blockdata. */
    open fun onPlaced(block: Block) {}

    /**
     * Called for **any** destruction of this block — player break, explosion, piston, burn, fade,
     * decay, liquid flow, entity change. Invoked **before** framework cleanup, so [cause]'s wrapped
     * event is still live.
     *
     * Framework cleanup afterwards:
     * - Player break (non-creative): vanilla drop is suppressed and the custom item drops instead.
     * - Any other cause: no custom drop, vanilla behaviour applies.
     *
     * To veto the destruction from here, set `cause.event.isCancelled = true`. For explosion causes
     * the framework rewrites your cancel: instead of aborting the whole explosion it un-cancels the
     * event and just removes this block from `event.blockList()` — only this block survives.
     */
    open fun onBroken(block: Block, cause: BreakCause) {}

    /** Owning plugin + plugin-unique block id. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /**
     * Unique 0..799 slot for this block, encoded into the placed noteblock's
     * `(instrument, note, powered)` blockstate triple. Must be unique across all
     * [EffectiveBlock]s registered in the JVM — collisions throw at init time.
     */
    abstract fun getCustomVariation(): Int

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}
