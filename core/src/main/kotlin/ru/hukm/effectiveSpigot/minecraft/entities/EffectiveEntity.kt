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

abstract class EffectiveEntity {
    companion object {
        private val ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "entity")

        val namespacedKeyToEntity = hashMapOf<String, EffectiveEntity>()
        val entities = ArrayList<Entity>()

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

        fun getEntitiesByNamespacedKey(namespacedKey: String) {
            entities.filter { getNamespacedKeyByEntity(it) == namespacedKey }
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
        val namespacedName = getNamespacedKey()
        if (namespacedKeyToEntity.containsKey(namespacedName)) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.entities.already_registered", namespacedName))
        }
        namespacedKeyToEntity[namespacedName] = this
    }

    fun createEntity(location: Location?): Entity {
        val world = Bukkit.getWorlds()[0]
        val typeClass = getEntityType().entityClass

        val loc = location ?: Location(Bukkit.getWorlds()[0], 0.0, 0.0, 0.0)

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
        val world = location.world ?: throw IllegalArgumentException("Location world cannot be null")
        val entity = createEntity(location)
        entities.add(entity)

        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    fun getEntitiesByNamespacedKey() {
        return getEntitiesByNamespacedKey(getNamespacedKey())
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
                val key = EffectiveDataContainerUtils.getContainerValue(
                    it,
                    ENTITY_KEY,
                    PersistentDataType.STRING
                )
                if (key != null) {
                    entities.add(it)
                }
            }
        }

        @EventHandler
        fun onChunkUnload(event: ChunkUnloadEvent) {
            event.chunk.entities.forEach { entity ->
                entities.removeIf { it.uniqueId == entity.uniqueId }
            }
        }

        @EventHandler
        fun onEntityDeath(event: EntityRemoveEvent) {
            if (getNamespacedKeyByEntity(event.entity) != null) {
                entities.removeIf { event.entity.uniqueId == it.uniqueId}
            }
        }
    }
}