package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class EffectiveGiveCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String?>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("Использование: /egive <селектор> <имя_ресурса>")
            return true
        }


        val selector = args[0]!!
        val namespacedKeyName = args[1]!!
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

        for (target in targets) {
            target.sendMessage("Привет!")
        }

        return true
    }
}