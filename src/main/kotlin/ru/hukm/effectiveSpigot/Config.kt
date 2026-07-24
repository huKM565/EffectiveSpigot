package ru.hukm.effectiveSpigot

import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.config.EffectiveConfig

object Config : EffectiveConfig() {
    override fun getInstance(): JavaPlugin = EffectiveSpigot.instance
    override fun getFileName(): String = "config.yml"

    fun getLanguage(): String = getString("language", "en") ?: "en"

    fun isResourcepackHttpServerEnabled(): Boolean =
        getBoolean("resourcepack.http-server.enable", false)

    fun getResourcepackHttpServerIp(): String =
        getString("resourcepack.http-server.ip", "") ?: ""

    fun getResourcepackHttpServerPort(): Int =
        getInt("resourcepack.http-server.port", 25566)
}