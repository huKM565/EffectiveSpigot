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

    fun getEntityByUUIDValue(
        container: PersistentDataHolder,
        key: NamespacedKey,
    ): Entity? {
        val entityUUID = getContainerValue(container, key, PersistentDataType.STRING) ?: return null
        return Bukkit.getEntity(UUID.fromString(entityUUID))
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