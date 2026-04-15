package ru.hukm.effectiveSpigot.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

abstract class EffectiveConfig {
    private lateinit var file: File
    lateinit var config: FileConfiguration
        private set

    fun init() {
        file = File(getInstance().dataFolder, getFileName())

        if (!file.exists()) {
            file.parentFile.mkdirs()
            getInstance().saveResource(getFileName(), false)
        }

        config = YamlConfiguration.loadConfiguration(file)
    }

    abstract fun getInstance(): JavaPlugin
    abstract fun getFileName(): String

    fun getString(path: String, def: String? = null): String? = config.getString(path, def)
    fun getInt(path: String, def: Int = 0): Int = config.getInt(path, def)
    fun getDouble(path: String, def: Double = 0.0): Double = config.getDouble(path, def)
    fun getBoolean(path: String, def: Boolean = false): Boolean = config.getBoolean(path, def)
    fun getList(path: String): List<*>? = config.getList(path)
    fun getConfigurationSection(path: String) = config.getConfigurationSection(path)
    fun contains(path: String): Boolean = config.contains(path)
    fun set(path: String, value: Any?) {
        config.set(path, value)
        config.save(file)
    }
}