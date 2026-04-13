package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

import org.bukkit.scheduler.BukkitTask
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItems
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import java.util.UUID

object EffectiveZoneRenderer {
    private val activeTasks = hashMapOf<UUID, BukkitTask>()

    fun startRendering(selection: () -> Pair<EffectiveBlockPos?, EffectiveBlockPos?>?, isRegistered: Boolean = true) {
        val dustOptions = DustOptions(if (!isRegistered) Color.fromRGB(102, 178, 255) else Color.fromRGB(255, 255, 0), 1.0f)

        val taskId = object : BukkitRunnable() {
            override fun run() {
                val selection = selection() ?: run {
                    cancel()
                    return
                }

                val pos1 = selection.first
                val pos2 = selection.second

                if (pos1 == null || pos2 == null) {
                    cancel()
                    return
                }

                val players = Bukkit.getOnlinePlayers().filter {
                    EffectiveItem.getNamespacedKeyByItem(
                        EffectiveInventoryUtils.getUsedItemFromHands(it)
                    ) == EffectiveItems.ZONE_SELECTOR.item.getNamespacedName()
                }

                if (players.isNotEmpty()) renderZone(players, pos1, pos2, dustOptions)
            }
        }.runTaskTimer(EffectiveSpigot.instance, 0, 5).taskId
    }

    private fun renderZone(players: Collection<Player>, pos1: EffectiveBlockPos, pos2: EffectiveBlockPos, dustOptions: DustOptions) {
        val minX = min(pos1.x, pos2.x).toDouble()
        val minY = min(pos1.y, pos2.y).toDouble()
        val minZ = min(pos1.z, pos2.z).toDouble()
        
        val maxX = max(pos1.x, pos2.x).toDouble() + 1.0
        val maxY = max(pos1.y, pos2.y).toDouble() + 1.0
        val maxZ = max(pos1.z, pos2.z).toDouble() + 1.0

        // Ребра по X
        drawEdge(players, minX, maxX, minY, minY, minZ, minZ, dustOptions)
        drawEdge(players, minX, maxX, maxY, maxY, minZ, minZ, dustOptions)
        drawEdge(players, minX, maxX, minY, minY, maxZ, maxZ, dustOptions)
        drawEdge(players, minX, maxX, maxY, maxY, maxZ, maxZ, dustOptions)

        // Ребра по Y
        drawEdge(players, minX, minX, minY, maxY, minZ, minZ, dustOptions)
        drawEdge(players, maxX, maxX, minY, maxY, minZ, minZ, dustOptions)
        drawEdge(players, minX, minX, minY, maxY, maxZ, maxZ, dustOptions)
        drawEdge(players, maxX, maxX, minY, maxY, maxZ, maxZ, dustOptions)

        // Ребра по Z
        drawEdge(players, minX, minX, minY, minY, minZ, maxZ, dustOptions)
        drawEdge(players, maxX, maxX, minY, minY, minZ, maxZ, dustOptions)
        drawEdge(players, minX, minX, maxY, maxY, minZ, maxZ, dustOptions)
        drawEdge(players, maxX, maxX, maxY, maxY, minZ, maxZ, dustOptions)
    }

    private fun drawEdge(players: Collection<Player>, x1: Double, x2: Double, y1: Double, y2: Double, z1: Double, z2: Double, dustOptions: DustOptions) {
        val step = 0.5
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length == 0.0) return
        
        val iterations = (length / step).toInt()
        
        for (i in 0..iterations) {
            val t = i.toDouble() / iterations
            val x = x1 + dx * t
            val y = y1 + dy * t
            val z = z1 + dz * t
            players.forEach { player ->
                player.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
            }
        }
    }
}