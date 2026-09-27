package ru.hukm.effectiveSpigot.minecraft.network

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.nms.NmsPlayerChannel

/**
 * Puts an [EffectivePacketHandler] into every player's Netty pipeline, right before the vanilla
 * `packet_handler`, so it sees each outgoing packet as an object before it is encoded to bytes.
 *
 * Injection runs on the channel's event loop: packets a plugin sends from the main thread are queued to that
 * same loop, so the handler is in place before anything sent after [PlayerJoinEvent] reaches the channel.
 */
internal object EffectivePacketHook {
    private const val HANDLER_NAME = "effective_spigot"
    private const val PACKET_HANDLER = "packet_handler"

    fun getModule(): IModule = object : IModule {
        override fun init() {
            event<PlayerJoinEvent>(EventPriority.LOWEST) { inject(it.player) }
            Bukkit.getOnlinePlayers().forEach(::inject)
        }
    }

    /** The handler in [player]'s pipeline, or null if they have none (not injected yet, or a fake player). */
    fun getHandler(player: Player): EffectivePacketHandler? =
        NmsPlayerChannel.get(player)?.pipeline()?.get(HANDLER_NAME) as? EffectivePacketHandler

    private fun inject(player: Player) {
        val channel = NmsPlayerChannel.get(player) ?: return
        channel.eventLoop().execute {
            val pipeline = channel.pipeline()
            if (pipeline.get(HANDLER_NAME) == null && pipeline.get(PACKET_HANDLER) != null) {
                pipeline.addBefore(PACKET_HANDLER, HANDLER_NAME, EffectivePacketHandler())
            }
        }
    }

    /** Takes the handler out of every online player's pipeline; called when the framework disables. */
    fun removeAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            val channel = NmsPlayerChannel.get(player) ?: continue
            channel.eventLoop().execute {
                if (channel.pipeline().get(HANDLER_NAME) != null) channel.pipeline().remove(HANDLER_NAME)
            }
        }
    }
}
