package ru.hukm.effectiveSpigot.minecraft.menu

import net.md_5.bungee.api.ChatColor

abstract class EffectiveTextureMenu : EffectiveMenu() {
    enum class BackSpace(val symbol: Char) {
        R1('\u2180'),
        R4('\u2181'),
        R32('\u2182'),
        R128('\u2184');

        operator fun invoke() = symbol.toString()
    }

    final override fun getMenuTitle(): String {
        return ChatColor.WHITE.toString() + (getTextureSymbol().toString())
    }

    abstract fun getTextureSymbol(): Char
}