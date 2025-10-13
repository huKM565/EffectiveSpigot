package ru.hukm.effectiveSpigot.config

import org.bukkit.configuration.file.FileConfiguration
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.interfaces.IModule

object ConfigModule: IModule {
    private lateinit var config: FileConfiguration

    override fun init() {
        instance.saveDefaultConfig()
        config = instance.config
    }

    fun isDebug() = config.getBoolean("debug", true)
}