package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveScreenEffects

class EffectiveScreenCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        val lastArg = args.last().lowercase()

        return when (args.size) {
            1 -> (listOf("@a", "@p") + Bukkit.getOnlinePlayers().map { it.name })
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()

            2 -> listOf("FADE", "SHAKE")
                .filter { it.lowercase().startsWith(lastArg) }
                .toMutableList()

            else -> {
                val effect = args[1].uppercase()
                when (effect) {
                    "FADE" -> when (args.size) {
                        3 -> mutableListOf(LanguageModule.getMessage("commands.escreen.completions.fade_in"))
                        4 -> mutableListOf(LanguageModule.getMessage("commands.escreen.completions.stay"))
                        5 -> mutableListOf(LanguageModule.getMessage("commands.escreen.completions.fade_out"))
                        else -> mutableListOf()
                    }
                    "SHAKE" -> when (args.size) {
                        3 -> mutableListOf(LanguageModule.getMessage("commands.escreen.completions.intensity"))
                        4 -> mutableListOf(LanguageModule.getMessage("commands.escreen.completions.duration"))
                        5 -> EffectiveScreenEffects.ShakeType.entries.map { it.name }
                            .filter { it.startsWith(lastArg, true) }
                            .toMutableList()
                        else -> mutableListOf()
                    }
                    else -> mutableListOf()
                }
            }
        }
    }
}