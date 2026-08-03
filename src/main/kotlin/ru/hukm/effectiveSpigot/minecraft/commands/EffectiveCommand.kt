package ru.hukm.effectiveSpigot.minecraft.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale

/**
 * Base class for a Brigadier-backed command.
 *
 * A subclass declares identity ([getNamespacedData]), [getDescription], a required [getPermission]
 * (empty string = no permission) and a [commandTree] built with [CommandNode]. The command registers
 * itself with the owning plugin's lifecycle on construction; execution walks the tree by argument and
 * runs the deepest matching executor, and tab-completion is driven by the same tree.
 *
 * Instantiate it from `onLoad` (not `onEnable`) — Brigadier commands register during the server's load
 * phase, so a command touched only in `onEnable` won't be registered.
 */
abstract class EffectiveCommand : BasicCommand {
    companion object {
        private val _registry = hashMapOf<String, EffectiveCommand>()

        /** Read-only registry of all constructed commands, keyed by [getNamespacedName]. */
        val registry: Map<String, EffectiveCommand> get() = _registry
    }

    init {
        val namespacedName = getNamespacedName()
        if (registry.containsKey(namespacedName)) {
            throw IllegalArgumentException(
                Locale.getMessage("errors.commands.already_registered", namespacedName)
            )
        }
        _registry[namespacedName] = this

        getNamespacedData().first.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                getNamespacedData().second.lowercase(),
                getDescription(),
                emptyList(),
                this
            )
        }
    }

    /** Owning plugin and the command's literal name (its second element). */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Short command description shown in help. */
    abstract fun getDescription(): String

    /** Permission node required to run the command; empty string means no permission check. */
    abstract fun getPermission(): String

    /** The argument tree (choices, dynamic completions, executors), or null for no arguments. */
    abstract fun commandTree(): CommandNode?

    /** Unique identity as `"<plugin-name>/<command>"`, both lowercased. */
    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()

    override fun execute(commandSourceStack: CommandSourceStack, args: Array<out String>) {
        val sender = commandSourceStack.sender
        val perm = getPermission()
        if (perm.isNotEmpty() && !sender.hasPermission(perm)) {
            sender.sendMessage(Locale.getComponent("commands.no_permission"))
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
