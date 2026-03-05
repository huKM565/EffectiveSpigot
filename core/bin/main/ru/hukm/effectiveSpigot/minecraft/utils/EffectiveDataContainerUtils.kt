package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object EffectiveDataContainerUtils {
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