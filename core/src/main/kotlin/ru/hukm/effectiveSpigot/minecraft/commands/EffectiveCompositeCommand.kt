package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveCompositeEntity

object EffectiveCompositeCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "ecomposite")
    override fun getPermission() = "effectivespigot.command.ecomposite"
    override fun getDescription() = "Spawn custom composite entities"

    override fun commandTree() = CommandNode.build {
        executes { args ->
            if (this !is Player) {
                sendMessage(Locale.getMessage("commands.ecomposite.only_players"))
                return@executes
            }
            if (args.isEmpty()) {
                sendMessage(Locale.getMessage("commands.ecomposite.usage"))
                return@executes
            }
            val key = args[0]
            val composite = EffectiveCompositeEntity.namespacedKeyToEffectiveCompositeEntity[key]
            if (composite == null) {
                sendMessage(Locale.getMessage("commands.ecomposite.entity_not_found", key))
                return@executes
            }

            val additionalArgs = args.drop(1)
            if (additionalArgs.isEmpty()) {
                composite.spawnEntities(location)
            } else {
                composite.spawnEntities(location, additionalArgs)
            }
            sendMessage(Locale.getMessage("commands.ecomposite.success", key))
        }
        dynamic { EffectiveCompositeEntity.namespacedKeyToEffectiveCompositeEntity.keys.toList() }
    }

    fun init() {}
}
