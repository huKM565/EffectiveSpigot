package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import ru.hukm.effectiveSpigot.EffectiveSpigot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import javax.naming.Name
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Helpers for reading and writing Bukkit [PersistentDataContainer] (PDC) data on items, entities and
 * other [PersistentDataHolder]s.
 *
 * Beyond thin typed get/set wrappers, it adds conveniences the vanilla API lacks:
 * - **reified** `get/setContainerValue<T>` that infer the [PersistentDataType] from `T`;
 * - **UUID / entity** storage packed into `LONG_ARRAY` keys (single and lists);
 * - **Base64** serialization of arbitrary `Serializable`/Bukkit objects (and item lists);
 * - **nested containers**, and structured **[Location]** storage.
 *
 * Every getter returns null (rather than throwing) when the key is absent or the holder has no meta.
 * Note: mutating an [ItemStack]'s PDC goes through its `itemMeta`, so the `set*(ItemStack, …)`
 * overloads return the updated stack — use the returned value.
 */
object EffectiveDataContainerUtils {
    private val LOC_WORLD_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "world") }
    private val LOC_X_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "x") }
    private val LOC_Y_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "y") }
    private val LOC_Z_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "z") }
    private val LOC_YAW_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "yaw") }
    private val LOC_PITCH_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "pitch") }

    /** Reads [key] of the given [type] from the item's PDC, or null if absent/no meta. */
    fun <Z : Any, T : Any> getContainerValue(
        item: ItemStack,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Z? {
        try {
            return item.itemMeta?.persistentDataContainer?.get(key, type)
        } catch (_: NullPointerException) {
        }
        return null
    };

    /** Reads [key] of the given [type] from the holder's PDC (entity, block state, …), or null. */
    fun <Z : Any, T : Any> getContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Z? {
        try {
            return holder.persistentDataContainer.get(key, type)
        } catch (_: NullPointerException) {
        }
        return null
    };

    /** Reads [key] of the given [type] directly from a [PersistentDataContainer], or null. */
    fun <Z : Any, T : Any> getContainerValue(
        container: PersistentDataContainer,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Z? {
        try {
            return container.get(key, type)
        } catch (_: NullPointerException) {
        }
        return null
    };

    @PublishedApi
    internal fun persistentDataTypeFor(type: KType): PersistentDataType<*, *>? = when (type) {
        typeOf<String>(), typeOf<String?>() -> PersistentDataType.STRING
        typeOf<Int>(), typeOf<Int?>() -> PersistentDataType.INTEGER
        typeOf<Long>(), typeOf<Long?>() -> PersistentDataType.LONG
        typeOf<Double>(), typeOf<Double?>() -> PersistentDataType.DOUBLE
        typeOf<Float>(), typeOf<Float?>() -> PersistentDataType.FLOAT
        typeOf<Byte>(), typeOf<Byte?>() -> PersistentDataType.BYTE
        typeOf<Short>(), typeOf<Short?>() -> PersistentDataType.SHORT
        typeOf<Boolean>(), typeOf<Boolean?>() -> PersistentDataType.BOOLEAN
        typeOf<ByteArray>(), typeOf<ByteArray?>() -> PersistentDataType.BYTE_ARRAY
        typeOf<IntArray>(), typeOf<IntArray?>() -> PersistentDataType.INTEGER_ARRAY
        typeOf<LongArray>(), typeOf<LongArray?>() -> PersistentDataType.LONG_ARRAY
        typeOf<List<String>>(), typeOf<List<String>?>() -> PersistentDataType.LIST.strings()
        typeOf<List<Int>>(), typeOf<List<Int>?>() -> PersistentDataType.LIST.integers()
        typeOf<List<Long>>(), typeOf<List<Long>?>() -> PersistentDataType.LIST.longs()
        typeOf<List<Double>>(), typeOf<List<Double>?>() -> PersistentDataType.LIST.doubles()
        typeOf<List<Float>>(), typeOf<List<Float>?>() -> PersistentDataType.LIST.floats()
        typeOf<List<Byte>>(), typeOf<List<Byte>?>() -> PersistentDataType.LIST.bytes()
        typeOf<List<Short>>(), typeOf<List<Short>?>() -> PersistentDataType.LIST.shorts()
        typeOf<List<Boolean>>(), typeOf<List<Boolean>?>() -> PersistentDataType.LIST.booleans()
        typeOf<List<ByteArray>>(), typeOf<List<ByteArray>?>() -> PersistentDataType.LIST.byteArrays()
        typeOf<List<IntArray>>(), typeOf<List<IntArray>?>() -> PersistentDataType.LIST.integerArrays()
        typeOf<List<LongArray>>(), typeOf<List<LongArray>?>() -> PersistentDataType.LIST.longArrays()
        else -> null
    }

    /**
     * Reads [key] from the item's PDC, inferring the [PersistentDataType] from [T].
     * @throws IllegalArgumentException if [T] has no supported PDC type
     */
    inline fun <reified T : Any> getContainerValue(item: ItemStack, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            item.itemMeta?.persistentDataContainer?.get(key, type)
        } catch (_: NullPointerException) { null }
    }

    /** Reads [key] from the holder's PDC, inferring the type from [T]. */
    inline fun <reified T : Any> getContainerValue(holder: PersistentDataHolder, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            holder.persistentDataContainer.get(key, type)
        } catch (_: NullPointerException) { null }
    }

    /** Reads [key] from a [PersistentDataContainer], inferring the type from [T]. */
    inline fun <reified T : Any> getContainerValue(container: PersistentDataContainer, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            container.get(key, type)
        } catch (_: NullPointerException) { null }
    }

    /**
     * Writes [value] under [key] on the item's PDC (type inferred from [T]); null removes the key.
     * @return the updated stack — use the return value, as it re-applies the meta
     */
    inline fun <reified T : Any> setContainerValue(item: ItemStack, key: NamespacedKey, value: T?): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        if (value == null) {
            container.remove(key)
        } else {
            @Suppress("UNCHECKED_CAST")
            val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
                ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
            container.set(key, type, value)
        }
        item.itemMeta = meta
        return item
    }

    /** Writes [value] under [key] on the holder's PDC (type inferred from [T]); null removes the key. */
    inline fun <reified T : Any> setContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: T?) {
        val container = holder.persistentDataContainer
        if (value == null) {
            container.remove(key)
        } else {
            @Suppress("UNCHECKED_CAST")
            val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
                ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
            container.set(key, type, value)
        }
    }

    /** Writes [value] under [key] on a [PersistentDataContainer] (type inferred from [T]); null removes it. */
    inline fun <reified T : Any> setContainerValue(container: PersistentDataContainer, key: NamespacedKey, value: T?) {
        if (value == null) {
            container.remove(key)
        } else {
            @Suppress("UNCHECKED_CAST")
            val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
                ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
            container.set(key, type, value)
        }
    }

    /** Resolves an entity stored as its UUID string under [key], or null if not stored/offline. */
    fun getEntityByUUIDValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
    ): Entity? {
        val entityUUID = getContainerValue(holder, key, PersistentDataType.STRING) ?: return null
        return Bukkit.getEntity(UUID.fromString(entityUUID))
    }

    /** Reads a single UUID packed as a 2-element `LONG_ARRAY` under [key], or null. */
    fun getUUIDFromLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey
    ): UUID? {
        val longArray = getContainerValue(holder, key, PersistentDataType.LONG_ARRAY) ?: return null
        return UUID(longArray[0], longArray[1])
    }

    /** Reads a list of UUIDs packed as pairs of longs under [key], or null. */
    fun getUUIDsFromLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey
    ): List<UUID>? {
        val longArray = getContainerValue(holder, key, PersistentDataType.LONG_ARRAY) ?: return null
        return longArray.toList().chunked(2) { (msb, lsb) -> UUID(msb, lsb) }
    }

    /** Resolves a single entity from a UUID packed under [key], or null if not stored/offline. */
    fun getEntityFromLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey
    ): Entity? {
        val uuid = getUUIDFromLongArray(holder, key) ?: return null
        return Bukkit.getEntity(uuid)
    }

    /** Resolves entities from UUIDs packed under [key]; list entries are null for offline entities. */
    fun getEntitiesFromLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey
    ): List<Entity?>? {
        val uuids = getUUIDsFromLongArray(holder, key) ?: return null
        return uuids.map { Bukkit.getEntity(it) }
    }

    /** Stores a single [uuid] as a 2-element `LONG_ARRAY` under [key]. */
    fun setUUIDToLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        uuid: UUID
    ) {
        setContainerValue(
            holder,
            key,
            PersistentDataType.LONG_ARRAY,
            longArrayOf(uuid.mostSignificantBits, uuid.leastSignificantBits)
        )
    }

    /** Stores a list of [uuids] as consecutive long pairs under [key]. */
    fun setUUIDsToLongArray(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        uuids: List<UUID>
    ) {
        setContainerValue(
            holder,
            key,
            PersistentDataType.LONG_ARRAY,
            uuids.flatMap { listOf(it.mostSignificantBits, it.leastSignificantBits) }.toLongArray()
        )
    }

    /**
     * Writes [value] of the given [type] under [key] on the item's PDC; null removes the key.
     * @return the updated stack — use the return value
     */
    fun <Z : Any, T : Any> setContainerValue(
        item: ItemStack,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>,
        value: Z?
    ): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer

        if (value == null) {
            container.remove(key)
        } else {
            container.set(key, type, value)
        }
        item.itemMeta = meta

        return item
    };

    /** Writes [value] of the given [type] under [key] on a [PersistentDataContainer]; null removes it. */
    fun <Z : Any, T : Any> setContainerValue(
        container: PersistentDataContainer,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>,
        value: Z?
    ) {
        if (value == null) {
            container.remove(key)
        } else {
            container.set(key, type, value)
        }
    };

    /** Writes [value] of the given [type] under [key] on the holder's PDC; null removes the key. */
    fun <Z : Any, T : Any> setContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>,
        value: Z?
    ) {
        val container = holder.persistentDataContainer
        if (value == null) {
            container.remove(key)
        } else {
            container.set(key, type, value)
        }
    };


    /** Whether the item's PDC has [key] of the given [type]. */
    fun <Z : Any, T : Any> hasContainerValue(
        item: ItemStack,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Boolean {
        return item.itemMeta?.persistentDataContainer?.has(key, type) ?: false
    }

    /** Whether the holder's PDC has [key] of the given [type]. */
    fun <Z : Any, T : Any> hasContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Boolean {
        return holder.persistentDataContainer.has(key, type)
    }


    /** Reads a Base64-serialized object of type [clazz] from the item's string value at [key], or null. */
    fun <Z> base64GetContainerValue(item: ItemStack, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue(item, key, PersistentDataType.STRING), clazz)
    }

    /** Reads a Base64-serialized object of type [clazz] from the holder's string value at [key], or null. */
    fun <Z> base64GetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue(holder, key, PersistentDataType.STRING), clazz)
    }


    /** Serializes [value] to Base64 and stores it as the item's string value at [key]. Returns the stack. */
    fun <Z> base64SetContainerValue(item: ItemStack, key: NamespacedKey, value: Z?): ItemStack {
        return setContainerValue(item, key, PersistentDataType.STRING, base64Serialize(value))
    }

    /** Serializes [value] to Base64 and stores it as the holder's string value at [key]. */
    fun <Z> base64SetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: Z?) {
        setContainerValue(holder, key, PersistentDataType.STRING, base64Serialize(value))
    }


    /** Serializes any Bukkit/`Serializable` object to a URL-safe Base64 string, or null on failure. */
    fun base64Serialize(`object`: Any?): String? {
        try {
            val bytesOut = ByteArrayOutputStream()
            val out = BukkitObjectOutputStream(bytesOut)

            out.writeObject(`object`)
            out.flush()
            out.close()

            return Base64.getUrlEncoder().encodeToString(bytesOut.toByteArray())
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }

    /** Deserializes a Base64 string produced by [base64Serialize] back to [clazz], or null on failure. */
    fun <Z> base64Deserialize(base64: String?, clazz: Class<Z?>): Z? {
        try {
            val data = Base64.getUrlDecoder().decode(base64)

            val bytesIn = ByteArrayInputStream(data)
            val `in` = BukkitObjectInputStream(bytesIn)
            `in`.close()

            return clazz.cast(`in`.readObject())
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }

    /** Reads the nested container at [key] and maps it via [block], or null if absent. */
    fun <T> getContainer(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> T
    ): T? {
        val container = getContainerValue(holder, key, PersistentDataType.TAG_CONTAINER) ?: return null
        return block(container)
    }

    /** Reads the nested container at [key] on an item and maps it via [block], or null if absent. */
    fun <T> getContainer(
        item: ItemStack,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> T
    ): T? {
        val container = getContainerValue(item, key, PersistentDataType.TAG_CONTAINER) ?: return null
        return block(container)
    }


    /** Stores a [Location] (world, x/y/z, yaw/pitch) as a nested container under [key]; null removes it. */
    fun setLocation(holder: PersistentDataHolder, key: NamespacedKey, location: Location?) {
        if (location == null) {
            holder.persistentDataContainer.remove(key)
            return
        }
        setContainer(holder, key) { container ->
            container.set(LOC_WORLD_KEY, PersistentDataType.STRING, location.world?.name ?: return@setContainer)
            container.set(LOC_X_KEY, PersistentDataType.DOUBLE, location.x)
            container.set(LOC_Y_KEY, PersistentDataType.DOUBLE, location.y)
            container.set(LOC_Z_KEY, PersistentDataType.DOUBLE, location.z)
            container.set(LOC_YAW_KEY, PersistentDataType.FLOAT, location.yaw)
            container.set(LOC_PITCH_KEY, PersistentDataType.FLOAT, location.pitch)
        }
    }

    /** Stores a [Location] as a nested container under [key] on an item; null removes it. Returns the stack. */
    fun setLocation(item: ItemStack, key: NamespacedKey, location: Location?): ItemStack {
        if (location == null) {
            val meta = item.itemMeta ?: return item
            meta.persistentDataContainer.remove(key)
            item.itemMeta = meta
            return item
        }
        return setContainer(item, key) { container ->
            container.set(LOC_WORLD_KEY, PersistentDataType.STRING, location.world?.name ?: return@setContainer)
            container.set(LOC_X_KEY, PersistentDataType.DOUBLE, location.x)
            container.set(LOC_Y_KEY, PersistentDataType.DOUBLE, location.y)
            container.set(LOC_Z_KEY, PersistentDataType.DOUBLE, location.z)
            container.set(LOC_YAW_KEY, PersistentDataType.FLOAT, location.yaw)
            container.set(LOC_PITCH_KEY, PersistentDataType.FLOAT, location.pitch)
        }
    }


    /** Reads a [Location] stored under [key], or null if absent or its world is unloaded. */
    fun getLocation(holder: PersistentDataHolder, key: NamespacedKey): Location? {
        return getContainer(holder, key) { container ->
            val worldName = container.get(LOC_WORLD_KEY, PersistentDataType.STRING) ?: return@getContainer null
            val x = container.get(LOC_X_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val y = container.get(LOC_Y_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val z = container.get(LOC_Z_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val yaw = container.get(LOC_YAW_KEY, PersistentDataType.FLOAT) ?: 0f
            val pitch = container.get(LOC_PITCH_KEY, PersistentDataType.FLOAT) ?: 0f
            val world = Bukkit.getWorld(worldName) ?: return@getContainer null
            Location(world, x, y, z, yaw, pitch)
        }
    }

    /** Reads a [Location] stored under [key] on an item, or null if absent/world unloaded. */
    fun getLocation(item: ItemStack, key: NamespacedKey): Location? {
        return getContainer(item, key) { container ->
            val worldName = container.get(LOC_WORLD_KEY, PersistentDataType.STRING) ?: return@getContainer null
            val x = container.get(LOC_X_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val y = container.get(LOC_Y_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val z = container.get(LOC_Z_KEY, PersistentDataType.DOUBLE) ?: return@getContainer null
            val yaw = container.get(LOC_YAW_KEY, PersistentDataType.FLOAT) ?: 0f
            val pitch = container.get(LOC_PITCH_KEY, PersistentDataType.FLOAT) ?: 0f
            val world = Bukkit.getWorld(worldName) ?: return@getContainer null
            Location(world, x, y, z, yaw, pitch)
        }
    }

    /** Stores a list of items (Base64) under [key]; null removes the key. */
    fun setItems(holder: PersistentDataHolder, key: NamespacedKey, items: List<ItemStack?>?) {
        if (items == null) {
            holder.persistentDataContainer.remove(key)
            return
        }

        base64SetContainerValue(holder, key, items)
    }

    /** Reads a list of items stored under [key] (entries may be null), or null if absent. */
    fun getItems(holder: PersistentDataHolder, key: NamespacedKey): List<ItemStack?>? {
        val list = base64GetContainerValue(
            holder, key, ArrayList::class.java as Class<ArrayList<*>?>
        ) ?: return null
        return list.map { it as ItemStack? }
    }

    /** Stores a list of items (Base64) under [key] on an item; null removes it. Returns the stack. */
    fun setItems(item: ItemStack, key: NamespacedKey, items: List<ItemStack?>?): ItemStack {
        if (items == null) {
            val meta = item.itemMeta ?: return item
            meta.persistentDataContainer.remove(key)
            item.itemMeta = meta
            return item
        }

        return base64SetContainerValue(item, key, ArrayList(items))
    }

    /** Reads a list of items stored under [key] on an item (entries may be null), or null if absent. */
    fun getItems(item: ItemStack, key: NamespacedKey): List<ItemStack?>? {
        val list = base64GetContainerValue(
            item, key, ArrayList::class.java as Class<ArrayList<*>?>
        ) ?: return null
        return list.map { it as ItemStack? }
    }


    /**
     * Opens (or creates) a nested container at [key], lets [block] mutate it, then writes it back.
     * The building block for structured storage like [setLocation].
     */
    fun setContainer(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> Unit
    ) {
        val root = holder.persistentDataContainer
        val folder = getContainerValue(holder, key, PersistentDataType.TAG_CONTAINER) ?: root.adapterContext.newPersistentDataContainer()
        block(folder)
        root.set(key, PersistentDataType.TAG_CONTAINER, folder)
    }

    /** [setContainer] for items: opens/creates a nested container at [key], mutates it, writes back. Returns the stack. */
    fun setContainer(
        item: ItemStack,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> Unit
    ): ItemStack {
        val meta = item.itemMeta ?: return item
        val root = meta.persistentDataContainer
        val folder = getContainerValue(item, key, PersistentDataType.TAG_CONTAINER) ?: root.adapterContext.newPersistentDataContainer()
        block(folder)
        root.set(key, PersistentDataType.TAG_CONTAINER, folder)
        item.itemMeta = meta
        return item
    }
}