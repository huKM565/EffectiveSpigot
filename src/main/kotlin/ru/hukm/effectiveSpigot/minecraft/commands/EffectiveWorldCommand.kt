package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale

/** Built-in `/eworld` command: `/eworld tp <world key> [player]` teleports to the spawn of any loaded world. */
internal object EffectiveWorldCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "eworld")
    override fun getPermission() = "effectivespigot.command.eworld"
    override fun getDescription() = "Teleport between worlds"

    override fun commandTree() = CommandNode.build {
        executes { _ ->
            sendMessage(Locale.getComponent("commands.eworld.usage"))
        }

        choice("tp") {
            executes { _ ->
                sendMessage(Locale.getComponent("commands.eworld.usage"))
            }
            dynamic({ Bukkit.getWorlds().map { it.key.asString() } }) {
                executes { args -> teleport(this, args) }
                dynamic { Bukkit.getOnlinePlayers().map { it.name } }
            }
        }
    }

    private fun teleport(sender: CommandSender, args: Array<out String>) {
        val world = NamespacedKey.fromString(args[1])?.let { Bukkit.getWorld(it) }
        if (world == null) {
            sender.sendMessage(Locale.getComponent("commands.eworld.world_not_found", args[1]))
            return
        }
        val target = args.getOrNull(2)?.let { name ->
            Bukkit.getPlayerExact(name) ?: run {
                sender.sendMessage(Locale.getComponent("commands.eworld.player_not_found", name))
                return
            }
        } ?: sender as? Player ?: run {
            sender.sendMessage(Locale.getComponent("commands.eworld.need_player"))
            return
        }
        target.teleportAsync(world.spawnLocation)
        sender.sendMessage(Locale.getComponent("commands.eworld.teleported", target.name, world.key.asString()))
    }

    fun init() {}
}
