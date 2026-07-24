package ru.hukm.effectiveSpigot.minecraft.menu

import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack

abstract class EffectiveTextureMenu : EffectiveMenu() {
    enum class BackSpace(val symbol: Char, val size: Int) {
        R128('ↄ', 128),
        R32('ↂ', 32),
        R4('ↁ', 4),
        R1('ↀ', 1);

        override fun toString(): String = symbol.toString()

        operator fun times(count: Int): String {
            return if (count <= 0) "" else symbol.toString().repeat(count)
        }
    }

    companion object {
        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveResourcepack.addSpaceProvider(
                        EffectiveSpigot.instance,
                        BackSpace.entries.associate { it.symbol to -it.size }
                    )
                }
            }
        }
    }

    init {
        EffectiveResourcepack.addGlyph(getNamespacedData().first, getTextureGlyph())
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
        return getBackspaces(8) + getTextureGlyph().char
    }

    abstract fun getTextureGlyph(): EffectiveGlyph
}
