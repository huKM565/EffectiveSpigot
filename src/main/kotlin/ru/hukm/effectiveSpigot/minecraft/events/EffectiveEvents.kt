package ru.hukm.effectiveSpigot.minecraft.events

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot

/**
 * Registers a Bukkit listener for event type [T] with a lambda, inferring the event class from [T].
 *
 * ```kotlin
 * plugin.event<PlayerJoinEvent> { it.joinMessage(null) }
 * ```
 *
 * @param priority event priority
 * @param ignoreCancelled skip the handler if the event is already cancelled
 * @return the created [Listener] (unregister with [unregister])
 */
inline fun <reified T : Event> JavaPlugin.event(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: (T) -> Unit
): Listener {
    val listener = object : Listener {}

    server.pluginManager.registerEvent(
        T::class.java,
        listener,
        priority,
        { _, event -> if (event is T) handler(event) },
        this,
        ignoreCancelled
    )

    return listener
}

/** [event] registered against the EffectiveSpigot plugin instance (for use outside a plugin class). */
inline fun <reified T : Event> event(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: (T) -> Unit
): Listener = EffectiveSpigot.instance.event<T>(priority, ignoreCancelled, handler)

/** Unregisters all handlers of this listener. */
fun Listener.unregister() {
    HandlerList.unregisterAll(this)
}
