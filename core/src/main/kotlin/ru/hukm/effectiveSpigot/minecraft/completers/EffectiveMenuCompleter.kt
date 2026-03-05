package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu

class EffectiveMenuCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        if (args.size == 1) {
            val lastArg = args.last().lowercase()
            return EffectiveMenu.namespacedNameToMenu.keys
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()
        }

        return mutableListOf()
    }
}
