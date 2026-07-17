package ru.hukm.effectiveSpigot.minecraft.nms

import xyz.jpenilla.reflectionremapper.ReflectionRemapper
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory
import xyz.jpenilla.reflectionremapper.proxy.annotation.ConstructorInvoker
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Type

@Proxies(className = "net.minecraft.server.level.ServerLevel")
interface ServerLevelProxy {
    fun getChunk(
        @Type(className = "net.minecraft.server.level.ServerLevel") instance: Any,
        chunkX: Int,
        chunkZ: Int
    ): Any

    fun getSectionsCount(@Type(className = "net.minecraft.server.level.ServerLevel") instance: Any): Int

    fun getHeight(@Type(className = "net.minecraft.server.level.ServerLevel") instance: Any): Int
}

@Proxies(className = "net.minecraft.world.level.chunk.LevelChunk")
interface LevelChunkProxy {
    fun getSection(
        @Type(className = "net.minecraft.world.level.chunk.LevelChunk") instance: Any,
        index: Int
    ): Any
}

@Proxies(className = "net.minecraft.world.level.chunk.LevelChunkSection")
interface LevelChunkSectionProxy {
    fun hasOnlyAir(@Type(className = "net.minecraft.world.level.chunk.LevelChunkSection") instance: Any): Boolean

    fun getBlockState(
        @Type(className = "net.minecraft.world.level.chunk.LevelChunkSection") instance: Any,
        x: Int,
        y: Int,
        z: Int
    ): Any
}

@Proxies(className = "net.minecraft.world.level.block.state.BlockState")
interface BlockStateProxy {
    fun getBlock(@Type(className = "net.minecraft.world.level.block.state.BlockState") instance: Any): Any
}

@Proxies(className = "net.minecraft.server.level.ServerPlayer")
interface ServerPlayerProxy {
    @FieldGetter("connection")
    fun connection(@Type(className = "net.minecraft.server.level.ServerPlayer") instance: Any): Any
}

@Proxies(className = "net.minecraft.server.network.ServerGamePacketListenerImpl")
interface ConnectionProxy {
    fun send(
        @Type(className = "net.minecraft.server.network.ServerGamePacketListenerImpl") instance: Any,
        @Type(className = "net.minecraft.network.protocol.Packet") packet: Any
    )
}

@Proxies(className = "net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket")
interface RotationPacketProxy {
    @ConstructorInvoker
    fun create(yaw: Float, relativeYaw: Boolean, pitch: Float, relativePitch: Boolean): Any
}

object NmsProxies {
    val remapper: ReflectionRemapper by lazy { ReflectionRemapper.forReobfMappingsInPaperJar() }

    private val factory: ReflectionProxyFactory by lazy {
        ReflectionProxyFactory.create(remapper, javaClass.classLoader)
    }

    val serverLevel: ServerLevelProxy by lazy { factory.reflectionProxy(ServerLevelProxy::class.java) }
    val levelChunk: LevelChunkProxy by lazy { factory.reflectionProxy(LevelChunkProxy::class.java) }
    val chunkSection: LevelChunkSectionProxy by lazy { factory.reflectionProxy(LevelChunkSectionProxy::class.java) }
    val blockState: BlockStateProxy by lazy { factory.reflectionProxy(BlockStateProxy::class.java) }
    val serverPlayer: ServerPlayerProxy by lazy { factory.reflectionProxy(ServerPlayerProxy::class.java) }
    val connection: ConnectionProxy by lazy { factory.reflectionProxy(ConnectionProxy::class.java) }
    val rotationPacket: RotationPacketProxy by lazy { factory.reflectionProxy(RotationPacketProxy::class.java) }
}
