package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import ru.hukm.effectiveSpigot.minecraft.events.event

object EffectiveWorldEvents {
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
            EffectiveWorld.setAir(it.block)
        }

        event<BlockBurnEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAir(it.block)
        }

        event<BlockExplodeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAirs(it.blockList())
        }

        event<BlockFadeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.newState.block)
        }

        event<BlockFertilizeEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlocks(it.blocks.map { state -> state.block })
        }

        event<BlockFormEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.newState.block)
        }

        event<BlockFromToEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            val block = it.block
            val toBlock = it.toBlock

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

        event<BlockPhysicsEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.block)
            //TODO(мб все же требуеться таймер)
        }

        event<BlockIgniteEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.ignitingBlock!!)
        }

        event<BlockGrowEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.newState.block)
        }

        event<EntityBlockFormEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.updateBlock(it.newState.block)
        }

        event<SpongeAbsorbEvent>(EventPriority.MONITOR, ignoreCancelled = true) {
            EffectiveWorld.setAirs(it.blocks.map { state -> state.block })
        }
    }
}
