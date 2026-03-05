package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Bukkit
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
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.InteractCallback
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
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
        fun cacheEntity(chunkIdentifier: ChunkIdentifier, entity: Entity) {
            val entities = chunkToEntities.getOrPut(chunkIdentifier) { arrayListOf() }
            entities.add(entity)
        }

        fun equalByNamespacedKey(entity1: Entity?, entity2: Entity?): Boolean {
            val value1 = getNamespacedKeyByEntity(entity1) ?: return false
            val value2 = getNamespacedKeyByEntity(entity2) ?: return false

            return value1 == value2
        }

        fun getNamespacedKeyByEntity(entity: Entity?): String? {
            return if (entity != null) {
                EffectiveDataContainerUtils.getContainerValue(entity, ENTITY_KEY, PersistentDataType.STRING)
            } else {
                return null
            }
        }

        fun equalByNamespacedKeyIfExistElseByEntityType(entity1: Entity?, entity2: Entity?): Boolean {
            val key1 = getNamespacedKeyByEntity(entity1)
            val key2 = getNamespacedKeyByEntity(entity2)

            if (key1 != null && key2 != null){
                return equalByNamespacedKey(entity1, entity2)
            }

            if (key1 == key2) {
                return entity1?.type == entity2?.type
            }

            return false
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
        //TODO(Сделать, чтобы нельзя было использовать названия обычных энтити)
        if (namespacedKeyToEntity.containsKey(getNamespacedKey())) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.entities.already_registered", getNamespacedKey()))
        }
        namespacedKeyToEntity[getNamespacedKey()] = this
    }

    fun createEntity(): Entity {
        val world = Bukkit.getWorlds()[0]
        val location = Location(world, 0.0, 0.0, 0.0)
        val typeClass = getEntityType().entityClass

        val entity = world.createEntity(location, typeClass as Class<out Entity>)
        editEntity(entity)

        EffectiveDataContainerUtils.setContainerValue(
            entity,
            ENTITY_KEY,
            PersistentDataType.STRING,
            getNamespacedKey()
        )

        return entity
    }

    fun spawnEntity(location: Location): Entity {
        val world = location.world ?: throw IllegalArgumentException("Location world cannot be null")

        val entity = createEntity()

        val chunk = location.chunk
        val chunkIdentifier = ChunkIdentifier(
            world.uid,
            EffectiveUtils.twoIntToLong(chunk.x, chunk.z)
        )

        cacheEntity(chunkIdentifier, entity)



        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    fun addInteractHandler(
        click: Click,
        callback: InteractCallback,
        cooldownData: EffectiveAbstractInteract.CooldownData<EffectiveEntityInteractable.EventsCallOptions>? = null
    ) {
        EffectiveEntityInteractable.addInteractHandler(
            createEntity(),
            click,
            callback,
            cooldownData
        )
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
                    cacheEntity(chunkIdentifier, it)
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