package ru.hukm.effectiveSpigot.language

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import java.io.File
import java.util.jar.JarFile

abstract class EffectiveLocale {
    abstract fun getPlugin(): JavaPlugin

    private var languageConfig: YamlConfiguration? = null

    init {
        val plugin = getPlugin()
        val langCode = instance.config.getString("language", "en") ?: "en"
        val langFolder = File(plugin.dataFolder, "languages")

        val jarPath = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        JarFile(jarPath).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.startsWith("languages/") && it.name.endsWith(".yml") && !it.isDirectory }
                .forEach { entry ->
                    val file = File(plugin.dataFolder, entry.name)
                    if (!file.exists()) {
                        try {
                            plugin.saveResource(entry.name, false)
                        } catch (_: Exception) { }
                    }
                }
        }

        val langFile = File(langFolder, "$langCode.yml")
        languageConfig = if (langFile.exists()) {
            YamlConfiguration.loadConfiguration(langFile)
        } else {
            plugin.logger.warning("Language file $langCode.yml not found for plugin '${plugin.name}', falling back to en.")
            val fallback = File(langFolder, "en.yml")
            if (fallback.exists()) YamlConfiguration.loadConfiguration(fallback) else YamlConfiguration()
        }
    }

    fun getMessage(key: String, vararg args: Any): String {
        val message = languageConfig?.getString(key) ?: key
        val formatted = String.format(message, *args)
        return ChatColor.translateAlternateColorCodes('&', formatted)
    }

    /**
     * Резолвит ключ в Adventure Component. Строки с legacy-кодами (`&c`, `§a`)
     * парсятся legacy-сериализатором, остальные — MiniMessage (`<red>`, `<gradient:...>`).
     */
    fun getComponent(key: String, vararg args: Any): Component {
        val message = languageConfig?.getString(key) ?: key
        val formatted = String.format(message, *args)

        return if (LEGACY_CODE_REGEX.containsMatchIn(formatted)) {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(formatted.replace('§', '&'))
        } else {
            MiniMessage.miniMessage().deserialize(formatted)
        }
    }

    private companion object {
        val LEGACY_CODE_REGEX = Regex("[&§][0-9a-fk-orxA-FK-ORX]")
    }
}
