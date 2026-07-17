package ru.hukm.effectiveSpigot.minecraft.utils

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.nms.NmsPlayerLook
import java.time.Duration
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
        player.showTitle(
            Title.title(
                Component.text(FADE_SCREEN_SYMBOL.toString()),
                Component.empty(),
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
                )
            )
        )
        if (fullCameraFadeRunnable != null) {
            EffectiveSpigot.instance.launch {
                delay((stay / 2 + fadeIn).ticks)
                fullCameraFadeRunnable.run()
            }
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
        EffectiveSpigot.instance.launch {
            var elapsed = 0
            var lastYawOffset = 0f
            var lastPitchOffset = 0f

            while (true) {
                val isDurationOver = duration > 0 && elapsed >= duration
                val isCallbackFalse = shouldContinue != null && !shouldContinue.invoke()

                if (!player.isOnline || isDurationOver || isCallbackFalse) {
                    NmsPlayerLook.sendRelativeLook(player, -lastYawOffset, -lastPitchOffset)
                    return@launch
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

                NmsPlayerLook.sendRelativeLook(player, deltaYaw, deltaPitch)

                lastYawOffset = newYawOffset
                lastPitchOffset = newPitchOffset
                elapsed++

                delay(1.ticks)
            }
        }
    }
}