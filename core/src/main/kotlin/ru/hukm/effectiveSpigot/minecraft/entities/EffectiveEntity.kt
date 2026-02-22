package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.utils.EffectiveUtils
import java.util.*

abstract class EffectiveEntity {
    data class ChunkIdentifier(
        val worldUID: UUID,
        val chunkKey: Long
    )

    companion object {
        private val ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "entity")

        val namespacedKeyToEntity = hashMapOf<String, EffectiveEntity>()
        val chunkToEntities = hashMapOf<ChunkIdentifier, MutableList<Entity>>()
        fun addEntity(chunkIdentifier: ChunkIdentifier, entity: Entity) {
            val entities = chunkToEntities.getOrPut(chunkIdentifier) { arrayListOf() }
            entities.add(entity)
        }

        fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }
    }

    init {
        if (namespacedKeyToEntity.containsKey(getNamespacedKey())) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.item_already_registered", getNamespacedKey()))
        }
        namespacedKeyToEntity[getNamespacedKey()] = this
    }

    fun spawnEntity(location: Location): Entity {
        val world = location.world ?: throw IllegalArgumentException("Location world cannot be null")
        return world.spawnEntity(location, getEntityType()).also {
            editEntity(it)
            EffectiveDataContainerUtils.setContainerValue(
                it,
                ENTITY_KEY,
                PersistentDataType.STRING,
                getNamespacedKey()
            )

            val chunk = location.chunk
            val chunkIdentifier = ChunkIdentifier(
                world.uid,
                EffectiveUtils.twoIntToLong(chunk.x, chunk.z)
            )

            addEntity(chunkIdentifier, it)
        }
    }


    abstract fun editEntity(entity: Entity)
    abstract fun getEntityType(): EntityType
    abstract fun getNamespacedKey(): String

    class Events(): Listener {
        @EventHandler
        fun onChunkLoad(event: ChunkLoadEvent) {
            val chunk = event.chunk
            val chunkIdentifier = ChunkIdentifier(
                event.world.uid,
                EffectiveUtils.twoIntToLong(chunk.x, chunk.z)
            )

            chunk.entities.forEach {
                val key = EffectiveDataContainerUtils.getContainerValue(
                    it,
                    ENTITY_KEY,
                    PersistentDataType.STRING
                )
                if (key != null) {
                    addEntity(chunkIdentifier, it)
                }
            }
        }

        @EventHandler
        fun onChunkUnload(event: ChunkUnloadEvent) {
            val chunk = event.chunk
            val chunkIdentifier = ChunkIdentifier(
                event.world.uid,
                EffectiveUtils.twoIntToLong(chunk.x, chunk.z)
            )

            chunkToEntities.remove(chunkIdentifier)
        }
    }
}