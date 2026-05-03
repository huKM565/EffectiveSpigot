package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveMenu

object EffectiveMenuCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "emenu")
    override fun getPermission() = "effectivespigot.command.emenu"
    override fun getDescription() = "Open custom menus"

    override fun commandTree() = CommandNode.build {
        executes { args ->
            if (this !is Player) {
                sendMessage(LanguageModule.getMessage("commands.emenu.only_players"))
                return@executes
            }
            if (args.isEmpty()) {
                sendMessage(LanguageModule.getMessage("commands.emenu.usage"))
                return@executes
            }
            val menu = EffectiveMenu.namespacedNameToMenu[args[0]]
            if (menu == null) {
                sendMessage(LanguageModule.getMessage("commands.emenu.menu_not_found", args[0]))
                return@executes
            }
            openInventory(menu.getMenu())
        }
        dynamic { EffectiveMenu.namespacedNameToMenu.keys.toList() }
    }

    fun init() {}
}
