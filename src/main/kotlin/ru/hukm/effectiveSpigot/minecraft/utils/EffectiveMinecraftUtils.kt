package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object EffectiveMinecraftUtils {
    @Deprecated("Use player.sendActionBar()")
    fun sendMessageToActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }
}
