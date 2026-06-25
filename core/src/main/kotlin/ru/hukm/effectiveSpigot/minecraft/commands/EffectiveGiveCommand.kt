package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils

object EffectiveGiveCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "egive")
    override fun getPermission() = "effectivespigot.command.egive"
    override fun getDescription() = "Give custom items to players"

    override fun commandTree() = CommandNode.build {
        executes { args ->
            if (args.size < 2) {
                sendMessage(Locale.getMessage("commands.egive.usage"))
                return@executes
            }
            val targets = ArrayList<Player>()
            try { Bukkit.selectEntities(this, args[0]).filterIsInstanceTo(targets) }
            catch (e: IllegalArgumentException) { Bukkit.getPlayer(args[0])?.let { targets.add(it) } }

            if (targets.isEmpty()) {
                sendMessage(Locale.getMessage("commands.egive.player_not_found"))
                return@executes
            }
            val effectiveItem = EffectiveItem.getEffectiveItemByNamespacedKey(args[1])
            if (effectiveItem == null) {
                sendMessage(Locale.getMessage("commands.egive.item_not_found", args[1]))
                return@executes
            }
            val count = args.getOrNull(2)?.toIntOrNull() ?: 1

            val additionalArgs = args.drop(3)

            for (target in targets) repeat(count) {
                EffectiveInventoryUtils.giveItem(effectiveItem.createItemStack(additionalArgs), target)
            }
        }
        dynamic({ listOf("@a", "@p") + Bukkit.getOnlinePlayers().map { it.name } }) {
            dynamic({ EffectiveItem.namespacedKeyToItem.keys.toList() }) {
                dynamic { listOf("<count>") }
            }
        }
    }

    fun init() {}
}
