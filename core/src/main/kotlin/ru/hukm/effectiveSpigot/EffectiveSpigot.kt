package ru.hukm.effectiveSpigot

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.advancements.EffectiveAdvancement
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveCompositeCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveGiveCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMenuCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveMobCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveScreenCommand
import ru.hukm.effectiveSpigot.minecraft.commands.EffectiveZoneCommand
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveCompositeEntity
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable
import ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityLookable
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItems
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveBrewable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveDropable
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveUndropable
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
						Locale.getMessage("errors.unsupported_version", Bukkit.getServer().version)
					)

			mcvModule = EffectiveUtils.loadMcvModule(className, instance) ?: return
		}
	}

	override fun onLoad() {
		instance = this

		Locale.init()

		EffectiveGiveCommand.init()
		EffectiveMobCommand.init()
		EffectiveMenuCommand.init()
		EffectiveScreenCommand.init()
		EffectiveZoneCommand.init()
		EffectiveCompositeCommand.init()
	}

	override fun onEnable() {
		initMcvModule()

		launch {
			while (true) {
				EffectiveClickable.resetPlayerUUIDInteractedWithEntity()
				delay(1.ticks)
			}
		}

		val modulesList =
			listOf<IModule>(
				//EffectiveWorld.Companion.EffectiveWorldModule,
				EffectiveDropable.getModule(),
				EffectiveClickable.getModule(),
				EffectiveWearable.getModule(),
				EffectiveUndropable.getModule(),
				EffectiveEntity.getModule(),
				EffectiveCompositeEntity.getModule(),
				EffectiveEntityInteractable.getModule(),
				EffectiveMenu.getModule(),
				EffectiveEntityLookable.getModule(),
				EffectiveZone.getModule(),
				EffectiveBrewable.getModule(),
				EffectiveAdvancement.getModule()
			)

		EffectiveItems.ZONE_SELECTOR

		modulesList.forEach { it.init() }
	}
}
