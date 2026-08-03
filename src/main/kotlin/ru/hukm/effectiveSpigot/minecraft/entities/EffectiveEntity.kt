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

/**
 * Base class for a custom entity type.
 *
 * A subclass configures spawned entities via [editEntity], picks a base [getEntityType] and declares
 * identity through [getNamespacedData]. Each spawned entity is tagged with the type's namespaced key in
 * persistent data, so plain [Entity] instances can be matched back via [getNamespacedKeyByEntity] /
 * [equalByNamespacedKey]. Like the item/zone bases, the `object` must be instantiated (e.g. `init()`
 * in `onEnable`) to register and start tracking. For example,
 * `EffectiveEntity.equalByNamespacedKey(firstEntity, secondEntity)` tells whether two entities are the
 * same custom type.
 *
 * Live instances are cached automatically as chunks/worlds load and unload; query them with
 * [getEntities]. Behaviours are added via [addInteractHandler] and [doEntityNearLookable].
 *
 * A built-in `/emob <entity>` command spawns any registered custom entity in-game.
 */
abstract class EffectiveEntity {
    companion object {
        internal val ENTITY_KEY = NamespacedKey(EffectiveSpigot.instance, "entity")

        private val _namespacedKeyToEffectiveEntity = hashMapOf<String, EffectiveEntity>()

        /** Read-only registry of all constructed custom entity types, keyed by [getNamespacedKey]. */
        val namespacedKeyToEffectiveEntity: Map<String, EffectiveEntity> get() = _namespacedKeyToEffectiveEntity

        /** Whether both entities carry the same custom-entity key; false if either lacks one. */
        fun equalByNamespacedKey(entity1: Entity?, entity2: Entity?): Boolean {
            val value1 = getNamespacedKeyByEntity(entity1) ?: return false
            val value2 = getNamespacedKeyByEntity(entity2) ?: return false

            return value1 == value2
        }

        /** Reads the custom-entity key stored on [entity], or null for vanilla/absent entities. */
        fun getNamespacedKeyByEntity(entity: Entity?): String? {
            return if (entity != null) {
                EffectiveDataContainerUtils.getContainerValue(entity, ENTITY_KEY, PersistentDataType.STRING)
            } else {
                null
            }
        }

        /** Compares by custom-entity key when both have one, otherwise by [EntityType]. */
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

        /**
         * Cached live entities of the type registered under [namespacedKey].
         * @throws IllegalArgumentException if no type is registered for the key
         */
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

    /** Backing cache of live entities of this type; kept in sync via chunk/world events. */
    val cachedEntities = arrayListOf<Entity>()

    /** Currently tracked live entities of this type. */
    fun getEntities(): ArrayList<Entity> {
        return cachedEntities
    }

    /** Manually adds [entity] to the tracking cache (spawns do this automatically). */
    fun addEntityToCache(entity: Entity) {
        cachedEntities.add(entity)
    }

    /** Removes [entity] from the tracking cache (matched by UUID). */
    fun removeEntityFromCache(entity: Entity) {
        cachedEntities.removeIf { it.uniqueId == entity.uniqueId }
    }

    /** Tracked entities of this type standing on the given [block] position. */
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
        _namespacedKeyToEffectiveEntity[namespacedName] = this

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

    /**
     * Builds (but does not add to the world) an entity of this type at [location] — null falls back to
     * the first world's origin. Use [spawnEntity] to actually place it.
     */
    fun createEntity(location: Location?): Entity {
        val world = location?.world ?: Bukkit.getWorlds()[0]
        val typeClass = getEntityType().entityClass

        val loc = location ?: Location(world, 0.0, 0.0, 0.0)

        val entity = world.createEntity(loc, typeClass as Class<out Entity>)
        return finalizeEntity(entity)
    }

    /** [createEntity] variant that also applies [additionalArgs] to the new entity. */
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

    /** Creates and spawns an entity of this type at [location], adding it to the tracking cache. */
    fun spawnEntity(location: Location): Entity {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entity = createEntity(location)
        addEntityToCache(entity)

        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    /** [spawnEntity] variant that also applies [additionalArgs]. */
    fun spawnEntity(location: Location, additionalArgs: List<String>): Entity {
        val world = location.world ?: throw IllegalArgumentException(Locale.getMessage("errors.world.location_null"))
        val entity = createEntity(location, additionalArgs)
        addEntityToCache(entity)

        world.addEntity(entity)
        entity.teleport(location)

        return entity
    }

    /** Convenience alias of [getEntities] for this type. */
    fun getEntitiesByNamespacedKey(): List<Entity> {
        return getEntities()
    }

    /**
     * Registers an interact handler for entities of *this* custom type (matched by key). To instead
     * handle a plain vanilla [EntityType], register through [EffectiveEntityInteractable.addInteractHandler]
     * with a non-custom entity.
     *
     * @param click left/right interaction that triggers [callback]
     * @param cooldownData optional per-player cooldown
     */
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

    /**
     * Makes entities of this type turn to look at nearby targets.
     *
     * @param whoToLook predicate choosing which entities to look at (default: nearest player)
     * @param lookDistance max distance to look, in blocks
     */
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

    /** Configures a freshly created entity (attributes, name, equipment, …). Called on every create. */
    abstract fun editEntity(entity: Entity)

    /** The base [EntityType] to spawn. */
    abstract fun getEntityType(): EntityType

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedKey]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /**
     * Declares extra, per-instance parameters this entity type accepts — values baked into an
     * individual entity's persistent data at creation, so two entities of the same type can carry
     * different settings.
     *
     * Override to declare the parameters (ordered `name → PDC type` pairs). They are then:
     * - supplied positionally at spawn — `spawnEntity(location, listOf("5"))`, or via the built-in
     *   `/emob <entity> <arg1> <arg2> …` command (parsed in declared order);
     * - read back with [additionalKey] + [EffectiveDataContainerUtils], typically inside [editEntity]
     *   or a behaviour.
     *
     * ```kotlin
     * override fun getAdditionalArgs() = AdditionalArgs(
     *     ExamplePlugin.instance,
     *     listOf("power" to PersistentDataType.INTEGER)
     * )
     * // reading it back:
     * val power = EffectiveDataContainerUtils.getContainerValue(
     *     entity, additionalKey("power"), PersistentDataType.INTEGER
     * )
     * ```
     *
     * Returns null (the default) for entities without extra parameters.
     */
    open fun getAdditionalArgs(): AdditionalArgs? = null

    /** Namespaced keys backing this type's [getAdditionalArgs] (key → PDC type), or null if none. */
    fun getAdditionalArgsNamespacedKeys() = AdditionalArgsSupport.namespacedKeys(getAdditionalArgs())

    /**
     * Resolves the persistent-data key for the declared additional arg [name] — use it to read the
     * per-entity value via [EffectiveDataContainerUtils]. See [getAdditionalArgs] for the full flow.
     * @throws IllegalArgumentException if [name] isn't a declared arg
     */
    fun additionalKey(name: String) = AdditionalArgsSupport.additionalKey(getAdditionalArgs(), name, "entities")

    /** Unique identity as `"<plugin-name>/<id>"`, both lowercased. */
    fun getNamespacedKey(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }
}