package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveScreenEffects

class EffectiveScreenCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("effectivespigot.command.escreen")) {
            sender.sendMessage(LanguageModule.getMessage("commands.escreen.no_permission"))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(LanguageModule.getMessage("commands.escreen.usage"))
            return true
        }

        val selector = args[0]
        val effectType = args[1].uppercase()
        val targets = ArrayList<Player>()

        try {
            val entities = Bukkit.selectEntities(sender, selector)
            for (entity in entities) {
                if (entity is Player) targets.add(entity)
            }
        } catch (e: IllegalArgumentException) {
            val player = Bukkit.getPlayer(selector)
            if (player != null) targets.add(player)
        }

        if (targets.isEmpty()) {
            sender.sendMessage(LanguageModule.getMessage("commands.escreen.player_not_found"))
            return true
        }

        when (effectType) {
            "FADE" -> {
                val fadeIn = args.getOrNull(2)?.toIntOrNull() ?: 10
                val stay = args.getOrNull(3)?.toIntOrNull() ?: 20
                val fadeOut = args.getOrNull(4)?.toIntOrNull() ?: 10
                for (target in targets) {
                    EffectiveScreenEffects.runCameraFade(target, fadeIn, stay, fadeOut)
                }
                sender.sendMessage(LanguageModule.getMessage("commands.escreen.success_fade"))
            }
            "SHAKE" -> {
                val intensity = args[2].toFloatOrNull() ?: 1.0f
                val duration = args[3].toIntOrNull() ?: 20
                val shakeType = try {
                    EffectiveScreenEffects.ShakeType.valueOf(args.getOrNull(4)?.uppercase() ?: "LINEAR")
                } catch (e: IllegalArgumentException) {
                    EffectiveScreenEffects.ShakeType.LINEAR
                }
                for (target in targets) {
                    EffectiveScreenEffects.runCameraShake(target, intensity, duration, shakeType)
                }
                sender.sendMessage(LanguageModule.getMessage("commands.escreen.success_shake"))
            }
            else -> {
                sender.sendMessage(LanguageModule.getMessage("commands.escreen.unknown_effect"))
            }
        }

        return true
    }
}