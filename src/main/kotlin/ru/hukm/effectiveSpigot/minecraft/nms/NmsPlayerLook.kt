package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.entity.Player

object NmsPlayerLook {
    fun sendRelativeLook(player: Player, yawOffset: Float, pitchOffset: Float) {
        val handle = CraftReflection.getPlayerHandle(player)
        val connection = NmsProxies.serverPlayer.connection(handle)
        val packet = NmsProxies.rotationPacket.create(yawOffset, true, pitchOffset, true)

        NmsProxies.connection.send(connection, packet)
    }
}
