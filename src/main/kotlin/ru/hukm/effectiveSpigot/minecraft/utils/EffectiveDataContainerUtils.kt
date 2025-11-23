package ru.hukm.effectiveSpigot.minecraft.utils

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object EffectiveDataContainerUtils {
    fun <Z, T> getContainerValue(item: ItemStack, key: NamespacedKey, type: PersistentDataType<T?, Z?>): Z? {
        try {
            return item.itemMeta!!.persistentDataContainer.get<T?, Z?>(key, type)
        } catch (_: NullPointerException) {
        }
        return null
    };

    fun <Z, T> getContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T?, Z?>
    ): Z? {
        try {
            return holder.persistentDataContainer.get<T?, Z?>(key, type)
        } catch (_: NullPointerException) { }
        return null
    };


    fun <Z, T> setContainerValue(
        item: ItemStack,
        key: NamespacedKey,
        type: PersistentDataType<T?, Z?>,
        value: Z?
    ): ItemStack {
        val meta = item.itemMeta
        val container = meta!!.persistentDataContainer

        container.set(key, type, value!!)
        item.itemMeta = meta

        return item
    };

    fun <Z, T> setContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<T?, Z?>,
        value: Z?
    ) {
        val container = holder.persistentDataContainer
        container.set<T?, Z?>(key, type, value!!)
    };


    fun <Z, T> hasContainerValue(item: ItemStack, key: NamespacedKey, type: PersistentDataType<Z?, T?>): Boolean {
        return item.itemMeta!!.persistentDataContainer.has<Z?, T?>(key, type)
    }

    fun <Z, T> hasContainerValue(
        holder: PersistentDataHolder,
        key: NamespacedKey,
        type: PersistentDataType<Z?, T?>
    ): Boolean {
        return holder.persistentDataContainer.has<Z?, T?>(key, type)
    }


    fun <Z> base64GetContainerValue(item: ItemStack, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue<String?, String?>(item, key, PersistentDataType.STRING), clazz)
    }

    fun <Z> base64GetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, clazz: Class<Z?>): Z? {
        return base64Deserialize<Z?>(getContainerValue<String?, String?>(holder, key, PersistentDataType.STRING), clazz)
    }


    fun <Z> base64SetContainerValue(item: ItemStack, key: NamespacedKey, value: Z?): ItemStack {
        return setContainerValue<String?, String?>(item, key, PersistentDataType.STRING, base64Serialize(value))
    }

    fun <Z> base64SetContainerValue(holder: PersistentDataHolder, key: NamespacedKey, value: Z?) {
        setContainerValue<String?, String?>(holder, key, PersistentDataType.STRING, base64Serialize(value))
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
}