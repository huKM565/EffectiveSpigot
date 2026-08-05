package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/** Misc Minecraft helpers. */
object EffectiveMinecraftUtils {
    /** @deprecated use [Player.sendActionBar] directly. */
    @Deprecated("Use player.sendActionBar()")
    fun sendMessageToActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }

    /**
     * The plugin's asset namespace: its name lowercased with any character outside `[a-z0-9._-]` replaced
     * by `_`. Used for the generated resource pack's asset paths and item-model ids, so it is safe to use
     * in a [org.bukkit.NamespacedKey] / `item_model` reference (e.g. to auto-attach an item model to a stack).
     */
    fun getNamespace(instance: JavaPlugin): String =
        instance.name.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
}
