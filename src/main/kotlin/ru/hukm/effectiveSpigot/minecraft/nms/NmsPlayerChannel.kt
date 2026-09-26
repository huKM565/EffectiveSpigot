package ru.hukm.effectiveSpigot.minecraft.nms

import io.netty.channel.Channel
import org.bukkit.entity.Player

object NmsPlayerChannel {
    fun get(player: Player): Channel {
        val handle = CraftReflection.getPlayerHandle(player)
        val listener = NmsProxies.serverPlayer.connection(handle)
        val connection = NmsProxies.connection.networkConnection(listener)
        return NmsProxies.networkConnection.channel(connection) as Channel
    }
}
