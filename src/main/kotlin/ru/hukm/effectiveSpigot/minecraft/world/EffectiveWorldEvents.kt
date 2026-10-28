package ru.hukm.effectiveSpigot.minecraft.world

import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Material
import io.papermc.paper.event.block.BlockBreakBlockEvent
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.event.block.Action
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.event.world.StructureGrowEvent
import ru.hukm.effectiveSpigot.minecraft.events.event

internal object EffectiveWorldEvents {
    fun init() {
        event<ChunkLoadEvent>(EventPriority.MONITOR) {
            EffectiveWorld.tryUploadChunk(it.chunk)
        }

        event<ChunkUnloadEvent>(EventPriority.MONITOR) {
            EffectiveWorld.setIsUnload(it.chunk)
        }

        event<BlockPlaceEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.blockPlaced)
        }

        event<BlockMultiPlaceEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlocks(it.replacedBlockStates.map { state -> state.block })
        }

        event<BlockBreakEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAir(it.block)
        }

        event<BlockBurnEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAir(it.block)
        }

        event<BlockExplodeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAirs(it.blockList())
        }

        event<EntityExplodeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAirs(it.blockList())
        }

        event<BlockFadeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<BlockFertilizeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            it.blocks.forEach { state -> updateTo(state.block, state.type) }
        }

        event<BlockFormEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<BlockFromToEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            val block = it.block
            val toBlock = it.toBlock

            updateTo(toBlock, block.type)

            if (block.type == Material.DRAGON_EGG) {
                updateTo(block, Material.AIR)
            }
        }

        event<BlockPhysicsEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.markDirty(it.block)
        }

        event<BlockDestroyEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.material)
        }

        event<BlockIgniteEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.markDirty(it.block)
        }

        event<BlockGrowEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<StructureGrowEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            it.blocks.forEach { state -> updateTo(state.block, state.type) }
        }

        event<LeavesDecayEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAir(it.block)
        }

        event<EntityBlockFormEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<EntityChangeBlockEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.to)
        }

        event<BlockPistonExtendEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            markMoved(it.block, it.blocks, it.direction)
        }

        event<BlockPistonRetractEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            markMoved(it.block, it.blocks, it.direction)
        }

        event<PlayerBucketEmptyEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.markDirty(it.block)
        }

        event<PlayerBucketFillEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.markDirty(it.block)
        }

        event<SpongeAbsorbEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAirs(it.blocks.map { state -> state.block })
            EffectiveWorld.markDirty(it.block)
        }

        event<BlockSpreadEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<TNTPrimeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAir(it.block)
        }

        event<BlockBreakBlockEvent>(EventPriority.MONITOR) {
            EffectiveWorld.markDirty(it.block)
        }

        event<FluidLevelChangeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newData.material)
        }

        event<CauldronLevelChangeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            updateTo(it.block, it.newState.type)
        }

        event<PortalCreateEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            it.blocks.forEach { state -> updateTo(state.block, state.type) }
        }

        event<BlockDispenseEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            val facing = (it.block.blockData as? Directional)?.facing ?: return@event
            EffectiveWorld.markDirty(it.block.getRelative(facing))
        }

        event<PlayerInteractEvent>(EventPriority.MONITOR) {
            if (it.action != Action.RIGHT_CLICK_BLOCK) return@event
            val block = it.clickedBlock ?: return@event
            EffectiveWorld.markDirty(block)
            EffectiveWorld.markDirty(block.getRelative(it.blockFace))
        }
    }

    private fun updateTo(block: Block, material: Material) {
        EffectiveWorld.updateBlock(block.world, block.x, block.y, block.z, material)
    }

    private fun markMoved(piston: Block, moved: List<Block>, direction: BlockFace) {
        EffectiveWorld.markDirty(piston)
        EffectiveWorld.markDirty(piston.getRelative(direction))
        moved.forEach {
            EffectiveWorld.markDirty(it)
            EffectiveWorld.markDirty(it.getRelative(direction))
        }
    }
}
