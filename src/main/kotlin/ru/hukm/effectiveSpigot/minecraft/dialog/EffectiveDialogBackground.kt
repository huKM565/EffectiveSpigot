package ru.hukm.effectiveSpigot.minecraft.dialog

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import ru.hukm.effectiveSpigot.minecraft.screen.EffectiveScreenElement
import ru.hukm.effectiveSpigot.minecraft.screen.EffectiveScreenTiles
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

/**
 * The per-dialog menu background behind [EffectiveDialog.hasBackground].
 *
 * A dialog is drawn after the client has blurred and darkened what is under it, so it cannot switch that off itself;
 * the switch has to be on the screen before the dialog. The framework pack:
 * - makes the vanilla darkening texture (`textures/gui/inworld_menu_background.png`) transparent;
 * - replaces the menu blur chain (`post_effect/blur.json`) with the vanilla box blur passes run through
 *   `effectivespigot:post/menu_blur`, whose last pass also darkens like the vanilla texture (black at alpha 64/255).
 *   If the top-left screen pixel has the flag color `RGB 37, 13, 72`, every pass copies the image unchanged instead.
 *
 * The flag is this element: a glyph in the screen-element boss bar (drawn with the HUD, before the menu blur) whose
 * corners carry the marker `RGBA 37, 13, 72, 1`; `text.vsh` moves it onto the top-left GUI pixel and fills it with
 * the opaque centre texel. [EffectiveDialog.show] shows it for a dialog without background and hides it for one with.
 *
 * Paper sends no event when a dialog closes, and the framework does not see the buttons of a dialog a plugin built
 * itself, so the flag goes away when an [EffectiveConfirmDialog] button is pressed, when a dialog with background is
 * shown, or when the player walks, jumps or turns the camera, which is impossible with a screen open (ignored for
 * [GRACE_TICKS] after showing, for packets sent before the dialog arrived). Until then other menus opened by the
 * player are not blurred either.
 *
 * Limits:
 * - the blur chain runs only with the menu background blur option at 1 or more, so at 0 no in-game menu is blurred
 *   or darkened;
 * - an opaque screen image or animation over the top-left corner can hide the flag.
 */
internal object EffectiveDialogBackground : EffectiveScreenElement() {
    private const val MARKER_ARGB = 0x01250D48
    private const val FLAG_ARGB = 0xFF250D48.toInt()
    private const val TEXTURE_SIZE = 3
    private const val GLYPH_HEIGHT = 8
    private const val GRACE_TICKS = 20

    private val glyph = EffectiveGlyph(
        texturePath = "effectivespigot/dialog_background_flag.png",
        height = GLYPH_HEIGHT,
        ascent = GLYPH_HEIGHT,
        textureBytes = texture(),
    )

    private val component: Component by lazy {
        val advance = (0.5 + TEXTURE_SIZE * GLYPH_HEIGHT / TEXTURE_SIZE.toDouble()).toInt() + 1
        withShaderData(Component.text(glyph.charGlyph.string + EffectiveScreenTiles.backSpaces(advance)), 0)
    }

    private val shownAt = HashMap<UUID, Int>()

    fun init() {
        EffectiveResourcepack.addGlyph(EffectiveSpigot.instance, glyph)
        event<PlayerInputEvent> {
            val input = it.input
            if (input.isForward || input.isBackward || input.isLeft || input.isRight || input.isJump) playing(it.player)
        }
        event<PlayerMoveEvent> {
            if (it.from.yaw != it.to.yaw || it.from.pitch != it.to.pitch) playing(it.player)
        }
        event<PlayerQuitEvent> { shownAt.remove(it.player.uniqueId) }
    }

    /** Shows the flag to [player] for a dialog without [background], hides it for one with. */
    fun set(player: Player, background: Boolean) {
        if (background) {
            clear(player)
        } else {
            shownAt[player.uniqueId] = Bukkit.getCurrentTick()
            showBar(player, component, FOREVER_TICKS)
        }
    }

    fun clear(player: Player) {
        if (shownAt.remove(player.uniqueId) != null) hide(player)
    }

    private fun playing(player: Player) {
        val since = shownAt[player.uniqueId] ?: return
        if (Bukkit.getCurrentTick() - since >= GRACE_TICKS) clear(player)
    }

    private fun texture(): ByteArray {
        val image = BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(TEXTURE_SIZE / 2, TEXTURE_SIZE / 2, FLAG_ARGB)
        stampCorners(image, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, MARKER_ARGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }
}
