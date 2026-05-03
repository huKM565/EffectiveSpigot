package ru.hukm.effectiveSpigot.minecraft.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.language.LanguageModule

abstract class EffectiveCommand : BasicCommand {
    companion object {
        val registry = hashMapOf<String, EffectiveCommand>()
    }

    init {
        val namespacedName = getNamespacedName()
        if (registry.containsKey(namespacedName)) {
            throw IllegalArgumentException(
                LanguageModule.getMessage("errors.commands.already_registered", namespacedName)
            )
        }
        registry[namespacedName] = this

        getNamespacedData().first.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                getNamespacedData().second.lowercase(),
                getDescription(),
                emptyList(),
                this
            )
        }
    }

    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    abstract fun getDescription(): String
    abstract fun getPermission(): String
    abstract fun commandTree(): CommandNode?

    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()

    override fun execute(commandSourceStack: CommandSourceStack, args: Array<out String>) {
        val sender = commandSourceStack.sender
        val perm = getPermission()
        if (perm.isNotEmpty() && !sender.hasPermission(perm)) {
            sender.sendMessage(LanguageModule.getMessage("commands.no_permission"))
            return
        }

        val tree = commandTree()
        if (tree != null) {
            var node: CommandNode? = tree
            var executorNode: CommandNode? = if (tree.hasExecutor) tree else null
            for (arg in args) {
                val next = node?.navigate(arg) ?: break
                node = next
                if (node.hasExecutor) executorNode = node
            }
            executorNode?.execute(sender, args)
        }
    }

    override fun suggest(commandSourceStack: CommandSourceStack, args: Array<out String>): Collection<String> {
        val tree = commandTree() ?: return emptyList()
        val sender = commandSourceStack.sender

        var node: CommandNode? = tree
        for (i in 0 until args.size - 1) {
            node = node?.navigate(args[i]) ?: return emptyList()
        }

        val partial = args.lastOrNull()?.lowercase() ?: ""
        return node?.values(sender)?.filter { it.lowercase().startsWith(partial) } ?: emptyList()
    }

    override fun canUse(sender: CommandSender): Boolean {
        val perm = getPermission()
        return perm.isEmpty() || sender.hasPermission(perm)
    }
}
