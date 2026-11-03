package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.NamespacedKey
import org.bukkit.WorldCreator

/**
 * Picks the [WorldCreator] for a plugin world so that its folder is unique and survives a server upgrade.
 *
 * Before 26.1 a world is a folder in the server root named after the level name, and `WorldCreator.ofKey(ns:name)`
 * names it after `name` alone, so two plugins' `lobby` would share one folder. So older servers get the level name
 * [legacyLevelName] (`<ns>_<name>`).
 *
 * From 26.1 every world lives inside the main save at `world/dimensions/<ns>/<name>/`, and `WorldCreator.ofKey`
 * looks for a pre-26.1 folder named `<ns>_<name>` (with a `level.dat`) and imports it there, so a world created on
 * 1.21 moves over on upgrade with no work from the plugin. `ofNameAndKey` with a name that doesn't match the key
 * throws on 26.x, hence the two paths.
 */
internal object EffectiveWorldCreator {
    fun legacyLevelName(key: NamespacedKey): String = "${key.namespace}_${key.key}"

    fun creatorFor(key: NamespacedKey, keyedStorage: Boolean): WorldCreator =
        if (keyedStorage) WorldCreator.ofKey(key)
        else WorldCreator.ofNameAndKey(legacyLevelName(key), key)
}
