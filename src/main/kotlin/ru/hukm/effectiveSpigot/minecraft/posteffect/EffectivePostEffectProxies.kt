package ru.hukm.effectiveSpigot.minecraft.posteffect

import ru.hukm.effectiveSpigot.minecraft.nms.NmsProxies
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static
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

@Proxies(className = "net.minecraft.resources.Identifier")
internal interface PostEffectIdentifierProxy {
    @Static
    fun fromNamespaceAndPath(namespace: String, path: String): Any
}

@Proxies(className = "net.minecraft.server.level.ServerPlayer")
internal interface ServerPlayerPostEffectsProxy {
    fun addPostEffect(instance: Any, @Type(className = "net.minecraft.resources.Identifier") id: Any): Boolean

    fun removePostEffect(instance: Any, @Type(className = "net.minecraft.resources.Identifier") id: Any): Boolean

    fun getPostEffects(instance: Any): List<*>
}

/** Reflection proxies to the Paper post-effect API (`Player#postEffects()`, added in 26.3), which the framework does not compile against. Null on older servers and on Paper builds without it, where the vanilla `ServerPlayer` methods ([serverPlayer]) are used instead. */
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

    val identifier: PostEffectIdentifierProxy? by lazy {
        runCatching { factory.reflectionProxy(PostEffectIdentifierProxy::class.java) }.getOrNull()
    }

    val serverPlayer: ServerPlayerPostEffectsProxy? by lazy {
        runCatching { factory.reflectionProxy(ServerPlayerPostEffectsProxy::class.java) }.getOrNull()
    }
}
