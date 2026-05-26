package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZone
import ru.hukm.effectiveSpigot.minecraft.zone.EffectiveZoneSelection

object EffectiveZoneCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "ezone")
    override fun getPermission() = "effectivespigot.command.ezone"
    override fun getDescription() = "Manage zones"

    override fun commandTree() = CommandNode.build {
        executes { _ ->
            sendMessage(Locale.getMessage("commands.ezone.usage"))
        }

        choice("list") {
            executes { _ ->
                EffectiveZone.namespacedKeyToZone.values.forEach { zone ->
                    sendMessage(zone.getNamespacedName())
                    for (zoneBox in zone.zoneBoxes) {
                        sendMessage(" - id: ${zoneBox.id}")
                        sendMessage("  ${zoneBox.firstPos.x} ${zoneBox.firstPos.y} ${zoneBox.firstPos.z} - ${zoneBox.secondPos.x} ${zoneBox.secondPos.y} ${zoneBox.secondPos.z}")
                    }
                }
            }
        }

        choice("create") {
            executes { args ->
                if (this !is Player) {
                    sendMessage(Locale.getMessage("commands.ezone.not_player"))
                    return@executes
                }
                val player = this
                if (args.size < 2) {
                    sendMessage(Locale.getMessage("commands.ezone.create_usage"))
                    sendMessage(Locale.getMessage("commands.ezone.available_zones"))
                    EffectiveZone.namespacedKeyToZone.keys.forEach { sendMessage(" - $it") }
                    return@executes
                }
                val selection = EffectiveZoneSelection.getSelection(player.uniqueId)
                if (selection == null || selection.first == null || selection.second == null) {
                    sendMessage(Locale.getMessage("commands.ezone.no_selection"))
                    return@executes
                }
                val zoneType = args[1]
                EffectiveZoneSelection.playerToSelectedCoords.remove(player.uniqueId)
                EffectiveZone.registerSelection(Triple(selection.first!!, selection.second!!, selection.third), zoneType)
                sendMessage(Locale.getMessage("commands.ezone.create_success", zoneType))
            }
            dynamic { EffectiveZone.namespacedKeyToZone.keys.toList() }
        }

        choice("delete") {
            executes { args ->
                if (args.size < 2) {
                    sendMessage(Locale.getMessage("commands.ezone.delete_usage"))
                    return@executes
                }
                val id = args[1].toIntOrNull()
                if (id == null) {
                    sendMessage(Locale.getMessage("commands.ezone.invalid_id"))
                    return@executes
                }
                if (EffectiveZone.deleteZoneBoxById(id)) {
                    sendMessage(Locale.getMessage("commands.ezone.delete_success", id))
                } else {
                    sendMessage(Locale.getMessage("commands.ezone.not_found"))
                }
            }
            dynamic {
                EffectiveZone.namespacedKeyToZone.values.flatMap { zone ->
                    zone.zoneBoxes.map { it.id.toString() }
                }
            }
        }
    }

    fun init() {}
}
