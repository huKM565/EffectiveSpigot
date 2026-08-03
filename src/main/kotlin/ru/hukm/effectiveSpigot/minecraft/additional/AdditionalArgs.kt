package ru.hukm.effectiveSpigot.minecraft.additional

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale

/**
 * Declares the extra, per-instance parameters an item/entity accepts (e.g. via `/egive item <arg>`).
 *
 * [keys] is the schema: an ordered list of `name to PersistentDataType` pairs. Each pair says
 * *what the parameter is called* (the [String] — used to build its `NamespacedKey(instance, name)`)
 * and *what type it holds* (the [PersistentDataType] — how the raw string is parsed and stored). The
 * order matters: positional command/`createItemStack` arguments are matched to keys in this order.
 *
 * Build it with Kotlin's `to` and the standard `PersistentDataType` constants:
 * ```kotlin
 * AdditionalArgs(
 *     ExamplePlugin.instance,
 *     listOf(
 *         "radius" to PersistentDataType.INTEGER,   // /egive example 5
 *         "owner"  to PersistentDataType.STRING,    // /egive example 5 Steve
 *     )
 * )
 * ```
 * Supported types are the scalar PDC constants (`STRING`, `INTEGER`, `LONG`, `DOUBLE`, `FLOAT`, `BYTE`,
 * `SHORT`, `BOOLEAN`) and the comma-separated arrays (`BYTE_ARRAY`, `INTEGER_ARRAY`, `LONG_ARRAY`).
 *
 * @property instance owning plugin (namespaces the keys)
 * @property keys ordered `name to PersistentDataType` schema; arguments are parsed positionally in this order
 */
data class AdditionalArgs(
    val instance: JavaPlugin,
    val keys: List<Pair<String, PersistentDataType<*, *>>>,
)

/**
 * Backend that turns the [AdditionalArgs] declaration into actual persistent-data reads and writes.
 *
 * It is the glue behind `EffectiveItem.getAdditionalArgs` / `EffectiveEntity.getAdditionalArgs`: it
 * parses raw string arguments (from `createItemStack(args)` / `spawnEntity(loc, args)` or the `/egive`
 * and `/emob` commands) into typed values, stores them on the holder's [PersistentDataHolder], and reads
 * them back. You rarely call it directly — the base classes and commands do — but it is public so custom
 * commands or tooling can reuse the same parsing.
 *
 * **Value formats.** Each declared key has a [PersistentDataType]; a raw string is parsed accordingly:
 * scalar types (`STRING`, `BYTE`, `SHORT`, `INTEGER`, `LONG`, `FLOAT`, `DOUBLE`, `BOOLEAN`) from a single
 * token, and array types (`BYTE_ARRAY`, `INTEGER_ARRAY`, `LONG_ARRAY`) from a comma-separated list.
 * Unsupported types fail to parse. When read back, values are stringified (arrays comma-joined) so they
 * round-trip through commands and lore.
 *
 * **`localeScope`.** Several methods take a `localeScope` (`"items"` or `"entities"`) used only to pick
 * the message key for errors — e.g. `errors.items.cannot_parse_additional_arg` — so item and entity
 * failures read naturally.
 */
object AdditionalArgsSupport {

    /**
     * Parses [rawValues] (positional, one per declared key in [args]) into their [PersistentDataType]s
     * and writes each onto [holder]'s persistent-data container under `NamespacedKey(plugin, name)`.
     *
     * No-op when [rawValues] is empty or [args] declares nothing. The counts must line up: passing a
     * different number of values than declared keys is an error.
     *
     * @param localeScope `"items"` / `"entities"`, used only for error message keys
     * @throws IllegalArgumentException if the value count mismatches the declared keys, or a value can't
     *   be parsed to its declared type
     */
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

    /**
     * Reads the declared [args] values from [holder]'s persistent data and returns them as strings, in
     * declaration order (arrays comma-joined). Returns an empty list if [args] is null/empty **or if any
     * declared key is missing** on the holder — i.e. it's all-or-nothing, matching how [applyToHolder]
     * writes the full set. Useful for showing current values in lore or echoing them back to commands.
     */
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

    /**
     * Resolves the [NamespacedKey] under which the declared arg [name] is stored — the key you then
     * pass to `EffectiveDataContainerUtils` to read a single value from a stack/entity. Validates that
     * [name] is actually declared in [args].
     *
     * @param localeScope `"items"` / `"entities"`, used only for error message keys
     * @throws IllegalStateException if [args] is null (no additional args declared)
     * @throws IllegalArgumentException if [name] isn't one of the declared keys
     */
    fun additionalKey(args: AdditionalArgs?, name: String, localeScope: String): NamespacedKey {
        val a = args ?: throw IllegalStateException(
            Locale.getMessage("errors.$localeScope.no_additional_args_defined")
        )
        require(a.keys.any { it.first == name }) {
            Locale.getMessage("errors.$localeScope.unknown_additional_arg_key", name)
        }
        return NamespacedKey(a.instance, name)
    }

    /**
     * All declared keys resolved to `NamespacedKey -> PDC type` pairs (in declaration order), or null if
     * [args] is null. Handy for enumerating every stored parameter without knowing the names up front.
     */
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
