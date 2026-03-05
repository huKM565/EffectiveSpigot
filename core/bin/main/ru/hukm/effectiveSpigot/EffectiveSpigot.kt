package ru.hukm.effectiveSpigot

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.config.ConfigModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveGiveCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMenuCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMobCommand
import ru.hukm.effectiveSpigot.minecraft.completers.EffectiveGiveCompleter
import ru.hukm.effectiveSpigot.minecraft.completers.EffectiveMenuCompleter
import ru.hukm.effectiveSpigot.minecraft.completers.EffectiveMobCompleter
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveFoundableAndDropable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveWearable
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.IMcvEffectiveModule
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu
import ru.hukm.effectiveSpigot.utils.EffectiveUtils
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld

class EffectiveSpigot : JavaPlugin() {
    companion object{
        lateinit var instance: EffectiveSpigot
            private set
        lateinit var mcvModule: IMcvEffectiveModule
            private set

        private fun initMcvModule() {
            val version = Bukkit.getServer().version.split(".").map {
                it.take(2).filter { it.isDigit() } .toInt()
            }

            val className = if (version[1] == 21) {
                if (version[2] >= 11) "ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_11.McvModuleV1_21_11"
                else "ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.McvModuleV1_21_9"
            } else throw IllegalArgumentException("Unsupported version: ${Bukkit.getServer().version}")

            mcvModule = EffectiveUtils.loadMcvModule(className, instance) ?: return
        }

    }

    override fun onEnable() {
        instance = this

        getCommand("egive")!!.let {
            it.setExecutor(EffectiveGiveCommand())
            it.tabCompleter = EffectiveGiveCompleter()
        }
        getCommand("emob")!!.let {
            it.setExecutor(EffectiveMobCommand())
            it.tabCompleter = EffectiveMobCompleter()

        }
        getCommand("emenu")!!.let {
            it.setExecutor(EffectiveMenuCommand())
            it.tabCompleter = EffectiveMenuCompleter()
        }
        initMcvModule()

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            EffectiveClickable.resetPlayerUUIDInteractedWithEntity()
        }, 0, 1)

        val modulesList = listOf<IModule>(
            ConfigModule,
            LanguageModule,
            EffectiveWorld.Companion.EffectiveWorldModule,
            EffectiveFoundableAndDropable.getModule(),
            EffectiveClickable.getModule(),
            EffectiveWearable.getModule(),
            EffectiveEntity.getModule(),
            EffectiveEntityInteractable.getModule(),
            EffectiveMenu.getModule()
        )

        modulesList.forEach { it.init() }

//        Bukkit.getScheduler().runTaskTimer(this, Runnable {
//            println("Найдено ${EffectiveWorld.findBlocksByMaterial(Material.ACACIA_STAIRS, Bukkit.getWorlds()[0]).count()}")
//        }, 0, 20)
    }
}
