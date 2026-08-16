package ru.hukm.effectiveSpigot.minecraft.blocks

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
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
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils

internal abstract class EffectiveBlock {
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
        private val BLOCK_KEY = NamespacedKey(EffectiveSpigot.instance, "block")

        private val _namespacedKeyToBlock = hashMapOf<String, EffectiveBlock>()
        val namespacedKeyToBlock: Map<String, EffectiveBlock> get() = _namespacedKeyToBlock

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                event<BlockPlaceEvent> { event ->
                    val effectiveBlock = _namespacedKeyToBlock.values.firstOrNull {
                        it.item.equalByNamespacedKey(event.itemInHand)
                    } ?: return@event
                    effectiveBlock.handlePlaced(event.block)
                }

                event<BlockBreakEvent> { event ->
                    dispatchBreak(event.block, BreakCause.Player(event))
                }

                event<BlockExplodeEvent> { event ->
                    event.blockList().toList().forEach { dispatchBreak(it, BreakCause.BlockExplosion(event)) }
                }

                event<EntityExplodeEvent> { event ->
                    event.blockList().toList().forEach { dispatchBreak(it, BreakCause.EntityExplosion(event)) }
                }

                event<BlockPistonExtendEvent> { event ->
                    event.blocks.forEach { dispatchBreak(it, BreakCause.Piston(event)) }
                }

                event<BlockPistonRetractEvent> { event ->
                    event.blocks.forEach { dispatchBreak(it, BreakCause.Piston(event)) }
                }

                event<BlockBurnEvent> { event ->
                    dispatchBreak(event.block, BreakCause.Burn(event))
                }

                event<BlockFadeEvent> { event ->
                    dispatchBreak(event.block, BreakCause.Fade(event))
                }

                event<LeavesDecayEvent> { event ->
                    dispatchBreak(event.block, BreakCause.LeavesDecay(event))
                }

                event<BlockFromToEvent> { event ->
                    dispatchBreak(event.toBlock, BreakCause.LiquidFlow(event))
                }

                event<EntityChangeBlockEvent> { event ->
                    dispatchBreak(event.block, BreakCause.EntityChange(event))
                }
            }
        }

        private fun dispatchBreak(block: Block, cause: BreakCause) {
            val display = getItemDisplayByBlock(block) ?: return
            val effectiveBlock = effectiveBlockFor(display) ?: return
            effectiveBlock.handleBroken(block, display, cause)
        }

        fun equalByNamespacedKey(itemDisplay1: ItemDisplay?, itemDisplay2: ItemDisplay?): Boolean {
            val value1 = getNamespacedKeyByItemDisplay(itemDisplay1) ?: return false
            val value2 = getNamespacedKeyByItemDisplay(itemDisplay2) ?: return false

            return value1 == value2
        }

        fun getNamespacedKeyByBlock(block: Block): String? {
            return getItemDisplayByBlock(block)?.let { getNamespacedKeyByItemDisplay(it) }
        }

        fun getNamespacedKeyByItemDisplay(itemDisplay: ItemDisplay?): String? {
            return if (itemDisplay != null) {
                EffectiveDataContainerUtils.getContainerValue(itemDisplay, BLOCK_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        fun equalByNamespacedKeyIfExistElseByMaterial(pair1: Pair<ItemDisplay?, Block>, pair2: Pair<ItemDisplay?, Block>): Boolean {
            val key1 = getNamespacedKeyByItemDisplay(pair1.first)
            val key2 = getNamespacedKeyByItemDisplay(pair2.first)

            if (key1 != null && key2 != null){
                return equalByNamespacedKey(pair1.first, pair2.first)
            }

            if (key1 == key2) {
                return pair1.second.type == pair2.second.type
            }

            return false
        }

        /**
         * The ItemDisplay standing exactly at [block]'s cell (matched by integer block coords, not by
         * a bounding-box radius), or null if there isn't one.
         */
        fun getItemDisplayByBlock(block: Block): ItemDisplay? {
            val x = block.x
            val y = block.y
            val z = block.z
            return block.chunk.entities.asSequence()
                .filterIsInstance<ItemDisplay>()
                .firstOrNull { display ->
                    val loc = display.location
                    loc.blockX == x && loc.blockY == y && loc.blockZ == z &&
                        getNamespacedKeyByItemDisplay(display) != null
                }
        }

        fun getItemDisplayByLocation(location: Location): ItemDisplay? {
            return getItemDisplayByBlock(location.block)
        }

        private fun effectiveBlockFor(display: ItemDisplay): EffectiveBlock? {
            val key = getNamespacedKeyByItemDisplay(display) ?: return null
            return _namespacedKeyToBlock[key]
        }
    }

    val itemDisplay = object : EffectiveEntity() {
        override fun editEntity(entity: Entity) {
            val itemDisplay = entity as ItemDisplay
            if (isUseCustomModelData()) itemDisplay.setItemStack(createBlock())
            editItemDisplay(itemDisplay)
        }

        override fun getEntityType() = EntityType.ITEM_DISPLAY
        override fun getNamespacedData() = EffectiveSpigot.instance to (this@EffectiveBlock.getNamespacedData().second + "/item_display")
    }

    val item = object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            this@EffectiveBlock.editItem(meta)
        }

        override fun getMaterial(): Material = getBlockMaterial()

        override fun getNamespacedData() = EffectiveSpigot.instance to (this@EffectiveBlock.getNamespacedData().second + "/item")
    }

    init {
        val namespacedName = getNamespacedName()

        if (!getBlockMaterial().isBlock || getBlockMaterial() == Material.AIR) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.invalid_material", namespacedName))
        }

        //TODO(Сделать, чтобы нельзя было использовать названия обычных блоков)
        if (_namespacedKeyToBlock.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.blocks.already_registered", namespacedName))
        }
        _namespacedKeyToBlock[namespacedName] = this
    }

    private fun handlePlaced(block: Block) {
        editBlock(block)
        val center = block.location.add(0.5, 0.5, 0.5)
        itemDisplay.spawnEntity(center).also {
            EffectiveDataContainerUtils.setContainerValue(it, BLOCK_KEY, PersistentDataType.STRING, getNamespacedName())
        }
        onPlaced(block)
    }

    private fun handleBroken(block: Block, display: ItemDisplay, cause: BreakCause) {
        onBroken(block, display, cause)
        display.remove()
        if (cause is BreakCause.Player && cause.event.player.gameMode != GameMode.CREATIVE) {
            cause.event.isDropItems = false
            block.world.dropItemNaturally(block.location, createBlock())
        }
    }

    fun createBlock(): ItemStack = createBlock(1)
    fun createBlock(amount: Int): ItemStack = item.createItemStack(amount)

    open fun isUseCustomModelData(): Boolean = false
    open fun editItemDisplay(itemDisplay: ItemDisplay) {}
    open fun editItem(meta: ItemMeta) {}
    open fun editBlock(block: Block) {}

    /** Called after this block was placed and its ItemDisplay was spawned. */
    open fun onPlaced(block: Block) {}

    /**
     * Called for **any** destruction of this block — player break, explosion, piston, burn, fade,
     * decay, liquid flow, entity change. Invoked **before** the ItemDisplay is removed and any
     * custom drop is spawned, so [display] and [cause]'s wrapped event are still live.
     *
     * The default framework behavior:
     * - Player break (non-creative): vanilla drop is suppressed and the custom item drops instead.
     * - Any other cause: no drop, just cleanup.
     *
     * To veto the destruction, set `cause.event.isCancelled = true` here (subclasses of [BreakCause]
     * that wrap a `Cancellable` event); when the event is cancelled, framework cleanup is still
     * called by this method — check with `cause.event.isCancelled` in your override if that matters.
     */
    open fun onBroken(block: Block, display: ItemDisplay, cause: BreakCause) {}

    abstract fun getBlockMaterial(): Material
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}
