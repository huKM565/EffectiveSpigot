package ru.hukm.effectiveSpigot.minecraft.additional

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale

data class AdditionalArgs(
    val instance: JavaPlugin,
    val keys: List<Pair<String, PersistentDataType<*, *>>>,
)

object AdditionalArgsSupport {

    fun applyToHolder(
        holder: PersistentDataHolder,
        args: AdditionalArgs?,
        rawValues: List<String>,
        localeScope: String,
    ) {
        if (rawValues.isEmpty()) return
        if (args == null || args.keys.isEmpty()) return

        if (rawValues.size != args.keys.size) throw IllegalArgumentException(
            Locale.getMessage(
                "errors.$localeScope.wrong_additional_args_count",
                args.keys.size, rawValues.size
            )
        )

        args.keys.forEachIndexed { index, (name, type) ->
            val raw = rawValues[index]
            val value = parseArg(raw, type) ?: throw IllegalArgumentException(
                Locale.getMessage(
                    "errors.$localeScope.cannot_parse_additional_arg",
                    index + 1, raw, name
                )
            )
            @Suppress("UNCHECKED_CAST")
            holder.persistentDataContainer.set(
                NamespacedKey(args.instance, name),
                type as PersistentDataType<Any, Any>,
                value
            )
        }
    }

    fun readFromHolder(holder: PersistentDataHolder, args: AdditionalArgs?): List<String> {
        if (args == null || args.keys.isEmpty()) return emptyList()

        val container = holder.persistentDataContainer

        return args.keys.map { (name, type) ->
            @Suppress("UNCHECKED_CAST")
            val value = container.get(
                NamespacedKey(args.instance, name),
                type as PersistentDataType<Any, Any>
            ) ?: return emptyList()

            serializeArg(value)
        }
    }

    private fun serializeArg(value: Any): String = when (value) {
        is ByteArray -> value.joinToString(",")
        is IntArray  -> value.joinToString(",")
        is LongArray -> value.joinToString(",")
        else         -> value.toString()
    }

    fun additionalKey(args: AdditionalArgs?, name: String, localeScope: String): NamespacedKey {
        val a = args ?: throw IllegalStateException(
            Locale.getMessage("errors.$localeScope.no_additional_args_defined")
        )
        require(a.keys.any { it.first == name }) {
            Locale.getMessage("errors.$localeScope.unknown_additional_arg_key", name)
        }
        return NamespacedKey(a.instance, name)
    }

    fun namespacedKeys(args: AdditionalArgs?): List<Pair<NamespacedKey, PersistentDataType<*, *>>>? {
        val a = args ?: return null
        return a.keys.map { NamespacedKey(a.instance, it.first) to it.second }
    }

    private fun parseArg(raw: String, type: PersistentDataType<*, *>): Any? = when (type) {
        PersistentDataType.STRING        -> raw
        PersistentDataType.BYTE          -> raw.toByteOrNull()
        PersistentDataType.SHORT         -> raw.toShortOrNull()
        PersistentDataType.INTEGER       -> raw.toIntOrNull()
        PersistentDataType.LONG          -> raw.toLongOrNull()
        PersistentDataType.FLOAT         -> raw.toFloatOrNull()
        PersistentDataType.DOUBLE        -> raw.toDoubleOrNull()
        PersistentDataType.BOOLEAN       -> raw.toBooleanStrictOrNull()
        PersistentDataType.BYTE_ARRAY    -> raw.split(",").mapNotNull { it.trim().toByteOrNull() }.toByteArray()
        PersistentDataType.INTEGER_ARRAY -> raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
        PersistentDataType.LONG_ARRAY    -> raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toLongArray()
        else -> null
    }
}
