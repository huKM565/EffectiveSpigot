package ru.hukm.effectiveSpigot.minecraft.events

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot

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

inline fun <reified T : Event> event(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: (T) -> Unit
): Listener = EffectiveSpigot.instance.event<T>(priority, ignoreCancelled, handler)

fun Listener.unregister() {
    HandlerList.unregisterAll(this)
}
