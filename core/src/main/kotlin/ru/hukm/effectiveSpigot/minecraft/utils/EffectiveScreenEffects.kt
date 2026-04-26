package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.hukm.effectiveSpigot.EffectiveSpigot
import kotlin.math.pow
import kotlin.random.Random

object EffectiveScreenEffects {
    const val FADE_SCREEN_SYMBOL = '\ueff3'

    enum class ShakeType {
        CONSTANT,
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT;

        fun getMultiplier(progress: Double): Double {
            return when (this) {
                CONSTANT -> 1.0
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
        runCameraFade(player, fadeIn, stay, fadeOut, null)
    }

    fun runCameraFade(player: Player, fadeIn: Int, stay: Int, fadeOut: Int, fullCameraFadeRunnable: Runnable?) {
        player.sendTitle(FADE_SCREEN_SYMBOL.toString(), "", fadeIn, stay, fadeOut)
        if (fullCameraFadeRunnable != null) {
            object : BukkitRunnable() {
                override fun run() {
                    fullCameraFadeRunnable.run()
                }
            }.runTaskLater(EffectiveSpigot.instance, (stay / 2 + fadeIn).toLong())
        }
    }

    fun runCameraShake(
        player: Player,
        intensity: Float,
        duration: Int,
        type: ShakeType = ShakeType.CONSTANT
    ) {
        runCameraShake(player, intensity, duration, type, null)
    }

    fun runCameraShake(
        player: Player,
        intensity: Float,
        type: ShakeType = ShakeType.CONSTANT,
        shouldContinue: () -> Boolean
    ) {
        runCameraShake(player, intensity, -1, type, shouldContinue)
    }

    fun runCameraShake(
        player: Player,
        intensity: Float,
        duration: Int = -1,
        type: ShakeType = ShakeType.CONSTANT,
        shouldContinue: (() -> Boolean)? = null
    ) {
        object : BukkitRunnable() {
            var elapsed: Int = 0
            var lastYawOffset: Float = 0f
            var lastPitchOffset: Float = 0f

            override fun run() {
                val isDurationOver = duration > 0 && elapsed >= duration
                val isCallbackFalse = shouldContinue != null && !shouldContinue.invoke()

                if (!player.isOnline || isDurationOver || isCallbackFalse) {
                    EffectiveSpigot.mcvModule.sendRelativeLook(player, -lastYawOffset, -lastPitchOffset)
                    this.cancel()
                    return
                }

                val progress: Double = if (duration > 0) {
                    1.0 - (elapsed.toDouble() / duration)
                } else {
                    1.0
                }

                val currentIntensity: Double = intensity * type.getMultiplier(progress)

                val newYawOffset: Float = (Random.nextFloat() * 2 - 1) * currentIntensity.toFloat()
                val newPitchOffset: Float = (Random.nextFloat() * 2 - 1) * currentIntensity.toFloat()

                val deltaYaw = newYawOffset - lastYawOffset
                val deltaPitch = newPitchOffset - lastPitchOffset

                EffectiveSpigot.mcvModule.sendRelativeLook(player, deltaYaw, deltaPitch)

                lastYawOffset = newYawOffset
                lastPitchOffset = newPitchOffset
                elapsed++
            }
        }.runTaskTimer(EffectiveSpigot.instance, 0L, 1L)
    }
}