package ru.hukm.effectiveSpigot.minecraft.completers

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZone

class EffectiveZoneCompleter : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        val lastArg = args.last().lowercase()
        val firstArg = args.getOrNull(0)?.lowercase()

        val suggestions = when (args.size) {
            1 -> listOf("list", "create", "delete")

            2 -> when (firstArg) {
                "create" -> EffectiveZone.namespacedKeyToZone.keys.toList()
                "delete" -> {
                    val ids = mutableListOf<String>()
                    EffectiveZone.namespacedKeyToZone.values.forEach { zone ->
                        zone.zoneBoxes.forEach { box -> ids.add(box.id.toString()) }
                    }
                    ids
                }
                else -> emptyList()
            }

            else -> emptyList()
        }

        return suggestions
            .filter { it.startsWith(lastArg, ignoreCase = true) }
            .toMutableList()
    }
}