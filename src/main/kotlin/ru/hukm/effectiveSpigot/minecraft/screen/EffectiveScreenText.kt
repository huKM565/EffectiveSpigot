package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Plain white text placed anywhere on a player's screen
 * ⚠️ **Experimental.** See [EffectiveScreenImage] — same caveats.
 * (see [EffectiveScreenElement]). The shader
 * shifts the whole string, keeping its layout, so that its centre lands at [x]/[y]. [scale] multiplies
 * the size the text would have in its transport (a title is 4× GUI text), quantized to 1/8 in
 * `0.125..4.0`. Blue `128..159` is reserved. Text is always white and unshadowed.
 *
 * ```kotlin
 * EffectiveScreenText.show(player, "Wave 3", x = 0.5f, y = 0.1f, scale = 2f)
 * EffectiveScreenText.show(player, "Go!", x = 0.5f, y = 0.5f, scale = 4f, durationTicks = 40)
 * EffectiveScreenText.hide(player)
 * ```
 *
 * Same requirements as [EffectiveScreenImage]: the plugin's resource pack must be enabled, one element
 * per player via the title slot, and shader packs ignore the placement.
 */
object EffectiveScreenText : EffectiveScreenElement() {
    private const val SCALE_STEPS = 32
    private const val STEPS_PER_UNIT_SCALE = 8

    override val marker = 0b100_00000

    /** [text] with the placement encoded in its color; put it in a title, action bar, boss bar, … */
    fun getComponent(text: String, x: Float, y: Float, scale: Float = 1f): Component {
        var step = Math.round(scale * STEPS_PER_UNIT_SCALE)
        if (step < 1) step = 1
        if (step > SCALE_STEPS) step = SCALE_STEPS
        return encode(Component.text(text), x, y, step - 1)
    }

    /** Shows [text] to [player] centred at [x]/[y] for [durationTicks]. */
    fun show(player: Player, text: String, x: Float, y: Float, scale: Float = 1f, durationTicks: Int = FOREVER_TICKS) =
        showTitle(player, getComponent(text, x, y, scale), durationTicks)
}
