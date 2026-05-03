package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityLookable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.InteractCallback
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import java.util.*
import kotlin.collections.arrayListOf

abstract class EffectiveEntity {
    companion object {
        private val ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "entity")

        val namespacedKeyToEffectiveEntity = hashMapOf<String, EffectiveEntity>()

        fun equalByNamespacedKey(entity1: Entity?, entity2: Entity?): Boolean {
            val value1 = getNamespacedKeyByEntity(entity1) ?: return false
            val value2 = getNamespacedKeyByEntity(entity2) ?: return false

            return value1 == value2
        }

        fun getNamespacedKeyByEntity(entity: Entity?): String? {
            return if (entity != null) {
                EffectiveDataContainerUtils.getContainerValue(entity, ENTITY_KEY, PersistentDataType.STRING)
            } else {
                null
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

        fun getEntitiesByNamespacedKey(namespacedKey: String): ArrayList<Entity> {
            val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: throw IllegalArgumentException(LanguageModule.getMessage("errors.entities.key_not_registered", namespacedKey))
            return effectiveEntity.getEntities()
        }

        fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }
    }

    val cachedEntities = arrayListOf<Entity>()
    fun getEntities(): ArrayList<Entity> {
        return cachedEntities
    }
    fun addEntityToCache(entity: Entity) {
        cachedEntities.add(entity)
    }
    fun removeEntityFromCache(entity: Entity) {
        cachedEntities.removeIf { it.uniqueId == entity.uniqueId }
    }

    init {
        val namespacedName = getNamespacedKey()
        if (namespacedKeyToEffectiveEntity.containsKey(namespacedName)) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.entities.already_registered", namespacedName))
        }
        namespacedKeyToEffectiveEntity[namespacedName] = this
    }

    fun createEntity(location: Location?): Entity {
        val world = location?.world ?: Bukkit.getWorlds()[0]
        val typeClass = getEntityType().entityClass

        val loc = location ?: Location(world, 0.0, 0.0, 0.0)

        val entity = world.createEntity(loc, typeClass as Class<out Entity>)
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
        val world = location.world ?: throw IllegalArgumentException(LanguageModule.getMessage("errors.world.location_null"))
        val entity = createEntity(location)
        addEntityToCache(entity)

        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    fun getEntitiesByNamespacedKey(): List<Entity> {
        return getEntities()
    }

    fun addInteractHandler(
        click: Click,
        callback: InteractCallback,
        cooldownData: EffectiveAbstractInteract.CooldownData<EffectiveEntityInteractable.EventsCallOptions>? = null
    ) {
        EffectiveEntityInteractable.addInteractHandler(
            createEntity(null),
            click,
            callback,
            cooldownData
        )
    }

    fun doEntityNearLookable(
        whoToLook: (Entity) -> Boolean = EffectiveEntityLookable.Look.TO_NEAR_PLAYER,
        lookDistance: Float = 5.0f
    ) {
        EffectiveEntityLookable.doEntityLookable(
            createEntity(null),
            whoToLook,
            lookDistance
        )
    }

    abstract fun editEntity(entity: Entity)
    abstract fun getEntityType(): EntityType
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedKey(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }

    class Events: Listener {
        @EventHandler
        fun onChunkLoad(event: ChunkLoadEvent) {
            event.chunk.entities.forEach {
                val namespacedKey = getNamespacedKeyByEntity(it)
                if (namespacedKey != null) {
                    val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey]!!
                    val list = effectiveEntity.cachedEntities
                    if (list.none { entity -> it.uniqueId == entity.uniqueId }) {
                        list.add(it)
                    }
                }
            }
        }

        //TODO(Срабатывание load и unload при взаимодествии с кастомным мобом, который на самом деле уже не был в загруженном чанке, если юзаеться таймер)

        @EventHandler
        fun onChunkUnload(event: ChunkUnloadEvent) {
            event.chunk.entities.forEach {
                val namespacedKey = getNamespacedKeyByEntity(it)
                if (namespacedKey != null) {
                    val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey]!!
                    effectiveEntity.removeEntityFromCache(it)
                }
            }
        }

        @EventHandler
        fun onEntityDeath(event: EntityRemoveEvent) {
            val namespacedKey = getNamespacedKeyByEntity(event.entity) ?: return

            val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey]!!
            effectiveEntity.removeEntityFromCache(event.entity)
        }
    }
}