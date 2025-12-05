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
        args: Array<out String?>
    ): List<String?>? {
        if (args.size == 1) {
            return listOf("@a", "@p") + Bukkit.getOnlinePlayers().map(Player::getName)
        }

        if (args.size == 2) {
            return EffectiveItem.namespacedKeyToItem.keys.toList()
        }

        if (args.size == 3) {
            return listOf("<count>")
        }

        return null
    }

}