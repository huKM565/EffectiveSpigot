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
}