package ru.hukm.effectiveSpigot.minecraft.menu

abstract class EffectiveTextureMenu : EffectiveMenu() {
    enum class BackSpace(val symbol: Char, val size: Int) {
        R128('\u2184', 128),
        R32('\u2182', 32),
        R4('\u2181', 4),
        R1('\u2180', 1);

        override fun toString(): String = symbol.toString()

        operator fun times(count: Int): String {
            return if (count <= 0) "" else symbol.toString().repeat(count)
        }
    }

    fun getBackspaces(pixels: Int): String {
        var remaining = pixels
        val result = StringBuilder()

        for (space in BackSpace.entries) {
            if (remaining >= space.size) {
                val count = remaining / space.size
                result.append(space * count)
                remaining %= space.size
            }
        }

        return result.toString()
    }

    final override fun getMenuTitle(): String {
        return getBackspaces(8) + getTextureSymbol()
    }

    abstract fun getTextureSymbol(): Char
}