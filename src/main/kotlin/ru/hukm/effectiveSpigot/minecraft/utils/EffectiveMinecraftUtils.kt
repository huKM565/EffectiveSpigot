package ru.hukm.effectiveSpigot.minecraft.utils

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import ru.hukm.effectiveSpigot.minecraft.nms.CraftReflection
import ru.hukm.effectiveSpigot.minecraft.nms.NmsProxies
import kotlin.math.cos
import kotlin.math.sin

/** Misc Minecraft helpers. */
object EffectiveMinecraftUtils {
    /** The sound event key of [sound] as a namespaced string, e.g. `"minecraft:block.wood.place"`. */
    fun getSoundKey(sound: Sound): String =
        Registry.SOUNDS.getKey(sound)?.asString() ?: error("Unregistered sound: $sound")

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

    /**
     * The server's Minecraft version as one comparable number `major * 10000 + minor * 100 + patch`, from
     * [Bukkit.getMinecraftVersion]: `1.21.4` → `12104`, `26.3` → `260300`, `26.3.1` → `260301`.
     */
    @JvmStatic
    val minecraftVersion: Int by lazy {
        val parts = Bukkit.getMinecraftVersion().split(".").map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        parts.getOrElse(0) { 0 } * 10000 + parts.getOrElse(1) { 0 } * 100 + parts.getOrElse(2) { 0 }
    }

    /** Whether the server runs Minecraft [major].[minor].[patch] or newer, e.g. `isVersionAtLeast(26, 3)`. */
    @JvmStatic
    @JvmOverloads
    fun isVersionAtLeast(major: Int, minor: Int, patch: Int = 0): Boolean =
        minecraftVersion >= major * 10000 + minor * 100 + patch

    /**
     * Yaw of the entity's body, which is not the same as `entity.location.yaw`: for a player that one is the yaw
     * of the camera, and the body lags behind it and catches up over several ticks. For a mob both are the body.
     *
     * Use it to ask where an entity's back really is — a player who only whipped the camera around has not turned
     * yet. The matching facing vector is `getBodyDirection`.
     */
    @JvmStatic
    fun getBodyYaw(entity: LivingEntity): Float =
        NmsProxies.livingEntity.getPreciseBodyRotation(CraftReflection.getEntityHandle(entity), 1.0f)

    /** Horizontal unit vector the entity's body faces, from [getBodyYaw]. */
    @JvmStatic
    fun getBodyDirection(entity: LivingEntity): Vector {
        val radians = Math.toRadians(getBodyYaw(entity).toDouble())

        return Vector(-sin(radians), 0.0, cos(radians))
    }
}
