package ru.hukm.effectiveSpigot

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
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
import ru.hukm.effectiveSpigot.http.EffectiveHttpServer
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveTextureMenu
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveScreenEffects
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZone

class EffectiveSpigot : JavaPlugin() {
	companion object {
		lateinit var instance: EffectiveSpigot
			private set
	}

	override fun onLoad() {
		instance = this

		Config.init()
		Locale.init()

		EffectiveGiveCommand.init()
		EffectiveMobCommand.init()
		EffectiveMenuCommand.init()
		EffectiveScreenCommand.init()
		EffectiveZoneCommand.init()
		EffectiveCompositeCommand.init()
	}

	override fun onEnable() {
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
				EffectiveAdvancement.getModule(),
				EffectiveHttpServer.getModule(),
				EffectiveResourcepack.getModule(),
				EffectiveScreenEffects.getModule(),
				EffectiveTextureMenu.getModule()
			)

		EffectiveItems.ZONE_SELECTOR

		modulesList.forEach { it.init() }
	}

	override fun onDisable() {
		EffectiveHttpServer.stop()
	}
}
