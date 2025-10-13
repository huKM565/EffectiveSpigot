package ru.hukm.effectiveSpigot.minecraft.world

import ru.hukm.effectiveSpigot.EffectiveSpigot.Companion.instance
import ru.hukm.effectiveSpigot.interfaces.IModule

object EffectiveWorldModule: IModule {
    override fun init() {
        instance.server.pluginManager.registerEvents(EffectiveWorldEvents(), instance)
    }
}