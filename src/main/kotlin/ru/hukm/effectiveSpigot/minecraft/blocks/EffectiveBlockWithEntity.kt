package ru.hukm.effectiveSpigot.minecraft.blocks

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.events.event

/**
 * An [EffectiveBlock] that carries a [Marker] entity at its centre — a place to keep per-block data.
 *
 * On placement a marker (invisible, no hitbox) is spawned in the block; on break it is removed, and
 * removing the marker by other means removes the block. Reach it with [getMarkerEntity] and store
 * per-block state in its persistent data (owner, charge, contents, …) via
 * [ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils].
 *
 * ```kotlin
 * object Altar : EffectiveBlockWithEntity() {
 *     // …EffectiveBlock overrides…
 *     override fun onPlace(event: BlockPlaceEvent) {
 *         val marker = getMarkerEntity(event.blockPlaced) ?: return
 *         EffectiveDataContainerUtils.setContainerValue(marker, OWNER, event.player.uniqueId.toString())
 *     }
 * }
 * ```
 */
abstract class EffectiveBlockWithEntity : EffectiveBlock() {

    companion object {
        private val _namespacedKeyToEffectiveBlockWithEntity = hashMapOf<String, EffectiveBlockWithEntity>()
        val namespacedKeyToEffectiveBlockWithEntity get() = _namespacedKeyToEffectiveBlockWithEntity

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<EntityRemoveEvent> {
                        val entity = it.entity
                        if (entity.type != EntityType.MARKER || it.cause == EntityRemoveEvent.Cause.UNLOAD) return@event

                        val namespacedKey = EffectiveEntity.getNamespacedKeyByEntity(entity)
                        for (effectiveBlockWithEntity in namespacedKeyToEffectiveBlockWithEntity.values) {
                            if (namespacedKey == effectiveBlockWithEntity.entity.getNamespacedKey()) {
                                EffectiveSpigot.instance.launch {
                                    delay(1.ticks)
                                    entity.location.world.setBlockData(entity.location, Material.AIR.createBlockData())
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    val entity = object : EffectiveEntity() {
        override fun editEntity(entity: Entity) {}

        override fun getEntityType() = EntityType.MARKER

        override fun getNamespacedData() = this@EffectiveBlockWithEntity.getNamespacedData()
    }

    init {
        _namespacedKeyToEffectiveBlockWithEntity[getNamespacedName()] = this

        addPlaceHandler { event ->
            val block = event.blockPlaced
            val center = block.location.toCenterLocation()
            entity.spawnEntity(center)
        }
        addBreakHandler { event ->
            getMarkerEntity(event.block)?.remove()
        }
    }

    fun getMarkerEntity(block: Block): Marker? {
        return block.world.getEntitiesByClass(Marker::class.java)
            .filter {
                block.x == it.location.blockX &&
                        block.y == it.location.blockY &&
                        block.z == it.location.blockZ
            }
            .firstOrNull {
                EffectiveEntity.getNamespacedKeyByEntity(it) == this.entity.getNamespacedKey()
            }
    }
}
