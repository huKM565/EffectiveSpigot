package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Misc Minecraft helpers. */
object EffectiveMinecraftUtils {
    /** @deprecated use [Player.sendActionBar] directly. */
    @Deprecated("Use player.sendActionBar()")
    fun sendMessageToActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }
}
