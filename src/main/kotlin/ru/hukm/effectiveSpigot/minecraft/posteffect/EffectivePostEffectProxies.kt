package ru.hukm.effectiveSpigot.minecraft.posteffect

import ru.hukm.effectiveSpigot.minecraft.nms.NmsProxies
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type

@Proxies(className = "org.bukkit.entity.Player")
internal interface PlayerPostEffectsGetterProxy {
    fun postEffects(instance: Any): Any
}

@Proxies(className = "io.papermc.paper.entity.PlayerPostEffects")
internal interface PlayerPostEffectsProxy {
    fun values(instance: Any): List<*>

    fun add(instance: Any, @Type(className = "net.kyori.adventure.key.Key") key: Any): Boolean

    fun remove(instance: Any, @Type(className = "net.kyori.adventure.key.Key") key: Any): Boolean

    fun clear(instance: Any): Boolean
}

/** Reflection proxies to the Paper post-effect API (`Player#postEffects()`, added in 26.3), which the framework does not compile against. Null on older servers. */
internal object PostEffectProxies {
    private val factory: ReflectionProxyFactory by lazy {
        ReflectionProxyFactory.create(NmsProxies.remapper, javaClass.classLoader)
    }

    val playerPostEffects: PlayerPostEffectsGetterProxy? by lazy {
        runCatching { factory.reflectionProxy(PlayerPostEffectsGetterProxy::class.java) }.getOrNull()
    }

    val postEffects: PlayerPostEffectsProxy? by lazy {
        runCatching { factory.reflectionProxy(PlayerPostEffectsProxy::class.java) }.getOrNull()
    }
}
