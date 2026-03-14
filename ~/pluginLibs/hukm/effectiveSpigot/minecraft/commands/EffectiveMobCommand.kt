package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity

class EffectiveMobCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(LanguageModule.getMessage("commands.emob.only_players"))
            return true
        }

        if (!sender.hasPermission("effectivespigot.command.emob")) {
            sender.sendMessage(LanguageModule.getMessage("commands.emob.no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(LanguageModule.getMessage("commands.emob.usage"))
            return true
        }

        val entityKey = args[0]
        val effectiveEntity = EffectiveEntity.namespacedKeyToEntity[entityKey]

        if (effectiveEntity == null) {
            sender.sendMessage(LanguageModule.getMessage("commands.emob.entity_not_found", entityKey))
            return true
        }

        effectiveEntity.spawnEntity(sender.location)
        sender.sendMessage(LanguageModule.getMessage("commands.emob.success", entityKey))

        return true
    }
}