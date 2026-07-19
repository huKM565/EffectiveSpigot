package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityLookable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.InteractCallback
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import java.util.*
import kotlin.collections.arrayListOf

abstract class EffectiveEntity {
    companion object {
        internal val ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "entity")

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
            val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: throw IllegalArgumentException(Locale.getMessage("errors.entities.key_not_registered", namespacedKey))
            return effectiveEntity.getEntities()
        }

        fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<ChunkLoadEvent> {
                        it.chunk.entities.forEach { chunkEntity ->
                            val namespacedKey = getNamespacedKeyByEntity(chunkEntity) ?: return@forEach
                            val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: return@forEach
                            val list = effectiveEntity.cachedEntities
                            if (list.none { entity -> chunkEntity.uniqueId == entity.uniqueId }) {
                                list.add(chunkEntity)
                            }
                        }
                    }

                    //TODO(Срабатывание load и unload при взаимодествии с кастомным мобом, который на самом деле уже не был в загруженном чанке, если юзаеться таймер)

                    event<ChunkUnloadEvent> {
                        it.chunk.entities.forEach { chunkEntity ->
                            val namespacedKey = getNamespacedKeyByEntity(chunkEntity) ?: return@forEach
                            val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: return@forEach
                            effectiveEntity.removeEntityFromCache(chunkEntity)
                        }
                    }

                    event<EntityAddToWorldEvent> {
                        val entity = it.entity
                        val namespacedKey = getNamespacedKeyByEntity(entity) ?: return@event
                        val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: return@event
                        val list = effectiveEntity.cachedEntities
                        if (list.none { cached -> cached.uniqueId == entity.uniqueId }) {
                            list.add(entity)
                        }
                    }

                    event<EntityRemoveEvent> {
                        val namespacedKey = getNamespacedKeyByEntity(it.entity) ?: return@event

                        val effectiveEntity = namespacedKeyToEffectiveEntity[namespacedKey] ?: return@event
                        effectiveEntity.removeEntityFromCache(it.entity)
                    }
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

    fun getEntitiesInBlock(block: Block): List<Entity> {
        return getEntities().filter {
            it.location.blockX == block.x
                && it.location.blockY == block.y
                && it.location.blockZ == block.z
                && it.world == block.world
        }
    }

    init {
        val namespacedName = getNamespacedKey()
        if (namespacedKeyToEffectiveEntity.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.entities.already_registered", namespacedName))
        }
        namespacedKeyToEffectiveEntity[namespacedName] = this

        // Подобрать уже загруженные энтити с этим ключом, которые могли быть пропущены
        // в ChunkLoadEvent до регистрации этого EffectiveEntity (например на старте сервера).
        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (getNamespacedKeyByEntity(entity) == namespacedName
                    && cachedEntities.none { it.uniqueId == entity.uniqueId }
                ) {
                    cachedEntities.add(entity)
                }
            }
        }
    }

    fun createEntity(location: Location?): Entity {
        val world = location?.world ?: Bukkit.getWorlds()[0]
        val typeClass = getEntityType().entityClass

        val loc = location ?: Location(world, 0.0, 0.0, 0.0)

        val entity = world.createEntity(loc, typeClass as Class<out Entity>)
        return finalizeEntity(entity)
    }

    fun createEntity(location: Location?, additionalArgs: List<String>): Entity {
        val world = location?.world ?: Bukkit.getWorlds()[0]
        val typeClass = getEntityType().entityClass
        val loc = location ?: Location(world, 0.0, 0.0, 0.0)
        val entity = world.createEntity(loc, typeClass as Class<out Entity>)
        AdditionalArgsSupport.applyToHolder(entity, getAdditionalArgs(), additionalArgs, "entities")
        return finalizeEntity(entity)
    }

    private fun finalizeEntity(entity: Entity): Entity {
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
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entity = createEntity(location)
        addEntityToCache(entity)

        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    fun spawnEntity(location: Location, additionalArgs: List<String>): Entity {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entity = createEntity(location, additionalArgs)
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
    open fun getAdditionalArgs(): AdditionalArgs? = null

    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "entities")

    fun getNamespacedKey(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}