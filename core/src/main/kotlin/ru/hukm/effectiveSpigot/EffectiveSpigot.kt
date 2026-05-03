package ru.hukm.effectiveSpigot

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveGiveCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMenuCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMobCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveScreenCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveZoneCommand
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityLookable
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItems
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveDropable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveWearable
import ru.hukm.effectiveSpigot.minecraft.mcv.interfaces.IMcvEffectiveModule
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZone
import ru.hukm.effectiveSpigot.utils.EffectiveUtils

class EffectiveSpigot : JavaPlugin() {
  companion object {
    lateinit var instance: EffectiveSpigot
      private set
    lateinit var mcvModule: IMcvEffectiveModule
      private set

    private fun initMcvModule() {
      val version =
              Bukkit.getServer().version.split(".").map {
                it.take(2).filter { it.isDigit() }.toInt()
              }

      EffectiveEntity.namespacedKeyToEffectiveEntity

      val className =
              if (version[1] == 21) {
                if (version[2] >= 11)
                        "ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_11.McvModuleV1_21_11"
                else "ru.hukm.effectiveSpigot.minecraft.mcv.v1_21_9.McvModuleV1_21_9"
              } else
                      throw IllegalArgumentException(
                              LanguageModule.getMessage("errors.unsupported_version", Bukkit.getServer().version)
                      )

      mcvModule = EffectiveUtils.loadMcvModule(className, instance) ?: return
    }
  }

  override fun onLoad() {
    instance = this

    LanguageModule.init()

      EffectiveGiveCommand.init()
      EffectiveMobCommand.init()
      EffectiveMenuCommand.init()
    EffectiveScreenCommand.init()
    EffectiveZoneCommand.init()
  }

  override fun onEnable() {
    initMcvModule()

    Bukkit.getScheduler()
            .runTaskTimer(
                this,
                Runnable { EffectiveClickable.resetPlayerUUIDInteractedWithEntity() },
                0,
                1
            )

    val modulesList =
            listOf<IModule>(
                LanguageModule,
                EffectiveWorld.Companion.EffectiveWorldModule,
                EffectiveDropable.getModule(),
                EffectiveClickable.getModule(),
                EffectiveWearable.getModule(),
                EffectiveEntity.getModule(),
                EffectiveEntityInteractable.getModule(),
                EffectiveMenu.getModule(),
                EffectiveEntityLookable.getModule(),
                EffectiveZone.getModule()
            )

    EffectiveItems.ZONE_SELECTOR

    modulesList.forEach { it.init() }
  }
}
