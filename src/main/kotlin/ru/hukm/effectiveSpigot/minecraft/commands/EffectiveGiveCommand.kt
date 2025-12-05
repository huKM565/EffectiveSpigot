package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils


class EffectiveGiveCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String?>): Boolean {
        if (args.size < 3) {
            sender.sendMessage("Usage: /egive <selector> <item_name> <count>")
            return true
        }

        val selector = args[0]!!
        val namespacedKeyName = args[1]!!
        val count = args[2]!!.toInt()
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
            sender.sendMessage("Игроки не найдены.")
            return true
        }

        val item = EffectiveItem.getItemByNamespacedKey(namespacedKeyName)
        if (item == null) {
            sender.sendMessage(ChatColor.RED.toString() + "Error: item is not found")
            return true
        }

        for (target in targets) {
            for(i in 0..count) {
                EffectiveInventoryUtils.giveItem(item, target)
            }
        }

        return true
    }
}