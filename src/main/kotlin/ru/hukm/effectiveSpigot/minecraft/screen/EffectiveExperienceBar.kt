package ru.hukm.effectiveSpigot.minecraft.screen

import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.minecraft.nms.NmsPackets

/**
 * Draws an arbitrary fill into the player's experience bar without changing the experience they own. Useful as a
 * charge or cast indicator: the bar above the hotbar fills up, and one call to [reset] puts the real experience back.
 *
 * The bar the client draws while a horse charges its jump cannot be used for this: the client only draws it when
 * the player rides a jumpable mount and computes the fill itself from how long jump is held. The experience bar
 * sits in the same place and is the closest thing a server can drive.
 *
 * ```kotlin
 * EffectiveExperienceBar.show(player, charge / 60f)
 * EffectiveExperienceBar.reset(player)
 * ```
 *
 * Nothing is stored server-side, so [reset] is enough to clean up, and a player who reconnects sees their real
 * experience anyway.
 */
object EffectiveExperienceBar {
    /**
     * Shows [progress] (`0.0..1.0`) in the bar for [player]. The number above it is [level], and the default `0`
     * hides the number entirely.
     */
    @JvmStatic
    @JvmOverloads
    fun show(player: Player, progress: Float, level: Int = 0) {
        NmsPackets.sendExperience(player, progress, player.totalExperience, level)
    }

    /** Puts the player's real experience back into the bar. */
    @JvmStatic
    fun reset(player: Player) {
        NmsPackets.sendExperience(player, player.exp, player.totalExperience, player.level)
    }
}
