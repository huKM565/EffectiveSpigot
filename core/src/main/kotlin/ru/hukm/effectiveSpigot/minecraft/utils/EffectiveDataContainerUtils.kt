package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Container
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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

object EffectiveDataContainerUtils {
    private val LOC_WORLD_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "world") }
    private val LOC_X_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "x") }
    private val LOC_Y_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "y") }
    private val LOC_Z_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "z") }
    private val LOC_YAW_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "yaw") }
    private val LOC_PITCH_KEY by lazy { NamespacedKey(EffectiveSpigot.instance, "pitch") }

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

    inline fun <reified T : Any> getContainerValue(item: ItemStack, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            item.itemMeta?.persistentDataContainer?.get(key, type)
        } catch (_: NullPointerException) { null }
    }

    inline fun <reified T : Any> getContainerValue(holder: PersistentDataHolder, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            holder.persistentDataContainer.get(key, type)
        } catch (_: NullPointerException) { null }
    }

    inline fun <reified T : Any> getContainerValue(container: PersistentDataContainer, key: NamespacedKey): T? {
        @Suppress("UNCHECKED_CAST")
        val type = persistentDataTypeFor(typeOf<T>()) as? PersistentDataType<*, T>
            ?: throw IllegalArgumentException("Unsupported type: ${T::class}")
        return try {
            container.get(key, type)
        } catch (_: NullPointerException) { null }
    }

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

    fun getEntityByUUIDValue(
        container: PersistentDataHolder,
        key: NamespacedKey,
    ): Entity? {
        val entityUUID = getContainerValue(container, key, PersistentDataType.STRING) ?: return null
        return Bukkit.getEntity(UUID.fromString(entityUUID))
    }

    fun getUUIDFromLongArray(
        container: PersistentDataHolder,
        key: NamespacedKey
    ): UUID? {
        val longArray = getContainerValue(container, key, PersistentDataType.LONG_ARRAY) ?: return null
        return UUID(longArray[0], longArray[1])
    }

    fun getUUIDsFromLongArray(
        container: PersistentDataHolder,
        key: NamespacedKey
    ): List<UUID>? {
        val longArray = getContainerValue(container, key, PersistentDataType.LONG_ARRAY) ?: return null
        return longArray.toList().chunked(2) { (msb, lsb) -> UUID(msb, lsb) }
    }

    fun getEntityFromLongArray(
        container: PersistentDataHolder,
        key: NamespacedKey
    ): Entity? {
        val uuid = getUUIDFromLongArray(container, key) ?: return null
        return Bukkit.getEntity(uuid)
    }

    fun getEntitiesFromLongArray(
        container: PersistentDataHolder,
        key: NamespacedKey
    ): List<Entity?>? {
        val uuids = getUUIDsFromLongArray(container, key) ?: return null
        return uuids.map { Bukkit.getEntity(it) }
    }


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


    fun <Z : Any, T : Any> hasContainerValue(
        item: ItemStack,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Boolean {
        return item.itemMeta?.persistentDataContainer?.has(key, type) ?: false
    }

    fun <Z : Any, T : Any> hasContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T, Z>
    ): Boolean {
        return holder.persistentDataContainer.has(key, type)
    }


    fun <Z> base64GetContainerValue(item: ItemStack, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue(item, key, PersistentDataType.STRING), clazz)
    }

    fun <Z> base64GetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue(holder, key, PersistentDataType.STRING), clazz)
    }


    fun <Z> base64SetContainerValue(item: ItemStack, key: NamespacedKey, value: Z?): ItemStack {
        return setContainerValue(item, key, PersistentDataType.STRING, base64Serialize(value))
    }

    fun <Z> base64SetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: Z?) {
        setContainerValue(holder, key, PersistentDataType.STRING, base64Serialize(value))
    }


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

    fun <T> getContainer(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> T
    ): T? {
        val container = getContainerValue(holder, key, PersistentDataType.TAG_CONTAINER) ?: return null
        return block(container)
    }

    fun <T> getContainer(
        item: ItemStack,
        key: NamespacedKey,
        block: (PersistentDataContainer) -> T
    ): T? {
        val container = getContainerValue(item, key, PersistentDataType.TAG_CONTAINER) ?: return null
        return block(container)
    }


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