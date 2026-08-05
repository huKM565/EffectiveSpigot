package ru.hukm.effectiveSpigot.minecraft.utils

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.nms.NmsPlayerLook
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import java.time.Duration
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveFontChar
import kotlin.math.pow
import kotlin.random.Random

/**
 * Client-side screen effects: full-screen camera fades and camera shake.
 *
 * The fade is drawn with a bundled full-screen glyph shown via a title, so it needs the framework's
 * resource pack enabled. Shake nudges the player's yaw/pitch over time with configurable easing.
 *
 * A built-in `/escreen <target> <fade|shake> …` command triggers these effects in-game.
 */
object EffectiveScreenEffects {
    /** [EffectiveFontChar] token bound to the full-screen fade texture in the resource pack. */
    val FADE_SCREEN_GLYPH by lazy {
        EffectiveGlyph(
            "font/fade.png",
            512,
            256
        )
    }

    internal fun getModule(): IModule {
        return object : IModule {
            override fun init() {
                EffectiveResourcepack.addGlyph(
                    EffectiveSpigot.instance,
                    FADE_SCREEN_GLYPH
                )
            }
        }
    }


    /** Easing curve for camera shake intensity over its duration. */
    enum class ShakeType {
        CONSTANT,
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT;

        /** Intensity multiplier at normalized [progress] `0.0..1.0` for this easing. */
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

    /** Plays a default full-screen fade (10/20/10 ticks) for [player]. */
    fun runCameraFade(player: Player) {
        runCameraFade(player, 10, 20, 10)
    }

    /** Plays a full-screen fade with the given fade-in/stay/fade-out durations (in ticks). */
    fun runCameraFade(player: Player, fadeIn: Int, stay: Int, fadeOut: Int) {
        runCameraFade(player, fadeIn, stay, fadeOut, null)
    }

    /**
     * Plays a full-screen fade and runs [fullCameraFadeRunnable] at the darkest point
     * (after `fadeIn + stay/2` ticks) — handy for teleporting/updating the world while hidden.
     */
    fun runCameraFade(player: Player, fadeIn: Int, stay: Int, fadeOut: Int, fullCameraFadeRunnable: Runnable?) {
        player.showTitle(
            Title.title(
                Component.text(FADE_SCREEN_GLYPH.charGlyph()),
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

    /** Shakes [player]'s camera at [intensity] for [duration] ticks with the given easing [type]. */
    fun runCameraShake(
        player: Player,
        intensity: Float,
        duration: Int,
        type: ShakeType = ShakeType.CONSTANT
    ) {
        runCameraShake(player, intensity, duration, type, null)
    }

    /** Shakes [player]'s camera indefinitely while [shouldContinue] returns true. */
    fun runCameraShake(
        player: Player,
        intensity: Float,
        type: ShakeType = ShakeType.CONSTANT,
        shouldContinue: () -> Boolean
    ) {
        runCameraShake(player, intensity, -1, type, shouldContinue)
    }

    /**
     * Shakes [player]'s camera. Runs for [duration] ticks (`-1` = until [shouldContinue] returns false).
     * @param intensity max yaw/pitch offset magnitude
     * @param type easing applied over the duration
     */
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