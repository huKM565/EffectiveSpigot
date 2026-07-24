package ru.hukm.effectiveSpigot.minecraft.resourcepack

data class EffectiveGlyph(
    val char: Char,
    val texturePath: String,
    val height: Int = 8,
    val ascent: Int = 7
)
