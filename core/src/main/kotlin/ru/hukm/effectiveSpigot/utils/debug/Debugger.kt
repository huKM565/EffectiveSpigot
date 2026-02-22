package ru.hukm.effectiveSpigot.utils.debug

import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.config.ConfigModule

object Debugger {
    private val logger = EffectiveSpigot.Companion.instance.logger

    fun info(message: String) {
        if(ConfigModule.isDebug()) logger.info(message)
    }
}