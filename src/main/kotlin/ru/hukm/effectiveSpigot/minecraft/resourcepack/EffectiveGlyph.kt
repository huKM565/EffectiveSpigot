package ru.hukm.effectiveSpigot.minecraft.resourcepack

/**
 * A single bitmap glyph in the generated resource pack: an [EffectiveFontChar] bound to a texture, drawn
 * inline in chat/GUI text. Used for custom fonts and texture menus (see [ru.hukm.effectiveSpigot.minecraft.menu.EffectiveTextureMenu]).
 *
 * ⚠️ Keep the texture image reasonably small. Minecraft caps the size of a bitmap-font texture, and an
 * oversized image (e.g. 4096×4096) simply won't load — the glyph then renders as a missing-character box
 * (tofu) instead of the texture. Downscale the source PNG if a glyph doesn't show up.
 *
 * @property charGlyph the [EffectiveFontChar] handle whose reserved codepoint renders as this glyph
 * @property texturePath resource path to the texture inside the plugin jar
 * @property height glyph height in pixels
 * @property ascent vertical baseline offset in pixels (must be ≤ [height])
 */
data class EffectiveGlyph(
    val texturePath: String,
    val height: Int = 8,
    val ascent: Int = 7
) {
    val charGlyph = EffectiveFontChar.getNextFree()
}
