package ru.hukm.effectiveSpigot.minecraft.zone

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import kotlin.math.max
import kotlin.math.min

import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItems
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveParticles
import java.util.UUID

object EffectiveZoneRenderer {
    private val UUIDToJob = hashMapOf<UUID, Job>()

    fun startRendering(selectionOrZoneBox: Triple<EffectiveBlockPos?, EffectiveBlockPos?, UUID>?, color: Color? = null) {
        startRendering(selectionOrZoneBox, null, color)
    }

    fun startRendering(selectionOrZoneBox: Triple<EffectiveBlockPos?, EffectiveBlockPos?, UUID>?, selectionOwnerUUID: UUID?, color: Color? = null) {
        if (selectionOwnerUUID != null) {
            UUIDToJob[selectionOwnerUUID]?.cancel()
        }

        val finalColor = color ?: Color.fromRGB(255, 255, 255)
        val dustOptions = DustOptions(finalColor, 1.0f)

        val region = selectionOrZoneBox ?: return

        val pos1 = region.first
        val pos2 = region.second
        val worldUUID = region.third

        if (pos1 == null || pos2 == null) {
            return
        }

        val job = EffectiveSpigot.instance.launch {
            while (true) {
                val players = Bukkit.getOnlinePlayers().filter { player ->
                    val key = EffectiveItem.getNamespacedKeyByItem(
                        EffectiveInventoryUtils.getUsedItemFromHands(player)
                    )
                    val isViewer = key == EffectiveItems.ZONE_SELECTOR.item.getNamespacedName()
                        || key in ZoneRegisteringSelector.registeredItemKeys
                    isViewer && player.world.uid == worldUUID
                }

                if (players.isNotEmpty()) renderZone(players, pos1, pos2, dustOptions)

                delay(5.ticks)
            }
        }

        if (selectionOwnerUUID != null) {
            UUIDToJob[selectionOwnerUUID] = job
        }
    }

    fun stopRendering(uuid: UUID): Boolean {
        val job = UUIDToJob[uuid] ?: return false

        job.cancel()
        UUIDToJob.remove(uuid)

        return true
    }

    private fun renderZone(players: Collection<Player>, pos1: EffectiveBlockPos, pos2: EffectiveBlockPos, dustOptions: DustOptions) {
        val minX = min(pos1.x, pos2.x).toDouble()
        val minY = min(pos1.y, pos2.y).toDouble()
        val minZ = min(pos1.z, pos2.z).toDouble()

        val maxX = max(pos1.x, pos2.x).toDouble() + 1.0
        val maxY = max(pos1.y, pos2.y).toDouble() + 1.0
        val maxZ = max(pos1.z, pos2.z).toDouble() + 1.0

        EffectiveParticles.drawBox(players, minX, minY, minZ, maxX, maxY, maxZ, dustOptions)
    }
}