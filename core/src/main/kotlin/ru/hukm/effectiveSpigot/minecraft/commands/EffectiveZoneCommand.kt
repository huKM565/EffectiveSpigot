package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZone
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZoneSelection

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
                EffectiveZone.namespacedKeyToZone.values.forEach {
                    sender.sendMessage(it.getNamespacedName())
                    for (zoneBox in it.zoneBoxes) {
                        sender.sendMessage(" - id: ${zoneBox.id}")
                        sender.sendMessage(" ${zoneBox.firstPos.x} ${zoneBox.firstPos.y} ${zoneBox.firstPos.z} - ${zoneBox.secondPos.x} ${zoneBox.secondPos.y} ${zoneBox.secondPos.z}")
                    }
                }
            }
            "create" -> {
                if (sender !is Player) {
                    sender.sendMessage(LanguageModule.getMessage("commands.ezone.not_player"))
                } else {
                    if (args.size < 2) {
                        sender.sendMessage(LanguageModule.getMessage("commands.ezone.create_usage"))
                        sender.sendMessage(LanguageModule.getMessage("commands.ezone.available_zones"))
                        EffectiveZone.namespacedKeyToZone.keys.forEach { zoneName ->
                            sender.sendMessage(" - $zoneName")
                        }
                        return true
                    }
                    val zoneType = args[1]

                    val selection = EffectiveZoneSelection.getSelection(sender.uniqueId)

                    if (selection == null || selection.first == null || selection.second == null) {
                        sender.sendMessage(LanguageModule.getMessage("commands.ezone.no_selection"))
                        return true
                    }

                    val firstPos = selection.first!!
                    val secondPos = selection.second!!

                    EffectiveZone.registerSelection(Triple(firstPos, secondPos, selection.third), zoneType)

                    sender.sendMessage(LanguageModule.getMessage("commands.ezone.create_success", zoneType))
                }


            }
            else -> {
                sender.sendMessage(LanguageModule.getMessage("commands.ezone.usage"))
            }
        }

        return true
    }
}