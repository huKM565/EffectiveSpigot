package ru.hukm.effectiveSpigot.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Base class for a plugin's YAML config.
 *
 * A subclass names its owning plugin ([getInstance]) and file ([getFileName]). [init] copies the
 * bundled default resource on first run and loads it; call it once before reading any values (from
 * `onLoad` or `onEnable` — both work, since `dataFolder`/`saveResource` are available by then).
 * Typed getters wrap [FileConfiguration], and [set] writes through to disk immediately.
 *
 * ```kotlin
 * object ExampleConfig : EffectiveConfig() {
 *     override fun getInstance() = ExamplePlugin.instance
 *     override fun getFileName() = "config.yml"
 *     fun getSpawnRadius() = getInt("spawn-radius", 10)
 * }
 * ```
 */
abstract class EffectiveConfig {
    private lateinit var file: File

    /** The loaded configuration; valid after [init]. */
    lateinit var config: FileConfiguration
        private set

    /** Copies the bundled default (if missing) and loads the config file. Call once before reading values. */
    fun init() {
        file = File(getInstance().dataFolder, getFileName())

        if (!file.exists()) {
            file.parentFile.mkdirs()
            getInstance().saveResource(getFileName(), false)
        }

        config = YamlConfiguration.loadConfiguration(file)
    }

    /** The plugin that owns this config (used for data folder and bundled resource). */
    abstract fun getInstance(): JavaPlugin

    /** Config file name inside the plugin's data folder, e.g. `"config.yml"`. */
    abstract fun getFileName(): String

    /** Raw value at [path], or null if absent. */
    fun get(path: String): Any? = config.get(path)

    /** String at [path], or [def] if absent. */
    fun getString(path: String, def: String? = null): String? = config.getString(path, def)

    /** Int at [path], or [def] if absent. */
    fun getInt(path: String, def: Int = 0): Int = config.getInt(path, def)

    /** Long at [path], or [def] if absent. */
    fun getLong(path: String, def: Long = 0L): Long = config.getLong(path, def)

    /** Double at [path], or [def] if absent. */
    fun getDouble(path: String, def: Double = 0.0): Double = config.getDouble(path, def)

    /** Boolean at [path], or [def] if absent. */
    fun getBoolean(path: String, def: Boolean = false): Boolean = config.getBoolean(path, def)

    /** Raw list at [path], or null if absent. */
    fun getList(path: String): List<*>? = config.getList(path)

    /** String list at [path] (empty if absent). */
    fun getStringList(path: String): List<String> = config.getStringList(path)

    /** Int list at [path] (empty if absent). */
    fun getIntegerList(path: String): List<Int> = config.getIntegerList(path)

    /** Long list at [path] (empty if absent). */
    fun getLongList(path: String): List<Long> = config.getLongList(path)

    /** Double list at [path] (empty if absent). */
    fun getDoubleList(path: String): List<Double> = config.getDoubleList(path)

    /** Boolean list at [path] (empty if absent). */
    fun getBooleanList(path: String): List<Boolean> = config.getBooleanList(path)

    /** Configuration section at [path], or null if absent. */
    fun getConfigurationSection(path: String): ConfigurationSection? = config.getConfigurationSection(path)

    /** Whether [path] exists in the config. */
    fun contains(path: String): Boolean = config.contains(path)

    /** Sets [path] to [value] and saves the file immediately. */
    fun set(path: String, value: Any?) {
        config.set(path, value)
        config.save(file)
    }
}
