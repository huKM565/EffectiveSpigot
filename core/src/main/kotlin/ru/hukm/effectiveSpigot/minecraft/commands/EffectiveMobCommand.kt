package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity

object EffectiveMobCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "emob")
    override fun getPermission() = "effectivespigot.command.emob"
    override fun getDescription() = "Spawn custom entities"

    override fun commandTree() = CommandNode.build {
        executes { args ->
            if (this !is Player) {
                sendMessage(Locale.getMessage("commands.emob.only_players"))
                return@executes
            }
            if (args.isEmpty()) {
                sendMessage(Locale.getMessage("commands.emob.usage"))
                return@executes
            }
            val entityKey = args[0]
            val effectiveEntity = EffectiveEntity.namespacedKeyToEffectiveEntity[entityKey]
            if (effectiveEntity == null) {
                sendMessage(Locale.getMessage("commands.emob.entity_not_found", entityKey))
                return@executes
            }
            effectiveEntity.spawnEntity(location)
            sendMessage(Locale.getMessage("commands.emob.success", entityKey))
        }
        dynamic { EffectiveEntity.namespacedKeyToEffectiveEntity.keys.toList() }
    }

    fun init() {}
}
