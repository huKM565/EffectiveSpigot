package ru.hukm.effectiveSpigot.minecraft.entities

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity.Companion.ENTITY_KEY
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import java.util.UUID

/**
 * A multi-part entity built from two or more [EffectiveEntity] types linked as parent + children.
 *
 * [getEffectiveEntities] returns the parts (index 0 is the parent/root); spawning links them via
 * persistent data so the group can be queried ([getChildren], [getParent], [getCompositesEntities])
 * and is cleaned up together — removing any part removes the whole composite. Requires at least two
 * parts and registers itself on construction.
 *
 * A built-in `/ecomposite <key>` command spawns any registered composite in-game.
 */
abstract class EffectiveCompositeEntity {
    /** Persistent-data key on the parent listing its children's UUIDs. */
    val CHILD_ENTITIES_KEY = NamespacedKey(EffectiveSpigot.instance, "child_entities")

    /** Persistent-data key on a child pointing to its parent's UUID. */
    val PARENT_ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "parent_entity")

    companion object {
        private val _namespacedKeyToEffectiveCompositeEntity = hashMapOf<String, EffectiveCompositeEntity>()

        /** Read-only registry of all constructed composite types, keyed by [getNamespacedKey]. */
        val namespacedKeyToEffectiveCompositeEntity: Map<String, EffectiveCompositeEntity> get() = _namespacedKeyToEffectiveCompositeEntity

        /** Reads the entity key stored on [entity] (shared with [EffectiveEntity]), or null. */
        fun getNamespacedKeyByEntity(entity: Entity?): String? {
            return if (entity != null) {
                EffectiveDataContainerUtils.getContainerValue(entity, ENTITY_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        /** Registered composite type for a namespaced key, or null. */
        fun getEffectiveCompositeEntityByNamespacedKey(namespacedKey: String): EffectiveCompositeEntity? {
            return namespacedKeyToEffectiveCompositeEntity[namespacedKey]
        }

        /** Composite type that [entity] belongs to, or null if it is not part of one. */
        fun getEffectiveCompositeEntityByEntity(entity: Entity?): EffectiveCompositeEntity? {
            val key = getNamespacedKeyByEntity(entity) ?: return null
            return namespacedKeyToEffectiveCompositeEntity[key]
        }

        /** Whether both entities share the same composite/entity key. */
        fun equalByNamespacedKey(entity1: Entity?, entity2: Entity?): Boolean {
            val key1 = getNamespacedKeyByEntity(entity1) ?: return false
            val key2 = getNamespacedKeyByEntity(entity2) ?: return false
            return key1 == key2
        }

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                val removingRoots = hashSetOf<UUID>()

                event<EntityRemoveEvent> {
                    if (it.cause == EntityRemoveEvent.Cause.UNLOAD) return@event

                    val entity = it.entity
                    val key = getNamespacedKeyByEntity(entity) ?: return@event
                    val composite = namespacedKeyToEffectiveCompositeEntity[key] ?: return@event

                    val parentUuid = EffectiveDataContainerUtils.getUUIDFromLongArray(
                        entity, composite.PARENT_ENTITY_KEY
                    )
                    val root = if (parentUuid == null) entity
                        else Bukkit.getEntity(parentUuid) ?: return@event

                    if (!removingRoots.add(root.uniqueId)) return@event
                    try {
                        val childrenUuids = EffectiveDataContainerUtils.getUUIDsFromLongArray(
                            root, composite.CHILD_ENTITIES_KEY
                        ) ?: emptyList()

                        if (root.uniqueId != entity.uniqueId && !root.isDead) root.remove()

                        for (uuid in childrenUuids) {
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
    }

    init {
        val namespacedName = getNamespacedKey()
        if (getEffectiveEntities().size < 2) {
            throw IllegalArgumentException(Locale.getMessage("errors.entities.composite_not_enough_parts", namespacedName))
        }
        if (namespacedKeyToEffectiveCompositeEntity.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.entities.composite_already_registered", namespacedName))
        }
        _namespacedKeyToEffectiveCompositeEntity[namespacedName] = this
    }

    /** All live composites of this type as lists of `[parent, child…]` entities. */
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

    /** Child entities of [parent] (may contain nulls for unloaded entities), or null if none stored. */
    fun getChildren(parent: Entity): List<Entity?>? = EffectiveDataContainerUtils.getEntitiesFromLongArray(
            parent,
            CHILD_ENTITIES_KEY
    )

    /** The child of [parent] whose entity type matches [namespacedKey], or null. */
    fun getChild(parent: Entity, namespacedKey: String): Entity? {
        val children = getChildren(parent) ?: return null

        if (children.isEmpty()) return null

        return children.find { EffectiveEntity.getNamespacedKeyByEntity(it) == namespacedKey }
    }

    /** The parent (root) entity of [possibleChild], or null if it has none. */
    fun getParent(possibleChild: Entity) = EffectiveDataContainerUtils.getEntityFromLongArray(
            possibleChild,
            PARENT_ENTITY_KEY
    )

    /** Whether [possibleParent] is the recorded parent of [possibleChild]. */
    fun isParent(possibleChild: Entity, possibleParent: Entity): Boolean {
        val parentUuid = EffectiveDataContainerUtils.getUUIDFromLongArray(
            possibleChild, PARENT_ENTITY_KEY
        )
        return parentUuid == possibleParent.uniqueId
    }

    /** The parts of the composite; index 0 is the parent/root. Must contain at least two. */
    abstract fun getEffectiveEntities(): List<EffectiveEntity>

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedKey]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Optional per-composite arguments applied to the parent at creation. */
    open fun getAdditionalArgs(): AdditionalArgs? = null

    /** Namespaced keys backing this composite's [getAdditionalArgs]. */
    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    /** Resolves the namespaced key for a single additional arg by [name]. */
    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "entities")

    /** Builds all parts (linked parent↔children) at [location] without adding them to the world. */
    fun createEntities(location: Location?): List<Entity> {
        val entities = arrayListOf<Entity>()
        val world = location?.world ?: Bukkit.getWorlds()[0]

        val loc = location ?: Location(world, 0.0, 0.0, 0.0)

        getEffectiveEntities().forEachIndexed { index, effectiveEntity ->
            val entity = effectiveEntity.createEntity(loc)

            if (index != 0) {
                EffectiveDataContainerUtils.setUUIDToLongArray(
                    entity,
                    PARENT_ENTITY_KEY,
                    entities[0].uniqueId
                )
            }

            entities.add(entity)
        }

        EffectiveDataContainerUtils.setUUIDsToLongArray(
            entities[0],
            CHILD_ENTITIES_KEY,
            entities.drop(1).map { it.uniqueId }
        )

        return entities
    }

    /** [createEntities] variant that also applies [additionalArgs] to the parent. */
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
                EffectiveDataContainerUtils.setUUIDToLongArray(
                    entity, PARENT_ENTITY_KEY, entities[0].uniqueId
                )
            }
            entities.add(entity)
        }


        EffectiveDataContainerUtils.setUUIDsToLongArray(
            entities[0], CHILD_ENTITIES_KEY, entities.drop(1).map { it.uniqueId }
        )

        return entities
    }

    /** Creates and spawns all parts of the composite at [location]. */
    fun spawnEntities(location: Location): List<Entity> {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entities = createEntities(location)

        entities.forEach { entity ->
            world.addEntity(entity)
            entity.teleport(location)
        }

        return entities
    }

    /** [spawnEntities] variant that also applies [additionalArgs]. */
    fun spawnEntities(location: Location, additionalArgs: List<String>): List<Entity> {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entities = createEntities(location, additionalArgs)

        entities.forEach { entity ->
            world.addEntity(entity)
            entity.teleport(location)
        }

        return entities
    }

    /** Unique identity as `"<plugin-name>/<id>"`, both lowercased. */
    fun getNamespacedKey(): String =
        getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
}