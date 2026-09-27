package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Packets sent to a single player to show them something that is not true on the server: an entity outline only
 * they see, or an arbitrary fill in their experience bar.
 *
 * Nothing here changes server state, so a player who reconnects sees reality again.
 */
object NmsPackets {
    /** Index of the shared-flags byte in the entity data of `net.minecraft.world.entity.Entity`. */
    private const val SHARED_FLAGS_ID = 0

    /** Bit of the shared-flags byte that makes the client draw an outline around the entity. */
    private const val GLOWING_FLAG = 0x40

    private val glowing = HashMap<UUID, MutableMap<Int, UUID>>()

    private val byteSerializer: Any by lazy {
        val serializers = Class.forName(NmsProxies.remapper.remapClassName("net.minecraft.network.syncher.EntityDataSerializers"))
        serializers.getDeclaredField(NmsProxies.remapper.remapFieldName(serializers, "BYTE"))
            .apply { isAccessible = true }
            .get(null)
    }

    /**
     * Outlines [entity] for [viewer] alone: the glowing bit is set on top of the flags the entity really has, so
     * `entity.isGlowing` stays false and no one else sees anything.
     *
     * The server keeps sending the real flags whenever the entity changes state (starts burning, sneaks, sprints),
     * and such an update overwrites the outline on the client — call [refreshGlow], or this method again, every few
     * ticks for an outline that has to last. Team colours are not involved, so the outline is always white.
     */
    fun showGlow(viewer: Player, entity: Entity) {
        sendSharedFlags(viewer, entity.entityId, (sharedFlags(entity).toInt() or GLOWING_FLAG).toByte())
        glowing.getOrPut(viewer.uniqueId) { HashMap() }[entity.entityId] = entity.uniqueId
    }

    /** Removes the outline again, restoring the flags [entity] really has. */
    fun hideGlow(viewer: Player, entity: Entity) {
        if (glowing[viewer.uniqueId]?.remove(entity.entityId) == null) return

        sendSharedFlags(viewer, entity.entityId, sharedFlags(entity))
    }

    /** Removes every outline [viewer] was shown. Entities that no longer exist are simply dropped. */
    fun hideAllGlow(viewer: Player) {
        val entities = glowing.remove(viewer.uniqueId) ?: return

        for ((entityId, entityUuid) in entities) {
            val entity = Bukkit.getEntity(entityUuid) ?: continue
            sendSharedFlags(viewer, entityId, sharedFlags(entity))
        }
    }

    /** Re-sends every outline [viewer] currently has, undoing metadata updates that overwrote them. */
    fun refreshGlow(viewer: Player) {
        val entities = glowing[viewer.uniqueId] ?: return

        for ((entityId, entityUuid) in entities) {
            val entity = Bukkit.getEntity(entityUuid) ?: continue
            sendSharedFlags(viewer, entityId, (sharedFlags(entity).toInt() or GLOWING_FLAG).toByte())
        }
    }

    /** Whether [viewer] is being shown an outline on [entity]. */
    fun isGlowShown(viewer: Player, entity: Entity): Boolean = glowing[viewer.uniqueId]?.containsKey(entity.entityId) == true

    /** Drops the outlines remembered for [viewer] without sending anything; call it when the player leaves. */
    fun forgetGlow(viewer: Player) {
        glowing.remove(viewer.uniqueId)
    }

    /** Shows [progress] `0.0..1.0` in the experience bar, with [level] above it, leaving the real experience alone. */
    fun sendExperience(player: Player, progress: Float, totalExperience: Int, level: Int) {
        val packet = NmsProxies.experiencePacket.create(progress.coerceIn(0f, 1f), totalExperience, level)

        send(player, packet)
    }

    /** The shared-flags byte [entity] actually has: on fire, sneaking, sprinting, swimming, invisible, glowing, gliding. */
    private fun sharedFlags(entity: Entity): Byte {
        val handle = CraftReflection.getEntityHandle(entity)
        var flags = 0

        for (bit in 0..7) if (NmsProxies.entityFlags.getSharedFlag(handle, bit)) flags = flags or (1 shl bit)

        return flags.toByte()
    }

    private fun sendSharedFlags(viewer: Player, entityId: Int, flags: Byte) {
        val value = NmsProxies.dataValue.create(SHARED_FLAGS_ID, byteSerializer, flags)

        send(viewer, NmsProxies.entityDataPacket.create(entityId, listOf(value)))
    }

    private fun send(player: Player, packet: Any) {
        NmsProxies.connection.send(NmsProxies.serverPlayer.connection(CraftReflection.getPlayerHandle(player)), packet)
    }
}
