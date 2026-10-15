package ru.hukm.effectiveSpigot.minecraft.screen

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import java.awt.image.BufferedImage
import java.util.UUID

/**
 * Base for things drawn on a player's screen through the framework's `text.vsh` core shader.
 * ⚠️ **Experimental.** The core-shader technique and this API may change.
 *
 * The shader never looks at the text color to decide what to move: it recognises an element by a marker in its glyph
 * texture. The framework stamps the four corner pixels of every glyph it owns with an almost transparent color
 * ([EffectiveScreenImage]: `RGBA 37, 13, 71, 1` plus the tile's position in the picture next to each corner;
 * [EffectiveScreenText]: `RGBA 37, 13, 100 + n, 1`, where `n` is the distance in pixels from that corner to the text
 * line, so the shader can find the line), so ordinary text of any color is left alone. The whole 24-bit text color
 * then carries the placement: x and y 9 bits each (a step of 1/512 of the screen) and the size 6 bits. Coordinates are
 * screen fractions `0.0..1.0`, top-left origin.
 *
 * Every element shown to a player shares ONE boss bar per player whose name is the concatenation of all shown
 * elements' glyphs, so any number of elements stay on screen together, each until its own [hide] or duration, and
 * the title slot stays free. The bar itself is invisible: the framework pack makes the green boss bar sprites
 * transparent and the elements use green bars without notches. Consequences:
 * - green boss bars of every other plugin become invisible too (vanilla uses pink, purple and red);
 * - only one bar is ever shown, so the client's ~6-bar drawing limit no longer caps the element count;
 * - real boss bars (ender dragon, wither, raids) shown after an element are drawn below it.
 */
abstract class EffectiveScreenElement {
    companion object {
        /** Duration that means «until [hide]»: no timer is started. */
        const val FOREVER_TICKS = Int.MAX_VALUE / 50

        internal const val IMAGE_MARKER_ARGB = 0x01250D47
        internal const val ANIMATION_MARKER_ARGB = 0x01250D46
        private const val TEXT_MARKER_BASE_ARGB = 0x01250D64

        private const val POSITION_MAX = 511
        private const val SIZE_STEPS = 64

        private class Shown(val component: Component, val expiry: Job?)

        private val shown = HashMap<UUID, LinkedHashMap<EffectiveScreenElement, Shown>>()
        private val bars = HashMap<UUID, BossBar>()

        /** Marker for a screen-text glyph corner [lineOffset] pixels away from the text line. */
        internal fun textMarkerArgb(lineOffset: Int): Int = TEXT_MARKER_BASE_ARGB + lineOffset

        /** Stamps the four corner pixels of the [width]×[height] cell at [x], [y] of [image] with [argb]. */
        internal fun stampCorners(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, argb: Int) =
            stampCorners(image, x, y, width, height, argb, argb)

        /** Stamps the top corners of the cell with [topArgb] and the bottom corners with [bottomArgb]. */
        internal fun stampCorners(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, topArgb: Int, bottomArgb: Int) {
            val right = x + width - 1
            val bottom = y + height - 1
            image.setRGB(x, y, topArgb)
            image.setRGB(right, y, topArgb)
            image.setRGB(x, bottom, bottomArgb)
            image.setRGB(right, bottom, bottomArgb)
        }

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                event<PlayerQuitEvent> { quit ->
                    shown.remove(quit.player.uniqueId)?.values?.forEach { it.expiry?.cancel() }
                    bars.remove(quit.player.uniqueId)
                }
            }
        }
    }

    /**
     * Packs [x], [y] (screen fractions) and a size [step] in `1..64` into the 24-bit color: `x9 | y9 | (step - 1)6`.
     */
    protected fun packPlacement(x: Float, y: Float, step: Int): Int {
        val packedX = Math.round(x * POSITION_MAX).coerceIn(0, POSITION_MAX)
        val packedY = Math.round(y * POSITION_MAX).coerceIn(0, POSITION_MAX)
        return (packedX shl 15) or (packedY shl 6) or (step.coerceIn(1, SIZE_STEPS) - 1)
    }

    /** Puts the shader data [rgb] into [component]'s color; shadow is disabled so no ghost copy is drawn. */
    protected fun withShaderData(component: Component, rgb: Int): Component =
        component
            .color(TextColor.color(rgb))
            .shadowColor(ShadowColor.none())

    /**
     * Shows [component] to [player] as this element's boss bar for [durationTicks] ([FOREVER_TICKS]: until [hide]).
     * Showing the element again replaces its content and restarts the duration.
     */
    protected fun showBar(player: Player, component: Component, durationTicks: Int) {
        val elements = shown.getOrPut(player.uniqueId) { LinkedHashMap() }
        val bar = bars.getOrPut(player.uniqueId) {
            BossBar.bossBar(Component.empty(), 0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS).also { player.showBossBar(it) }
        }
        elements[this]?.expiry?.cancel()
        val expiry = if (durationTicks >= FOREVER_TICKS) null else EffectiveSpigot.instance.launch {
            delay(durationTicks.ticks)
            hide(player)
        }
        elements[this] = Shown(component, expiry)
        bar.name(concatenate(elements))
    }

    /** Removes this element from [player]'s screen; other elements stay. */
    fun hide(player: Player) {
        val elements = shown[player.uniqueId] ?: return
        val current = elements.remove(this) ?: return
        current.expiry?.cancel()
        if (elements.isEmpty()) {
            shown.remove(player.uniqueId)
            bars.remove(player.uniqueId)?.let { player.hideBossBar(it) }
        } else {
            bars[player.uniqueId]?.name(concatenate(elements))
        }
    }

    private fun concatenate(elements: Map<EffectiveScreenElement, Shown>): Component {
        var name = Component.empty()
        for (entry in elements.values) {
            name = name.append(entry.component)
        }
        return name
    }
}
