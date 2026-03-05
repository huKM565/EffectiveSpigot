package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot

class EffectiveWorldEvents: Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onLoadChunkEvent(event: ChunkLoadEvent) {
        EffectiveWorld.tryUploadChunk(event.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onUnloadChunkEvent(event: ChunkUnloadEvent) {
        EffectiveWorld.setIsUnload(event.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        EffectiveWorld.updateBlock(event.blockPlaced)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onMultiBlockPlaceEvent(event: BlockMultiPlaceEvent) {
        EffectiveWorld.updateBlocks(event.replacedBlockStates.map { it.block })
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        EffectiveWorld.setAir(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBurnEvent(event: BlockBurnEvent) {
        EffectiveWorld.setAir(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        EffectiveWorld.setAirs(event.blockList())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockFadeEvent(event: BlockFadeEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockFertilizeBlockEvent(event: BlockFertilizeEvent) {
        EffectiveWorld.updateBlocks(event.blocks.map { it.block })
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockFormEvent(event: BlockFormEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockFromToEvent(event: BlockFromToEvent) {
        Bukkit.getScheduler().runTaskLater(EffectiveSpigot.instance, Runnable {
            EffectiveWorld.updateBlock(event.toBlock)
        }, 1)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        Bukkit.getScheduler().runTaskLater(EffectiveSpigot.instance, Runnable {
            EffectiveWorld.updateBlock(event.block)
        }, 1)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockIgniteEvent(event: BlockIgniteEvent) {
        EffectiveWorld.updateBlock(event.ignitingBlock!!)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockGrowEvent(event: BlockGrowEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityBlockFormEvent(event: EntityBlockFormEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSpongeAbsorbEvent(event: SpongeAbsorbEvent) {
        EffectiveWorld.setAirs(event.blocks.map { it.block })
    }
}