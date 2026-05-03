package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils

object EffectiveGiveCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "egive")
    override fun getPermission() = "effectivespigot.command.egive"
    override fun getDescription() = "Give custom items to players"

    override fun commandTree() = CommandNode.build {
        executes { args ->
            if (args.size < 3) {
                sendMessage(LanguageModule.getMessage("commands.egive.usage"))
                return@executes
            }
            val targets = ArrayList<Player>()
            try { Bukkit.selectEntities(this, args[0]).filterIsInstanceTo(targets) }
            catch (e: IllegalArgumentException) { Bukkit.getPlayer(args[0])?.let { targets.add(it) } }

            if (targets.isEmpty()) {
                sendMessage(LanguageModule.getMessage("commands.egive.player_not_found"))
                return@executes
            }
            val item = EffectiveItem.getItemByNamespacedKey(args[1])
            if (item == null) {
                sendMessage(LanguageModule.getMessage("commands.egive.item_not_found", args[1]))
                return@executes
            }
            val count = args[2].toIntOrNull() ?: 0
            for (target in targets) repeat(count) { EffectiveInventoryUtils.giveItem(item, target) }
        }
        dynamic({ listOf("@a", "@p") + Bukkit.getOnlinePlayers().map { it.name } }) {
            dynamic({ EffectiveItem.namespacedKeyToItem.keys.toList() }) {
                dynamic { listOf("<count>") }
            }
        }
    }

    fun init() {}
}
