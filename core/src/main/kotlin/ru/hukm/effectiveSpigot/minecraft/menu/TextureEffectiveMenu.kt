package ru.hukm.effectiveSpigot.minecraft.menu

abstract class TextureEffectiveMenu : EffectiveMenu() {
    val REVERSE_PIXLES_TO_SYMBOL = hashMapOf<Int, Char>(
        -1 to '\u2180',
        -4 to '\u2181',
        -32 to '\u2182',
        -128 to '\u2184'
    )

    final override fun getMenuTitle(): String {
        return getTextureSymbol().toString()
    }

    abstract fun getTextureSymbol(): Char
}