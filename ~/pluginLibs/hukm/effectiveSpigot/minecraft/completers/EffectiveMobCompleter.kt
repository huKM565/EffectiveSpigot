package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity

class EffectiveMobCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        val lastArg = args.last().lowercase()
        if (args.size == 1) {
            return EffectiveEntity.namespacedKeyToEntity.keys
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()
        }

        return mutableListOf()
    }
}