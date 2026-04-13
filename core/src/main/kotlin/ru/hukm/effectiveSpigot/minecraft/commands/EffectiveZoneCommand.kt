package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import ru.hukm.effectiveSpigot.language.LanguageModule

class EffectiveZoneCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("effectivespigot.command.ezone")) {
            sender.sendMessage(LanguageModule.getMessage("commands.ezone.no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(LanguageModule.getMessage("commands.ezone.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "list" -> {
                sender.sendMessage("Список зон (заглушка)")
            }
            "create" -> {
                if (args.size < 3 || args[1].lowercase() != "trigger") {
                    sender.sendMessage(LanguageModule.getMessage("commands.ezone.create_usage"))
                    return true
                }
                val triggerType = args[2]
                sender.sendMessage("Создание зоны с триггером: $triggerType (заглушка)")
            }
            else -> {
                sender.sendMessage(LanguageModule.getMessage("commands.ezone.usage"))
            }
        }

        return true
    }
}