package ru.hukm.effectiveSpigot.minecraft.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

object EffectiveMinecraftUtils {
    fun sendMessageToActionBar(player: Player, message: String, color: ChatColor) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message, color))
    }
}