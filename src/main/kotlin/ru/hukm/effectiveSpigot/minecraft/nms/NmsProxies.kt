package ru.hukm.effectiveSpigot.minecraft.nms

import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory
import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type

@Proxies(className = "net.minecraft.world.level.Level")
interface LevelProxy {
    fun getChunk(instance: Any, chunkX: Int, chunkZ: Int): Any
}

@Proxies(className = "net.minecraft.world.level.LevelHeightAccessor")
interface LevelHeightAccessorProxy {
    fun getHeight(instance: Any): Int

    fun getSectionsCount(instance: Any): Int
}

@Proxies(className = "net.minecraft.world.level.chunk.ChunkAccess")
interface ChunkAccessProxy {
    fun getSection(instance: Any, index: Int): Any
}

@Proxies(className = "net.minecraft.world.level.chunk.LevelChunkSection")
interface LevelChunkSectionProxy {
    fun hasOnlyAir(instance: Any): Boolean

    fun getBlockState(instance: Any, x: Int, y: Int, z: Int): Any
}

@Proxies(className = "net.minecraft.world.level.block.state.BlockBehaviour\$BlockStateBase")
interface BlockStateProxy {
    fun getBlock(instance: Any): Any
}

@Proxies(className = "net.minecraft.server.level.ServerPlayer")
interface ServerPlayerProxy {
    @FieldGetter("connection")
    fun connection(instance: Any): Any
}

@Proxies(className = "net.minecraft.server.network.ServerCommonPacketListenerImpl")
interface ConnectionProxy {
    fun send(
        instance: Any,
        @Type(className = "net.minecraft.network.protocol.Packet") packet: Any
    )
}

@Proxies(className = "net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket")
interface RotationPacketProxy {
    @ConstructorInvoker
    fun create(yaw: Float, relativeYaw: Boolean, pitch: Float, relativePitch: Boolean): Any
}

@Proxies(className = "net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket")
interface AbsoluteRotationPacketProxy {
    @ConstructorInvoker
    fun create(yaw: Float, pitch: Float): Any
}

@Proxies(className = "net.minecraft.world.entity.LivingEntity")
interface LivingEntityProxy {
    fun getPreciseBodyRotation(instance: Any, partialTick: Float): Float
}

@Proxies(className = "net.minecraft.network.protocol.game.ClientboundSetExperiencePacket")
interface ExperiencePacketProxy {
    @ConstructorInvoker
    fun create(progress: Float, totalExperience: Int, level: Int): Any
}

object NmsProxies {
    val remapper: ReflectionRemapper by lazy { ReflectionRemapper.forReobfMappingsInPaperJar() }

    private val factory: ReflectionProxyFactory by lazy {
        ReflectionProxyFactory.create(remapper, javaClass.classLoader)
    }

    val level: LevelProxy by lazy { factory.reflectionProxy(LevelProxy::class.java) }
    val heightAccessor: LevelHeightAccessorProxy by lazy { factory.reflectionProxy(LevelHeightAccessorProxy::class.java) }
    val chunkAccess: ChunkAccessProxy by lazy { factory.reflectionProxy(ChunkAccessProxy::class.java) }
    val chunkSection: LevelChunkSectionProxy by lazy { factory.reflectionProxy(LevelChunkSectionProxy::class.java) }
    val blockState: BlockStateProxy by lazy { factory.reflectionProxy(BlockStateProxy::class.java) }
    val serverPlayer: ServerPlayerProxy by lazy { factory.reflectionProxy(ServerPlayerProxy::class.java) }
    val connection: ConnectionProxy by lazy { factory.reflectionProxy(ConnectionProxy::class.java) }
    val rotationPacket: RotationPacketProxy? by lazy { runCatching { factory.reflectionProxy(RotationPacketProxy::class.java) }.getOrNull() }
    val absoluteRotationPacket: AbsoluteRotationPacketProxy by lazy { factory.reflectionProxy(AbsoluteRotationPacketProxy::class.java) }
    val livingEntity: LivingEntityProxy by lazy { factory.reflectionProxy(LivingEntityProxy::class.java) }
    val experiencePacket: ExperiencePacketProxy by lazy { factory.reflectionProxy(ExperiencePacketProxy::class.java) }
}
