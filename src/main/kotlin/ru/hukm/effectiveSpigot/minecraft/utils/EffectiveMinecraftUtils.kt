package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object EffectiveMinecraftUtils {
    fun sendMessageToActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }
}
