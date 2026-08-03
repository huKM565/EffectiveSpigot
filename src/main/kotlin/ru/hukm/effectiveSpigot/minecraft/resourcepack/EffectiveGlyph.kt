package ru.hukm.effectiveSpigot.minecraft.resourcepack

/**
 * A single bitmap glyph in the generated resource pack: a character bound to a texture, drawn inline in
 * chat/GUI text. Used for custom fonts and texture menus (see [ru.hukm.effectiveSpigot.minecraft.menu.EffectiveTextureMenu]).
 *
 * @property char the character that renders as this glyph
 * @property texturePath resource path to the texture inside the plugin jar
 * @property height glyph height in pixels
 * @property ascent vertical baseline offset in pixels (must be ≤ [height])
 */
data class EffectiveGlyph(
    val char: Char,
    val texturePath: String,
    val height: Int = 8,
    val ascent: Int = 7
)
