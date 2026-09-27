package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
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
import java.util.logging.Level
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
 *
 * From Java call it through the object instance: `EffectiveDataContainerUtils.INSTANCE.getContainerValue(...)`.
 * The functions are deliberately not `@JvmStatic`: on a named `object` that would turn them into static methods
 * and break every plugin already compiled against the instance ones. The Base64 format (Bukkit object stream,
 * URL-safe Base64) is the same as the legacy `org.hukm.api.Api` one, so data written by it stays readable.
 */
object EffectiveDataContainerUtils {
    private val LOC_WORLD_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "world") }
    private val LOC_X_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "x") }
    private val LOC_Y_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "y") }
    private val LOC_Z_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "z") }
    private val LOC_YAW_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "yaw") }
    private val LOC_PITCH_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "pitch") }

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

    /** The [PersistentDataType] for [T]. @throws IllegalArgumentException if [T] has no supported PDC type */
    @PublishedApi
    internal inline fun <reified T : Any> typeFor(): PersistentDataType<Any, T> {
        @Suppress("UNCHECKED_CAST")
        return persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<Any, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
    }

    /** Reads [key] of the given [type] from the item's PDC, or null if absent/no meta. */
    fun <Z : Any, T : Any> getContainerValue(item: ItemStack, key: NamespacedKey, type: PersistentDataType<T, Z>): Z? =
        item.itemMeta?.persistentDataContainer?.get(key, type)

    /** Reads [key] of the given [type] from the holder's PDC (entity, block state, …), or null. */
    fun <Z : Any, T : Any> getContainerValue(holder: PersistentDataHolder, key: NamespacedKey, type: PersistentDataType<T, Z>): Z? =
        holder.persistentDataContainer.get(key, type)

    /** Reads [key] of the given [type] directly from a [PersistentDataContainer], or null. */
    fun <Z : Any, T : Any> getContainerValue(container: PersistentDataContainer, key: NamespacedKey, type: PersistentDataType<T, Z>): Z? =
        container.get(key, type)

    /**
     * Reads [key] from the item's PDC, inferring the [PersistentDataType] from [T].
     * @throws IllegalArgumentException if [T] has no supported PDC type
     */
    inline fun <reified T : Any> getContainerValue(item: ItemStack, key: NamespacedKey): T? =
        getContainerValue(item, key, typeFor<T>())

    /** Reads [key] from the holder's PDC, inferring the type from [T]. */
    inline fun <reified T : Any> getContainerValue(holder: PersistentDataHolder, key: NamespacedKey): T? =
        getContainerValue(holder, key, typeFor<T>())

    /** Reads [key] from a [PersistentDataContainer], inferring the type from [T]. */
    inline fun <reified T : Any> getContainerValue(container: PersistentDataContainer, key: NamespacedKey): T? =
        getContainerValue(container, key, typeFor<T>())

    /**
     * Writes [value] of the given [type] under [key] on the item's PDC; null removes the key.
     * @return the updated stack — use the return value
     */
    fun <Z : Any, T : Any> setContainerValue(item: ItemStack, key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z?): ItemStack =
        editContainer(item) { setContainerValue(it, key, type, value) }

    /** Writes [value] of the given [type] under [key] on the holder's PDC; null removes the key. */
    fun <Z : Any, T : Any> setContainerValue(holder: PersistentDataHolder, key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z?) {
        setContainerValue(holder.persistentDataContainer, key, type, value)
    }

    /** Writes [value] of the given [type] under [key] on a [PersistentDataContainer]; null removes it. */
    fun <Z : Any, T : Any> setContainerValue(container: PersistentDataContainer, key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z?) {
        if (value == null) container.remove(key) else container.set(key, type, value)
    }

    /**
     * Writes [value] under [key] on the item's PDC (type inferred from [T]); null removes the key.
     * @return the updated stack — use the return value, as it re-applies the meta
     */
    inline fun <reified T : Any> setContainerValue(item: ItemStack, key: NamespacedKey, value: T?): ItemStack =
        setContainerValue(item, key, typeFor<T>(), value)

    /** Writes [value] under [key] on the holder's PDC (type inferred from [T]); null removes the key. */
    inline fun <reified T : Any> setContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: T?) {
        setContainerValue(holder, key, typeFor<T>(), value)
    }

    /** Writes [value] under [key] on a [PersistentDataContainer] (type inferred from [T]); null removes it. */
    inline fun <reified T : Any> setContainerValue(container: PersistentDataContainer, key: NamespacedKey, value: T?) {
        setContainerValue(container, key, typeFor<T>(), value)
    }

    /** Whether the item's PDC has [key] of the given [type]. */
    fun <Z : Any, T : Any> hasContainerValue(item: ItemStack, key: NamespacedKey, type: PersistentDataType<T, Z>): Boolean =
        item.itemMeta?.persistentDataContainer?.has(key, type) ?: false

    /** Whether the holder's PDC has [key] of the given [type]. */
    fun <Z : Any, T : Any> hasContainerValue(holder: PersistentDataHolder, key: NamespacedKey, type: PersistentDataType<T, Z>): Boolean =
        holder.persistentDataContainer.has(key, type)

    /** Resolves an entity stored as its UUID string under [key], or null if not stored/offline. */
    fun getEntityByUUIDValue(holder: PersistentDataHolder, key: NamespacedKey): Entity? {
        val uuid = getContainerValue(holder, key, PersistentDataType.STRING) ?: return null
        return Bukkit.getEntity(UUID.fromString(uuid))
    }

    /** Reads a single UUID packed as a 2-element `LONG_ARRAY` under [key], or null. */
    fun getUUIDFromLongArray(holder: PersistentDataHolder, key: NamespacedKey): UUID? {
        val longs = getContainerValue(holder, key, PersistentDataType.LONG_ARRAY) ?: return null
        return UUID(longs[0], longs[1])
    }

    /** Reads a list of UUIDs packed as pairs of longs under [key], or null. */
    fun getUUIDsFromLongArray(holder: PersistentDataHolder, key: NamespacedKey): List<UUID>? {
        val longs = getContainerValue(holder, key, PersistentDataType.LONG_ARRAY) ?: return null
        return longs.toList().chunked(2) { (msb, lsb) -> UUID(msb, lsb) }
    }

    /** Resolves a single entity from a UUID packed under [key], or null if not stored/offline. */
    fun getEntityFromLongArray(holder: PersistentDataHolder, key: NamespacedKey): Entity? =
        getUUIDFromLongArray(holder, key)?.let { Bukkit.getEntity(it) }

    /** Resolves entities from UUIDs packed under [key]; list entries are null for offline entities. */
    fun getEntitiesFromLongArray(holder: PersistentDataHolder, key: NamespacedKey): List<Entity?>? =
        getUUIDsFromLongArray(holder, key)?.map { Bukkit.getEntity(it) }

    /** Stores a single [uuid] as a 2-element `LONG_ARRAY` under [key]. */
    fun setUUIDToLongArray(holder: PersistentDataHolder, key: NamespacedKey, uuid: UUID) {
        setUUIDsToLongArray(holder, key, listOf(uuid))
    }

    /** Stores a list of [uuids] as consecutive long pairs under [key]. */
    fun setUUIDsToLongArray(holder: PersistentDataHolder, key: NamespacedKey, uuids: List<UUID>) {
        val longs = uuids.flatMap { listOf(it.mostSignificantBits, it.leastSignificantBits) }.toLongArray()
        setContainerValue(holder, key, PersistentDataType.LONG_ARRAY, longs)
    }

    /** Reads a Base64-serialized object of type [clazz] from the item's string value at [key], or null. */
    fun <Z> base64GetContainerValue(item: ItemStack, key: NamespacedKey, clazz: Class<Z>): Z? =
        base64Deserialize(getContainerValue(item, key, PersistentDataType.STRING), clazz)

    /** Reads a Base64-serialized object of type [clazz] from the holder's string value at [key], or null. */
    fun <Z> base64GetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, clazz: Class<Z>): Z? =
        base64Deserialize(getContainerValue(holder, key, PersistentDataType.STRING), clazz)

    /** Serializes [value] to Base64 and stores it as the item's string value at [key]. Returns the stack. */
    fun base64SetContainerValue(item: ItemStack, key: NamespacedKey, value: Any?): ItemStack =
        setContainerValue(item, key, PersistentDataType.STRING, base64Serialize(value))

    /** Serializes [value] to Base64 and stores it as the holder's string value at [key]. */
    fun base64SetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: Any?) {
        setContainerValue(holder, key, PersistentDataType.STRING, base64Serialize(value))
    }

    /** Serializes any Bukkit/`Serializable` object to a URL-safe Base64 string, or null on failure (logged). */
    fun base64Serialize(value: Any?): String? = try {
        val bytes = ByteArrayOutputStream()
        BukkitObjectOutputStream(bytes).use { it.writeObject(value) }
        Base64.getUrlEncoder().encodeToString(bytes.toByteArray())
    } catch (e: Exception) {
        EffectiveSpigot.instance.logger.log(Level.WARNING, "Failed to serialize ${value?.javaClass?.name}", e)
        null
    }

    /**
     * Deserializes a Base64 string produced by [base64Serialize] back to [clazz]. Returns null for a null
     * string (absent key) and on failure (logged).
     */
    fun <Z> base64Deserialize(base64: String?, clazz: Class<Z>): Z? {
        if (base64 == null) return null
        return try {
            val bytes = Base64.getUrlDecoder().decode(base64)
            BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { clazz.cast(it.readObject()) }
        } catch (e: Exception) {
            EffectiveSpigot.instance.logger.log(Level.WARNING, "Failed to deserialize ${clazz.name}", e)
            null
        }
    }

    /** Reads the nested container at [key] and maps it via [block], or null if absent. */
    fun <T> getContainer(holder: PersistentDataHolder, key: NamespacedKey, block: (PersistentDataContainer) -> T): T? =
        getContainerValue(holder, key, PersistentDataType.TAG_CONTAINER)?.let(block)

    /** Reads the nested container at [key] on an item and maps it via [block], or null if absent. */
    fun <T> getContainer(item: ItemStack, key: NamespacedKey, block: (PersistentDataContainer) -> T): T? =
        getContainerValue(item, key, PersistentDataType.TAG_CONTAINER)?.let(block)

    /**
     * Opens (or creates) a nested container at [key], lets [block] mutate it, then writes it back.
     * The building block for structured storage like [setLocation].
     */
    fun setContainer(holder: PersistentDataHolder, key: NamespacedKey, block: (PersistentDataContainer) -> Unit) {
        editNested(holder.persistentDataContainer, key, block)
    }

    /** [setContainer] for items: opens/creates a nested container at [key], mutates it, writes back. Returns the stack. */
    fun setContainer(item: ItemStack, key: NamespacedKey, block: (PersistentDataContainer) -> Unit): ItemStack =
        editContainer(item) { editNested(it, key, block) }

    /** Stores a [Location] (world, x/y/z, yaw/pitch) as a nested container under [key]; null removes it. */
    fun setLocation(holder: PersistentDataHolder, key: NamespacedKey, location: Location?) {
        if (location == null) holder.persistentDataContainer.remove(key)
        else setContainer(holder, key) { writeLocation(it, location) }
    }

    /** Stores a [Location] as a nested container under [key] on an item; null removes it. Returns the stack. */
    fun setLocation(item: ItemStack, key: NamespacedKey, location: Location?): ItemStack =
        if (location == null) editContainer(item) { it.remove(key) }
        else setContainer(item, key) { writeLocation(it, location) }

    /** Reads a [Location] stored under [key], or null if absent or its world is unloaded. */
    fun getLocation(holder: PersistentDataHolder, key: NamespacedKey): Location? = getContainer(holder, key, ::readLocation)

    /** Reads a [Location] stored under [key] on an item, or null if absent/world unloaded. */
    fun getLocation(item: ItemStack, key: NamespacedKey): Location? = getContainer(item, key, ::readLocation)

    /** Stores a list of items (Base64) under [key]; null removes the key. */
    fun setItems(holder: PersistentDataHolder, key: NamespacedKey, items: List<ItemStack?>?) {
        if (items == null) holder.persistentDataContainer.remove(key)
        else base64SetContainerValue(holder, key, ArrayList(items))
    }

    /** Stores a list of items (Base64) under [key] on an item; null removes it. Returns the stack. */
    fun setItems(item: ItemStack, key: NamespacedKey, items: List<ItemStack?>?): ItemStack =
        if (items == null) editContainer(item) { it.remove(key) }
        else base64SetContainerValue(item, key, ArrayList(items))

    /** Reads a list of items stored under [key] (entries may be null), or null if absent. */
    fun getItems(holder: PersistentDataHolder, key: NamespacedKey): List<ItemStack?>? =
        base64GetContainerValue(holder, key, List::class.java)?.map { it as ItemStack? }

    /** Reads a list of items stored under [key] on an item (entries may be null), or null if absent. */
    fun getItems(item: ItemStack, key: NamespacedKey): List<ItemStack?>? =
        base64GetContainerValue(item, key, List::class.java)?.map { it as ItemStack? }

    /** Runs [block] on the item's PDC and re-applies the meta; returns [item] unchanged if it has no meta. */
    private inline fun editContainer(item: ItemStack, block: (PersistentDataContainer) -> Unit): ItemStack {
        val meta = item.itemMeta ?: return item
        block(meta.persistentDataContainer)
        item.itemMeta = meta
        return item
    }

    private fun editNested(root: PersistentDataContainer, key: NamespacedKey, block: (PersistentDataContainer) -> Unit) {
        val nested = root.get(key, PersistentDataType.TAG_CONTAINER) ?: root.adapterContext.newPersistentDataContainer()
        block(nested)
        root.set(key, PersistentDataType.TAG_CONTAINER, nested)
    }

    private fun writeLocation(container: PersistentDataContainer, location: Location) {
        val world = location.world ?: return
        container.set(LOC_WORLD_KEY, PersistentDataType.STRING, world.name)
        container.set(LOC_X_KEY, PersistentDataType.DOUBLE, location.x)
        container.set(LOC_Y_KEY, PersistentDataType.DOUBLE, location.y)
        container.set(LOC_Z_KEY, PersistentDataType.DOUBLE, location.z)
        container.set(LOC_YAW_KEY, PersistentDataType.FLOAT, location.yaw)
        container.set(LOC_PITCH_KEY, PersistentDataType.FLOAT, location.pitch)
    }

    private fun readLocation(container: PersistentDataContainer): Location? {
        val world = container.get(LOC_WORLD_KEY, PersistentDataType.STRING)?.let { Bukkit.getWorld(it) } ?: return null
        val x = container.get(LOC_X_KEY, PersistentDataType.DOUBLE) ?: return null
        val y = container.get(LOC_Y_KEY, PersistentDataType.DOUBLE) ?: return null
        val z = container.get(LOC_Z_KEY, PersistentDataType.DOUBLE) ?: return null
        val yaw = container.get(LOC_YAW_KEY, PersistentDataType.FLOAT) ?: 0f
        val pitch = container.get(LOC_PITCH_KEY, PersistentDataType.FLOAT) ?: 0f
        return Location(world, x, y, z, yaw, pitch)
    }
}
