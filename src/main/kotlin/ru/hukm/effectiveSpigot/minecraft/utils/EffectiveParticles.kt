package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.sqrt

object EffectiveParticles {
    fun drawLine(
        players: Collection<Player>,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        dustOptions: DustOptions,
        step: Double = 0.5
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1

        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length == 0.0) return

        val iterations = max(1, (length / step).toInt())

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

    fun drawBox(
        players: Collection<Player>,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        dustOptions: DustOptions,
        step: Double = 0.5
    ) {
        drawLine(players, minX, minY, minZ, maxX, minY, minZ, dustOptions, step)
        drawLine(players, minX, maxY, minZ, maxX, maxY, minZ, dustOptions, step)
        drawLine(players, minX, minY, maxZ, maxX, minY, maxZ, dustOptions, step)
        drawLine(players, minX, maxY, maxZ, maxX, maxY, maxZ, dustOptions, step)

        drawLine(players, minX, minY, minZ, minX, maxY, minZ, dustOptions, step)
        drawLine(players, maxX, minY, minZ, maxX, maxY, minZ, dustOptions, step)
        drawLine(players, minX, minY, maxZ, minX, maxY, maxZ, dustOptions, step)
        drawLine(players, maxX, minY, maxZ, maxX, maxY, maxZ, dustOptions, step)

        drawLine(players, minX, minY, minZ, minX, minY, maxZ, dustOptions, step)
        drawLine(players, maxX, minY, minZ, maxX, minY, maxZ, dustOptions, step)
        drawLine(players, minX, maxY, minZ, minX, maxY, maxZ, dustOptions, step)
        drawLine(players, maxX, maxY, minZ, maxX, maxY, maxZ, dustOptions, step)
    }
}
