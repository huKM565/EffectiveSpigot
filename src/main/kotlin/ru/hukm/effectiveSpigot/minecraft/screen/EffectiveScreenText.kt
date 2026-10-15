package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Plain white text placed anywhere on a player's screen
 * ⚠️ **Experimental.** See [EffectiveScreenImage] — same caveats.
 * (see [EffectiveScreenElement]). The shader
 * shifts the whole string, keeping its layout, so that its centre lands at [x]/[y]. [scale] multiplies
 * the size of normal GUI text (chat-sized), quantized to 1/4 in `0.25..16.0`; the default 4 is title-sized. Text is
 * always white and unshadowed.
 *
 * The text is drawn with the framework's own font ([FONT]): a copy of the vanilla Latin, accented and Cyrillic glyphs
 * whose textures carry the screen-text marker, built into the framework pack by [EffectiveScreenTextFont]. Ordinary text in the
 * default font is never moved, whatever its color. Characters missing from those vanilla bitmaps (CJK and other
 * unifont-only scripts) are not supported.
 *
 * ```kotlin
 * EffectiveScreenText.show(player, "Wave 3", x = 0.5f, y = 0.1f, scale = 2f)
 * EffectiveScreenText.show(player, "Go!", x = 0.5f, y = 0.5f, durationTicks = 40)
 * EffectiveScreenText.hide(player)
 * ```
 *
 * One text per player: showing again replaces it. It lives in its own boss bar next to the images
 * ([EffectiveScreenElement]). The shader finds the string's centre from how the client lays out a boss bar name
 * (centred on the screen, at normal GUI size), so [getComponent] lands right only as a boss bar name: in a title or an
 * action bar it ends up elsewhere. The framework pack must be enabled, and shader packs ignore the placement.
 */
object EffectiveScreenText : EffectiveScreenElement() {
    private const val STEPS_PER_UNIT_SCALE = 4
    private const val DEFAULT_SCALE = 4f

    /** The framework font whose glyphs the shader moves. */
    val FONT: Key = Key.key(EffectiveScreenTextFont.NAMESPACE, EffectiveScreenTextFont.NAME)

    /** [text] in the screen-text font with the placement encoded in its color; meant for a boss bar name. */
    fun getComponent(text: String, x: Float, y: Float, scale: Float = DEFAULT_SCALE): Component {
        val step = Math.round(scale * STEPS_PER_UNIT_SCALE)
        return withShaderData(Component.text(text).font(FONT), packPlacement(x, y, step))
    }

    /** Shows [text] to [player] centred at [x]/[y] for [durationTicks]. */
    fun show(player: Player, text: String, x: Float, y: Float, scale: Float = DEFAULT_SCALE, durationTicks: Int = FOREVER_TICKS) =
        showBar(player, getComponent(text, x, y, scale), durationTicks)
}
