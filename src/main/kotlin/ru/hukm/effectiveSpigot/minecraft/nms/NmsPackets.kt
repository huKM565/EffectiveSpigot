package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.entity.Player

/**
 * Packets sent to a single player to show them something that is not true on the server: an arbitrary fill in
 * their experience bar.
 *
 * Nothing here changes server state, so a player who reconnects sees reality again.
 */
object NmsPackets {
    /** Shows [progress] `0.0..1.0` in the experience bar, with [level] above it, leaving the real experience alone. */
    fun sendExperience(player: Player, progress: Float, totalExperience: Int, level: Int) {
        val packet = NmsProxies.experiencePacket.create(progress.coerceIn(0f, 1f), totalExperience, level)

        send(player, packet)
    }

    private fun send(player: Player, packet: Any) {
        NmsProxies.connection.send(NmsProxies.serverPlayer.connection(CraftReflection.getPlayerHandle(player)), packet)
    }
}
