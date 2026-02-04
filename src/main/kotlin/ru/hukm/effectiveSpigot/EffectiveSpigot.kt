package ru.hukm.effectiveSpigot

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.config.ConfigModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveGiveCommand
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveFoundableAndDropable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveWearable
import ru.hukm.effectiveSpigot.minecraft.nms.interfaces.INmsModule
import ru.hukm.effectiveSpigot.minecraft.nms.v1_21_6.NmsModuleV1_21_6
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld
import ru.hukm.effectiveSpigot.utils.debug.Debugger

class EffectiveSpigot : JavaPlugin() {
    companion object{
        lateinit var instance: EffectiveSpigot
            private set
        lateinit var nmsModule: INmsModule
            private set

        private fun initNmsModule() {
            Debugger.info(Bukkit.getServer().version)
            nmsModule = NmsModuleV1_21_6()
        }

        val modulesList: List<IModule> = listOf(
            ConfigModule,
            LanguageModule,
            EffectiveWorld.Companion.EffectiveWorldModule,
            EffectiveFoundableAndDropable.getModule(),
            EffectiveClickable.getModule(),
            EffectiveWearable.getModule()
        )

    }

    override fun onEnable() {
        instance = this

        getCommand("egive")!!.setExecutor(EffectiveGiveCommand())

        modulesList.forEach { it.init() }
        initNmsModule()

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            EffectiveClickable.resetPlayerUUIDInteractedWithEntity()
        }, 0, 1)

//        Bukkit.getScheduler().runTaskTimer(this, Runnable {
//            println("Найдено ${EffectiveWorld.findBlocksByMaterial(Material.ACACIA_STAIRS, Bukkit.getWorlds()[0]).count()}")
//        }, 0, 20)
    }
}
