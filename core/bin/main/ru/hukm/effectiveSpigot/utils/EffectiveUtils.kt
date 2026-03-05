package ru.hukm.effectiveSpigot.utils

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object EffectiveUtils {
    fun <T> loadMcvModule(className: String, plugin: JavaPlugin): T? {
        return try {
            val clazz = Class.forName(className)
            clazz.getField("INSTANCE").get(null) as T
        } catch (e: Exception) {
            e.printStackTrace()
            Bukkit.getPluginManager().disablePlugin(plugin)
            null
        }
    }

    fun twoIntToLong(first: Int, second: Int): Long {
        return (first.toLong() shl 32) or (second.toLong() and 0xFFFFFFFFL)
    }
}