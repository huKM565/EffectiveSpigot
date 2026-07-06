package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity.Companion.ENTITY_KEY
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import java.util.UUID

abstract class EffectiveCompositeEntity {
    val CHILD_ENTITIES_KEY = NamespacedKey(EffectiveSpigot.instance, "child_entities")
    val PARENT_ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "parent_entity")

    companion object {
        val namespacedKeyToEffectiveCompositeEntity = hashMapOf<String, EffectiveCompositeEntity>()

        fun getNamespacedKeyByEntity(entity: Entity?): String? {
            return if (entity != null) {
                EffectiveDataContainerUtils.getContainerValue(entity, ENTITY_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        fun getEffectiveCompositeEntityByNamespacedKey(namespacedKey: String): EffectiveCompositeEntity? {
            return namespacedKeyToEffectiveCompositeEntity[namespacedKey]
        }

        fun getEffectiveCompositeEntityByEntity(entity: Entity?): EffectiveCompositeEntity? {
            val key = getNamespacedKeyByEntity(entity) ?: return null
            return namespacedKeyToEffectiveCompositeEntity[key]
        }

        fun equalByNamespacedKey(entity1: Entity?, entity2: Entity?): Boolean {
            val key1 = getNamespacedKeyByEntity(entity1) ?: return false
            val key2 = getNamespacedKeyByEntity(entity2) ?: return false
            return key1 == key2
        }

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
            }
        }
    }

    init {
        val namespacedName = getNamespacedKey()
        if (getEffectiveEntities().size < 2) {
            throw IllegalArgumentException(Locale.getMessage("errors.entities.composite_not_enough_parts", namespacedName))
        }
        if (namespacedKeyToEffectiveCompositeEntity.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.entities.composite_already_registered", namespacedName))
        }
        namespacedKeyToEffectiveCompositeEntity[namespacedName] = this
    }

    fun getCompositesEntities(): List<List<Entity>> {
        val allCompositesEntities = arrayListOf<ArrayList<Entity>>()

        val parentEffectiveEntity = getEffectiveEntities()[0]
        for (parentEntity in parentEffectiveEntity.getEntities()) {
            allCompositesEntities.add(arrayListOf(parentEntity))
        }

        for (childEffectiveEntity in getEffectiveEntities().drop(1)) {
            for (compositeEntity in allCompositesEntities) {
                for (childEntity in childEffectiveEntity.getEntities()) {
                    if (isParent(childEntity, compositeEntity[0])) {
                        compositeEntity.add(childEntity)
                    }
                }
            }
        }

        return allCompositesEntities
    }

    fun getParent(possibleChild: Entity) = EffectiveDataContainerUtils.getEntityByUUIDValue(
            possibleChild,
            PARENT_ENTITY_KEY
    )

    fun isParent(possibleChild: Entity, possibleParent: Entity): Boolean {
        val parentUuid = EffectiveDataContainerUtils.getContainerValue(
            possibleChild, PARENT_ENTITY_KEY, PersistentDataType.STRING
        )
        return parentUuid == possibleParent.uniqueId.toString()
    }

    abstract fun getEffectiveEntities(): List<EffectiveEntity>
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    open fun getAdditionalArgs(): AdditionalArgs? = null

    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "entities")

    fun createEntities(location: Location?): List<Entity> {
        val entities = arrayListOf<Entity>()
        val world = location?.world ?: Bukkit.getWorlds()[0]

        val loc = location ?: Location(world, 0.0, 0.0, 0.0)

        getEffectiveEntities().forEachIndexed { index, effectiveEntity ->
            val entity = effectiveEntity.createEntity(loc)

            if (index != 0) {
                EffectiveDataContainerUtils.setContainerValue(entity, PARENT_ENTITY_KEY, PersistentDataType.STRING, entities[0].uniqueId.toString())
            }

            entities.add(entity)
        }

        EffectiveDataContainerUtils.setContainerValue(
            entities[0],
            CHILD_ENTITIES_KEY,
            entities.drop(1).map { it.uniqueId.toString() }
        )

        return entities
    }

    fun createEntities(location: Location?, additionalArgs: List<String>): List<Entity> {
        val effectiveEntities = getEffectiveEntities()
        val world = location?.world ?: Bukkit.getWorlds()[0]
        val loc = location ?: Location(world, 0.0, 0.0, 0.0)
        val entities = arrayListOf<Entity>()

        effectiveEntities.forEachIndexed { index, effectiveEntity ->
            val entity = if (index == 0) {
                @Suppress("UNCHECKED_CAST")
                val e = world.createEntity(loc, effectiveEntity.getEntityType().entityClass as Class<out Entity>)
                AdditionalArgsSupport.applyToHolder(e, getAdditionalArgs(), additionalArgs, "entities")
                effectiveEntity.editEntity(e)
                EffectiveDataContainerUtils.setContainerValue(
                    e, ENTITY_KEY, PersistentDataType.STRING, effectiveEntity.getNamespacedKey()
                )
                e
            } else {
                effectiveEntity.createEntity(loc)
            }

            if (index != 0) {
                EffectiveDataContainerUtils.setContainerValue(
                    entity, PARENT_ENTITY_KEY, PersistentDataType.STRING, entities[0].uniqueId.toString()
                )
            }
            entities.add(entity)
        }

        val longlyUuids = entities.drop(1).map {
            val uuid = it.uniqueId
            listOf(
                uuid.mostSignificantBits,
                uuid.leastSignificantBits
            )
        }.flatten().toLongArray()

        EffectiveDataContainerUtils.setContainerValue(
            entities[0], CHILD_ENTITIES_KEY, PersistentDataType.LONG_ARRAY, longlyUuids
        )

        return entities
    }

    fun spawnEntities(location: Location): List<Entity> {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entities = createEntities(location)

        entities.forEach { entity ->
            world.addEntity(entity)
            entity.teleport(location)
        }

        return entities
    }

    fun spawnEntities(location: Location, additionalArgs: List<String>): List<Entity> {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entities = createEntities(location, additionalArgs)

        entities.forEach { entity ->
            world.addEntity(entity)
            entity.teleport(location)
        }

        return entities
    }

    fun getNamespacedKey(): String =
        getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()

    class Events : Listener {
        companion object {
            private val removingRoots = hashSetOf<UUID>()
        }

        @EventHandler
        fun onEntityRemove(event: EntityRemoveEvent) {
            if (event.cause == EntityRemoveEvent.Cause.UNLOAD) return

            val entity = event.entity
            val key = getNamespacedKeyByEntity(entity) ?: return
            val composite = namespacedKeyToEffectiveCompositeEntity[key] ?: return

            val parentUuidStr = EffectiveDataContainerUtils.getContainerValue(
                entity, composite.PARENT_ENTITY_KEY, PersistentDataType.STRING
            )
            val root = if (parentUuidStr == null) entity
                else Bukkit.getEntity(UUID.fromString(parentUuidStr)) ?: return

            if (!removingRoots.add(root.uniqueId)) return
            try {
                val childrenUuids = EffectiveDataContainerUtils.getContainerValue<List<String>>(
                    root, composite.CHILD_ENTITIES_KEY
                ) ?: emptyList()

                if (root.uniqueId != entity.uniqueId && !root.isDead) root.remove()

                for (uuidStr in childrenUuids) {
                    val uuid = UUID.fromString(uuidStr)
                    if (uuid == entity.uniqueId) continue
                    val child = Bukkit.getEntity(uuid) ?: continue
                    if (!child.isDead) child.remove()
                }

            } finally {
                removingRoots.remove(root.uniqueId)
            }
        }
    }
}
