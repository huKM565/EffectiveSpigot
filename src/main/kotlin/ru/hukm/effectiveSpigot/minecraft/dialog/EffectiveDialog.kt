package ru.hukm.effectiveSpigot.minecraft.dialog

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils

/**
 * Base class for native dialog windows (the Paper dialog API, Minecraft 1.21.6+).
 *
 * The subclass builds the dialog itself with the Paper API in [getDialog] (any type, any body and buttons); the
 * framework only shows it and controls the background behind it with [hasBackground]. Ready-made:
 * [EffectiveConfirmDialog].
 *
 * ```kotlin
 * object ExampleDialog : EffectiveDialog() {
 *     override fun getDialog(player: Player) = Dialog.create { builder ->
 *         builder.empty()
 *             .base(DialogBase.builder(Component.text("Example")).build())
 *             .type(DialogType.notice())
 *     }
 *
 *     override fun hasBackground() = false
 * }
 *
 * ExampleDialog.show(player)
 * ```
 *
 * [hasBackground] true is the vanilla blur and darkening, false leaves whatever is under the dialog (the world, HUD,
 * screen images and animations) sharp and unshaded. See [EffectiveDialogBackground] for how the framework pack does
 * it and its limits: it needs the framework pack, and players with menu background blur set to 0 get no blur and no
 * darkening in any in-game menu.
 *
 * The pack also makes the client's «custom screen» warning button next to every dialog title transparent
 * (`gui/sprites/dialog/warning_button*`; the sprite is the whole button). The client always adds that button, so it
 * is only hidden: the 20×20 spot right of the title still shows its tooltip on hover and opens the client's
 * note about custom screens on click.
 *
 * On servers older than 1.21.6 [isSupported] is false and [show] throws [IllegalStateException]. The framework
 * compiles against 1.21.4, so the Paper dialog classes are reached through reflection proxies ([DialogProxies]).
 */
abstract class EffectiveDialog {
    companion object {
        /** Whether this server can show dialogs (Minecraft 1.21.6+). */
        @JvmStatic
        val isSupported: Boolean get() = EffectiveMinecraftUtils.isVersionAtLeast(1, 21, 6)

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                if (isSupported) EffectiveDialogBackground.init()
            }
        }
    }

    /**
     * The dialog to show to [player], built with the Paper API: an `io.papermc.paper.dialog.Dialog` (any Adventure
     * `DialogLike`). Typed [Any] because the framework compiles against 1.21.4, which has no dialog API.
     */
    abstract fun getDialog(player: Player): Any

    /** True: the vanilla blur and darkening behind the dialog; false: none (see [EffectiveDialogBackground]). */
    open fun hasBackground(): Boolean = true

    /**
     * Shows [getDialog] to [player]. It opens on the next tick and closes the player's open inventory first, so it is
     * safe to call from a menu click handler.
     *
     * @throws IllegalStateException on servers older than 1.21.6 (check [isSupported] first).
     */
    fun show(player: Player) {
        check(isSupported) { Locale.getMessage("errors.dialog.unsupported_version", Bukkit.getMinecraftVersion()) }
        val dialog = getDialog(player)
        val background = hasBackground()

        EffectiveSpigot.instance.launch {
            delay(1.ticks)
            if (!player.isOnline) return@launch
            player.closeInventory()
            EffectiveDialogBackground.set(player, background)
            DialogProxies.audience.showDialog(player, dialog)
        }
    }
}
