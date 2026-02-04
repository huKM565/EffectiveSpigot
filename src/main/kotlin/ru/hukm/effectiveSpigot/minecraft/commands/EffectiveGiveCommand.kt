package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils


class EffectiveGiveCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(LanguageModule.getMessage("commands.egive.usage"))
            return true
        }

        val selector = args[0]
        val namespacedKeyName = args[1]
        val count = args[2].toIntOrNull() ?: 0
        val targets = ArrayList<Player>()

        try {
            val entities = Bukkit.selectEntities(sender, selector)
            for (entity in entities) {
                if (entity is Player) {
                    targets.add(entity)
                }
            }
        } catch (e: IllegalArgumentException) {
            val player: Player? = Bukkit.getPlayer(selector)
            if (player != null) {
                targets.add(player)
            }
        }

        if (targets.isEmpty()) {
            sender.sendMessage(LanguageModule.getMessage("commands.egive.player_not_found"))
            return true
        }

        val item = EffectiveItem.getItemByNamespacedKey(namespacedKeyName)
        if (item == null) {
            sender.sendMessage(LanguageModule.getMessage("commands.egive.item_not_found", namespacedKeyName))
            return true
        }

        for (target in targets) {
            for(i in 0 until count) {
                EffectiveInventoryUtils.giveItem(item, target)
            }
        }

        return true
    }
}