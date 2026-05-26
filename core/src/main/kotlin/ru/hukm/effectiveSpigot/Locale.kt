package ru.hukm.effectiveSpigot

import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.language.EffectiveLocale

object Locale : EffectiveLocale() {
    override fun getPlugin(): JavaPlugin = instance

    fun init() {}
}
