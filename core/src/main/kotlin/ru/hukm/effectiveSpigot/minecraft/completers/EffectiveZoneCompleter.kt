package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class EffectiveZoneCompleter : TabCompleter {
    private val triggers = listOf("enter", "exit", "stay")

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        val lastArg = args.last().lowercase()
        
        if (args.size == 1) {
            return listOf("list", "create")
                .filter { it.startsWith(lastArg) }
                .toMutableList()
        }

        if (args.size == 2 && args[0].lowercase() == "create") {
            return listOf("<trigger_name>")
                .filter { it.startsWith(lastArg) }
                .toMutableList()
        }

        if (args.size == 3 && args[0].lowercase() == "create" && args[1].lowercase() == "trigger") {
            return triggers
                .filter { it.startsWith(lastArg) }
                .toMutableList()
        }

        return mutableListOf()
    }
}