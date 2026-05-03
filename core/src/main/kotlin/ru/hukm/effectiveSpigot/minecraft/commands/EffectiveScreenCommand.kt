package ru.hukm.effectiveSpigot.minecraft.commands

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveScreenEffects

object EffectiveScreenCommand : EffectiveCommand() {

    override fun getNamespacedData(): Pair<JavaPlugin, String> = Pair(EffectiveSpigot.instance, "escreen")
    override fun getPermission() = "effectivespigot.command.escreen"
    override fun getDescription() = "Play screen effects"

    override fun commandTree() = CommandNode.build {
        executes { _ ->
            sendMessage(LanguageModule.getMessage("commands.escreen.usage"))
        }
        dynamic({ listOf("@a", "@p") + Bukkit.getOnlinePlayers().map { it.name } }) {
            executes { args ->
                if (args.size < 2) {
                    sendMessage(LanguageModule.getMessage("commands.escreen.usage"))
                } else {
                    sendMessage(LanguageModule.getMessage("commands.escreen.unknown_effect"))
                }
            }
            choice("fade") {
                executes { args ->
                    val targets = resolveTargets(this, args[0])
                    if (targets.isEmpty()) {
                        sendMessage(LanguageModule.getMessage("commands.escreen.player_not_found"))
                        return@executes
                    }
                    val fadeIn = args.getOrNull(2)?.toIntOrNull() ?: 10
                    val stay = args.getOrNull(3)?.toIntOrNull() ?: 20
                    val fadeOut = args.getOrNull(4)?.toIntOrNull() ?: 10
                    targets.forEach { EffectiveScreenEffects.runCameraFade(it, fadeIn, stay, fadeOut) }
                    sendMessage(LanguageModule.getMessage("commands.escreen.success_fade"))
                }
                dynamic({ listOf(LanguageModule.getMessage("commands.escreen.completions.fade_in")) }) {
                    dynamic({ listOf(LanguageModule.getMessage("commands.escreen.completions.stay")) }) {
                        dynamic { listOf(LanguageModule.getMessage("commands.escreen.completions.fade_out")) }
                    }
                }
            }
            choice("shake") {
                executes { args ->
                    val targets = resolveTargets(this, args[0])
                    if (targets.isEmpty()) {
                        sendMessage(LanguageModule.getMessage("commands.escreen.player_not_found"))
                        return@executes
                    }
                    val intensity = args.getOrNull(2)?.toFloatOrNull() ?: 1.0f
                    val duration = args.getOrNull(3)?.toIntOrNull() ?: 20
                    val shakeType = try {
                        EffectiveScreenEffects.ShakeType.valueOf(args.getOrNull(4)?.uppercase() ?: "LINEAR")
                    } catch (e: IllegalArgumentException) {
                        EffectiveScreenEffects.ShakeType.LINEAR
                    }
                    targets.forEach { EffectiveScreenEffects.runCameraShake(it, intensity, duration, shakeType) }
                    sendMessage(LanguageModule.getMessage("commands.escreen.success_shake"))
                }
                dynamic({ listOf(LanguageModule.getMessage("commands.escreen.completions.intensity")) }) {
                    dynamic({ listOf(LanguageModule.getMessage("commands.escreen.completions.duration")) }) {
                        dynamic { EffectiveScreenEffects.ShakeType.entries.map { it.name } }
                    }
                }
            }
        }
    }

    private fun resolveTargets(sender: CommandSender, selector: String): List<Player> {
        val targets = ArrayList<Player>()
        try { Bukkit.selectEntities(sender, selector).filterIsInstanceTo(targets) }
        catch (e: IllegalArgumentException) { Bukkit.getPlayer(selector)?.let { targets.add(it) } }
        return targets
    }

    fun init() {}
}