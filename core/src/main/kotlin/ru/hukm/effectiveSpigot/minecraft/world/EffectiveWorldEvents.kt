package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos

class EffectiveWorldEvents : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onLoadChunkEvent(event: ChunkLoadEvent) {
        EffectiveWorld.tryUploadChunk(event.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onUnloadChunkEvent(event: ChunkUnloadEvent) {
        EffectiveWorld.setIsUnload(event.chunk)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        EffectiveWorld.updateBlock(event.blockPlaced)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMultiBlockPlaceEvent(event: BlockMultiPlaceEvent) {
        EffectiveWorld.updateBlocks(event.replacedBlockStates.map { it.block })
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
//        val pos = EffectiveBlockPos(event.block.x, event.block.y, event.block.z)
//
//// Берем данные ДО того, как превратим блок в воздух в нашей системе
//        val effectiveBlock = EffectiveWorld.getOrCreateInstance(event.block.world).getBlock(pos)
//
//
//        if (effectiveBlock != null) {
//            val bukkitY = event.block.y
//            val effectiveY = effectiveBlock.y
//            val bukkitMat = event.block.type
//            val effectiveMat = effectiveBlock.material
//
//            // Условия успешности
//            val yMatches = bukkitY == effectiveY
//            val matMatches = bukkitMat == effectiveMat
//
//            val color = if (yMatches && matMatches) "§a" else "§c"
//
//            event.player.sendMessage("""
//        $color--- Результаты теста EffectiveWorld ---
//        §7Координаты: §f[${pos.x}, ${pos.y}, ${pos.z}]
//
//        §7Высота Y: §fBukkit($bukkitY) vs Effective($effectiveY) ${if (yMatches) "§a✔" else "§c✘"}
//        §7Материал: §fBukkit($bukkitMat) vs Effective($effectiveMat) ${if (matMatches) "§a✔" else "§c✘"}
//
//        §7Статус: ${if (yMatches && matMatches) "§aСИНХРОНИЗИРОВАНО" else "§cРАССИНХРОН (Проверь 64/65)"}
//        §8[Инфо] Система установила AIR после этой проверки.
//    """.trimIndent())
//        } else {
//            event.player.sendMessage("§6[!] Ошибка: Блок по координатам ${pos.x}, ${pos.y}, ${pos.z} не найден в кэше SoA.")
//        }
        EffectiveWorld.setAir(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBurnEvent(event: BlockBurnEvent) {
        EffectiveWorld.setAir(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockExplodeEvent(event: BlockExplodeEvent) {
        EffectiveWorld.setAirs(event.blockList())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFadeEvent(event: BlockFadeEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFertilizeBlockEvent(event: BlockFertilizeEvent) {
        EffectiveWorld.updateBlocks(event.blocks.map { it.block })
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFormEvent(event: BlockFormEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFromToEvent(event: BlockFromToEvent) {
        val block = event.block
        val toBlock = event.toBlock

        EffectiveWorld.updateBlock(
            toBlock.world,
            toBlock.x,
            toBlock.y,
            toBlock.z,
            block.type
        )

        if (block.type == Material.DRAGON_EGG) {
            EffectiveWorld.updateBlock(
                block.world,
                block.x,
                block.y,
                block.z,
                Material.AIR
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPhysicsEvent(event: BlockPhysicsEvent) {
        val block = event.block
        EffectiveWorld.updateBlock(block)
        //TODO(мб все же требуеться таймер)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockIgniteEvent(event: BlockIgniteEvent) {
        EffectiveWorld.updateBlock(event.ignitingBlock!!)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockGrowEvent(event: BlockGrowEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityBlockFormEvent(event: EntityBlockFormEvent) {
        EffectiveWorld.updateBlock(event.newState.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSpongeAbsorbEvent(event: SpongeAbsorbEvent) {
        EffectiveWorld.setAirs(event.blocks.map { it.block })
    }
}