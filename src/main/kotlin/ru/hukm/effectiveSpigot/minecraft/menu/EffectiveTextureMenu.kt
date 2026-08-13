package ru.hukm.effectiveSpigot.minecraft.menu

import net.md_5.bungee.api.ChatColor
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveFontChar
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import kotlin.math.abs

/**
 * An [EffectiveMenu] whose title renders a custom resource-pack texture instead of plain text.
 *
 * The subclass provides a [getTextureGlyph]; the menu registers it with the resource pack and builds
 * a title that shifts the cursor by [getTitleOffset] pixels and then draws the glyph. Positioning is
 * done with space glyphs — [Forward] shifts right (positive advance), [BackSpace] shifts left.
 * Override [getTitleOffset] for a simple shift, or [getMenuTitle] itself for full control.
 */
abstract class EffectiveTextureMenu : EffectiveMenu() {
    /** Negative-space glyphs of fixed pixel widths, used to shift the title cursor left. */
    enum class BackSpace(val symbol: EffectiveFontChar, val size: Int) {
        R128(EffectiveFontChar.getNextFree(), 128),
        R32(EffectiveFontChar.getNextFree(), 32),
        R4(EffectiveFontChar.getNextFree(), 4),
        R1(EffectiveFontChar.getNextFree(), 1);

        override fun toString(): String = symbol().toString()

        /** Repeats this back-space glyph [count] times (empty for non-positive counts). */
        operator fun times(count: Int): String {
            return if (count <= 0) "" else symbol().toString().repeat(count)
        }
    }

    /** Positive-space glyphs of fixed pixel widths, used to shift the title cursor right. */
    enum class Forward(val symbol: EffectiveFontChar, val size: Int) {
        R128(EffectiveFontChar.getNextFree(), 128),
        R32(EffectiveFontChar.getNextFree(), 32),
        R4(EffectiveFontChar.getNextFree(), 4),
        R1(EffectiveFontChar.getNextFree(), 1);

        override fun toString(): String = symbol().toString()

        /** Repeats this forward glyph [count] times (empty for non-positive counts). */
        operator fun times(count: Int): String {
            return if (count <= 0) "" else symbol().toString().repeat(count)
        }
    }

    companion object {
        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveResourcepack.addSpaceProvider(
                        EffectiveSpigot.instance,
                        BackSpace.entries.associate { it.symbol to -it.size } +
                            Forward.entries.associate { it.symbol to it.size }
                    )
                }
            }
        }
    }

    /** [getTextureGlyph] resolved once, so registration and the title share the same glyph codepoint. */
    private val titleGlyph by lazy { getTextureGlyph() }

    /** Texture pixel width — used to pull the title back to the texture's left edge after the glyph. */
    private val titleGlyphAdvance = EffectiveResourcepack.getImageWidth(getNamespacedData().first, titleGlyph.texturePath)

    init {
        EffectiveResourcepack.addGlyph(getNamespacedData().first, titleGlyph)
    }

    /**
     * Cursor shift (px) applied before drawing the texture: **positive shifts right**, **negative shifts
     * left**. Default `-8` reproduces the previous left shift; override to reposition the menu texture.
     */
    open fun getTitleOffset(): Int = -8

    /**
     * Builds a string of space glyphs shifting the cursor by [pixels]: positive → right (forward),
     * negative → left (back-space), `0` → empty.
     */
    fun getBackspaces(pixels: Int): String {
        if (pixels == 0) return ""

        val result = StringBuilder()
        var remaining = abs(pixels)

        if (pixels > 0) {
            for (space in Forward.entries) {
                if (remaining >= space.size) {
                    result.append(space * (remaining / space.size))
                    remaining %= space.size
                }
            }
        } else {
            for (space in BackSpace.entries) {
                if (remaining >= space.size) {
                    result.append(space * (remaining / space.size))
                    remaining %= space.size
                }
            }
        }

        return result.toString()
    }

    override fun getMenuTitle(): String {
        return getBackspaces(getTitleOffset()) +
                ChatColor.WHITE.toString() + titleGlyph.charGlyph() +
                getBackspaces(-titleGlyphAdvance + 7) +
                ChatColor.WHITE.toString() + getTitle().orEmpty()
    }

    open fun getTitle(): String? = null

    /** The resource-pack glyph rendered as this menu's title texture. */
    abstract fun getTextureGlyph(): EffectiveGlyph
}
