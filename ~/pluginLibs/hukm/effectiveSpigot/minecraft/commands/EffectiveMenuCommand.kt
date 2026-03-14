package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu

class EffectiveMenuCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(LanguageModule.getMessage("commands.emenu.only_players"))
            return true
        }

        if (!sender.hasPermission("effectivespigot.command.emenu")) {
            sender.sendMessage(LanguageModule.getMessage("commands.emenu.no_permission"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(LanguageModule.getMessage("commands.emenu.usage"))
            return true
        }

        val menuName = args[0]
        val menu = EffectiveMenu.namespacedNameToMenu[menuName]

        if (menu == null) {
            sender.sendMessage(LanguageModule.getMessage("commands.emenu.menu_not_found", menuName))
            return true
        }

        sender.openInventory(menu.getMenu())
        return true
    }
}
