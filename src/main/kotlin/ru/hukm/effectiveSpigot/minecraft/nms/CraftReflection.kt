package ru.hukm.effectiveSpigot.minecraft.nms

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

object CraftReflection {
    private val lookup = MethodHandles.lookup()

    private val craftBukkitPackage: String by lazy {
        Bukkit.getServer().javaClass.packageName
    }

    private val worldGetHandle: MethodHandle by lazy {
        val craftWorld = Class.forName("$craftBukkitPackage.CraftWorld")
        lookup.unreflect(craftWorld.getMethod("getHandle"))
    }

    private val playerGetHandle: MethodHandle by lazy {
        val craftPlayer = Class.forName("$craftBukkitPackage.entity.CraftPlayer")
        lookup.unreflect(craftPlayer.getMethod("getHandle"))
    }

    private val getMaterialByBlock: MethodHandle by lazy {
        val magicNumbers = Class.forName("$craftBukkitPackage.util.CraftMagicNumbers")
        val nmsBlockName = NmsProxies.remapper.remapClassName("net.minecraft.world.level.block.Block")
        val method = magicNumbers.methods.first {
            it.name == "getMaterial" && it.parameterTypes.size == 1 && it.parameterTypes[0].name == nmsBlockName
        }
        lookup.unreflect(method)
    }

    fun getWorldHandle(world: World): Any = worldGetHandle.invoke(world)

    fun getPlayerHandle(player: Player): Any = playerGetHandle.invoke(player)

    fun getMaterial(nmsBlock: Any): Material = getMaterialByBlock.invoke(nmsBlock) as Material
}
