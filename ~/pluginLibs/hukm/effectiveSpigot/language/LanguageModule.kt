package ru.hukm.effectiveSpigot.language

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.interfaces.IModule
import java.io.File

object LanguageModule : IModule {
    private var languageConfig: YamlConfiguration? = null
    private lateinit var langFolder: File

    override fun init() {
        langFolder = File(instance.dataFolder, "languages")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }
        
        saveDefaultLanguages()
        reload()
    }

    fun reload() {
        val langCode = instance.config.getString("language", "en") ?: "en"
        val langFile = File(langFolder, "$langCode.yml")

        languageConfig = if (!langFile.exists()) {
            instance.logger.warning("Language file $langCode.yml not found! Falling back to en.")
            val fallbackFile = File(langFolder, "en.yml")
            if (fallbackFile.exists()) {
                YamlConfiguration.loadConfiguration(fallbackFile)
            } else {
                YamlConfiguration()
            }
        } else {
            YamlConfiguration.loadConfiguration(langFile)
        }
    }

    private fun saveDefaultLanguages() {
        listOf("en.yml", "ru.yml").forEach { fileName ->
            val file = File(langFolder, fileName)
            if (!file.exists()) {
                instance.saveResource("languages/$fileName", false)
            }
        }
    }

    fun getMessage(key: String, vararg args: Any): String {
        val message = languageConfig?.getString(key) ?: key
        val formatted = String.format(message, *args)
        return ChatColor.translateAlternateColorCodes('&', formatted)
    }
}