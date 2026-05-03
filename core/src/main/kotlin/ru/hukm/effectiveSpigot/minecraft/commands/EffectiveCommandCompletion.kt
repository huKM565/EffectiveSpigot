package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.CommandSender

class CommandNode internal constructor(
    private val valuesProvider: (CommandSender) -> List<String>,
    private val navigator: (String) -> CommandNode?,
    internal val executor: (CommandSender.(Array<out String>) -> Unit)?
) {
    internal fun values(sender: CommandSender): List<String> = valuesProvider(sender)
    internal fun navigate(typed: String): CommandNode? = navigator(typed)
    internal fun execute(sender: CommandSender, args: Array<out String>) = executor?.invoke(sender, args)
    internal val hasExecutor get() = executor != null

    companion object {
        fun build(block: NodeBuilder.() -> Unit): CommandNode = NodeBuilder().apply(block).build()
    }
}

class NodeBuilder internal constructor() {
    private val staticChoices = linkedMapOf<String, NodeBuilder?>()
    private val dynamicProviders = mutableListOf<(CommandSender) -> List<String>>()
    private var dynamicChild: NodeBuilder? = null
    private var nodeExecutor: (CommandSender.(Array<out String>) -> Unit)? = null

    fun choice(value: String) {
        staticChoices[value.lowercase()] = null
    }

    fun choice(value: String, block: NodeBuilder.() -> Unit) {
        staticChoices[value.lowercase()] = NodeBuilder().apply(block)
    }

    fun dynamic(provider: (CommandSender) -> List<String>) {
        dynamicProviders += provider
    }

    fun dynamic(provider: (CommandSender) -> List<String>, block: NodeBuilder.() -> Unit) {
        dynamicProviders += provider
        dynamicChild = NodeBuilder().apply(block)
    }

    fun executes(block: CommandSender.(Array<out String>) -> Unit) {
        nodeExecutor = block
    }

    internal fun build(): CommandNode {
        val children = staticChoices.mapValues { it.value?.build() }
        val fallback = dynamicChild?.build()
        val providers = dynamicProviders.toList()
        val exec = nodeExecutor

        return CommandNode(
            valuesProvider = { sender ->
                buildList {
                    addAll(children.keys)
                    providers.forEach { addAll(it(sender)) }
                }
            },
            navigator = { typed ->
                val key = typed.lowercase()
                if (children.containsKey(key)) children[key] else fallback
            },
            executor = exec
        )
    }
}
