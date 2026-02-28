package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem


class EffectiveGiveCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        val lastArg = args.last().lowercase()
        if (args.size == 1) {
            return (listOf("@a", "@p") + Bukkit.getOnlinePlayers().map(Player::getName))
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()
        }

        if (args.size == 2) {
            return EffectiveItem.namespacedKeyToItem.keys
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()
        }

        if (args.size == 3) {
            return mutableListOf("<count>")
        }

        return mutableListOf()
    }

}