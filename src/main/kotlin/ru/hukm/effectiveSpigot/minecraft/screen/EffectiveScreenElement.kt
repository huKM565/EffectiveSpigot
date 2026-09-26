package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Base for things drawn on a player's screen
 * ⚠️ **Experimental.** The core-shader technique and this API may change.
 * through the framework's `text.vsh` core shader: the
 * element's text color carries its placement (`R = x`, `G = y`, `B = marker | 5-bit step`), which the
 * shader decodes. Subclasses pick the [marker] and the meaning of the step (see [EffectiveScreenImage],
 * [EffectiveScreenText]). Coordinates are screen fractions `0.0..1.0`, top-left origin.
 */
abstract class EffectiveScreenElement {
    companion object {
        /** Longest title duration in ticks that still fits the millisecond conversion. */
        const val FOREVER_TICKS = Int.MAX_VALUE / 50

        private const val MILLIS_PER_TICK = 50L
        private const val COLOR_CHANNEL_MAX = 255
    }

    /** Top 3 bits of blue identifying this element kind to the shader; blue values with these bits are reserved. */
    protected abstract val marker: Int

    /** Encodes [x]/[y] and a 5-bit [step] into the color the shader reads; shadow is disabled so no ghost copy is drawn. */
    protected fun encode(component: Component, x: Float, y: Float, step: Int): Component {
        val red = Math.round(x * COLOR_CHANNEL_MAX)
        val green = Math.round(y * COLOR_CHANNEL_MAX)
        val blue = marker + step
        return component
            .color(TextColor.color(red, green, blue))
            .shadowColor(ShadowColor.none())
    }

    /** Shows [component] to [player] as a title for [durationTicks]. */
    protected fun showTitle(player: Player, component: Component, durationTicks: Int) {
        val stay = Duration.ofMillis(durationTicks * MILLIS_PER_TICK)
        val times = Title.Times.times(Duration.ZERO, stay, Duration.ZERO)
        player.showTitle(Title.title(component, Component.empty(), times))
    }

    /** Hides whatever this element showed via a title. */
    fun hide(player: Player) = player.clearTitle()

    /** Whether the element stays on [player]'s screen permanently (re-shown automatically on join/respawn/world change). */
    open fun isPersistent(player: Player): Boolean = false
}
