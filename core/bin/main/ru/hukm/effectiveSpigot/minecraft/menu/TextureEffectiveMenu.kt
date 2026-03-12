package ru.hukm.effectiveSpigot.minecraft.menu

import net.md_5.bungee.api.ChatColor
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem;
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.Material

abstract class TextureEffectiveMenu : EffectiveMenu() { 
    val NO_TEXTURE_ITEM = object : EffectiveItem() {
        override fun editMeta(meta: ItemMeta) {
            meta.customModelData(1)
        }
        override fun getMaterial(): Material {
            return Material.FIREWORK_STAR
        }
        override fun getNamespacedName(): String {
            return "no_texture"
        }
    }

    val REVERSE_PIXLES_TO_SYMBOL = hashMapOf<Int, Char>(
        -1 to '\u2180',
        -4 to '\u2181',
        -32 to '\u2182',
        -128 to '\u2184'
    )

    final override fun getMenuTitle(): String {
        return ChatColor.WHITE.toString() + (getTextureSymbol().toString())
    }

    abstract fun getTextureSymbol(): Char
}