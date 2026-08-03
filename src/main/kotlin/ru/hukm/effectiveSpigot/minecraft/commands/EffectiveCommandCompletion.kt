package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.command.CommandSender

/**
 * An immutable node of a command's argument tree: the completions offered at this position, how to
 * navigate to the next node for a typed argument, and an optional executor.
 *
 * Build one with [build] and the [NodeBuilder] DSL:
 * ```kotlin
 * override fun commandTree() = CommandNode.build {
 *     choice("give") {
 *         dynamic({ EffectiveItem.namespacedKeyToItem.keys.toList() }) {
 *             executes { args -> /* give args[1] */ }
 *         }
 *     }
 * }
 * ```
 */
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
        /** Builds a [CommandNode] tree from a [NodeBuilder] DSL block. */
        fun build(block: NodeBuilder.() -> Unit): CommandNode = NodeBuilder().apply(block).build()
    }
}

/** DSL builder for a [CommandNode]: declare static [choice]s, [dynamic] completions and an [executes] body. */
class NodeBuilder internal constructor() {
    private val staticChoices = linkedMapOf<String, NodeBuilder?>()
    private val dynamicProviders = mutableListOf<(CommandSender) -> List<String>>()
    private var dynamicChild: NodeBuilder? = null
    private var nodeExecutor: (CommandSender.(Array<out String>) -> Unit)? = null

    /** Adds a literal completion [value] with no further arguments. */
    fun choice(value: String) {
        staticChoices[value.lowercase()] = null
    }

    /** Adds a literal completion [value] with a nested sub-tree built by [block]. */
    fun choice(value: String, block: NodeBuilder.() -> Unit) {
        staticChoices[value.lowercase()] = NodeBuilder().apply(block)
    }

    /** Adds a runtime-computed set of completions from [provider]. */
    fun dynamic(provider: (CommandSender) -> List<String>) {
        dynamicProviders += provider
    }

    /** Adds runtime completions from [provider] with a nested sub-tree (for the next argument) via [block]. */
    fun dynamic(provider: (CommandSender) -> List<String>, block: NodeBuilder.() -> Unit) {
        dynamicProviders += provider
        dynamicChild = NodeBuilder().apply(block)
    }

    /** Sets the executor run when the command ends at this node; receiver is the sender, param the args. */
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
