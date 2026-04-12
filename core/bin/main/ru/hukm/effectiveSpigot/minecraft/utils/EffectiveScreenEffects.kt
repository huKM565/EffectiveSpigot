package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.hukm.effectiveSpigot.EffectiveSpigot
import kotlin.math.pow
import kotlin.random.Random

object EffectiveScreenEffects {
    const val FADE_SCREEN_SYMBOL = ' '

    enum class ShakeType {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT;

        fun getMultiplier(progress: Double): Double {
            return when (this) {
                EASE_IN -> progress * progress
                EASE_OUT -> 1 - (1 - progress) * (1 - progress)
                EASE_IN_OUT -> if (progress < 0.5) 2 * progress * progress else 1 - (-2 * progress + 2).pow(
                    2.0
                ) / 2

                else -> progress
            }
        }
    }

    fun runCameraFade(player: Player) {
        runCameraFade(player, 10, 20, 10)
    }

    fun runCameraFade(player: Player, fadeIn: Int, stay: Int, fadeOut: Int) {
        player.sendTitle(FADE_SCREEN_SYMBOL.toString(), null, fadeIn, stay, fadeOut)
    }

    fun runCameraShake(player: Player, intensity: Float, duration: Int, type: ShakeType) {
        object : BukkitRunnable() {
            var elapsed: Int = 0

            override fun run() {
                if (elapsed >= duration || !player.isOnline()) {
                    this.cancel()
                    return
                }

                val progress: Double = 1.0 - (elapsed.toDouble() / duration)
                val currentIntensity: Double = intensity * type.getMultiplier(progress)

                val yawOffset: Float = (Random.nextFloat() * 2 - 1) * currentIntensity.toFloat()
                val pitchOffset: Float = (Random.nextFloat() * 2 - 1) * currentIntensity.toFloat()

                val location = player.location

                player.setRotation(location.yaw + yawOffset, location.pitch + pitchOffset)

                elapsed++
            }
        }.runTaskTimer(EffectiveSpigot.instance, 0L, 1L)
    }
}