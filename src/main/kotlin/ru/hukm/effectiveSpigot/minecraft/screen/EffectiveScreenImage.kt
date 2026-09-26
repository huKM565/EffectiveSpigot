package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Base class for an image shown on a player's screen.
 * ⚠️ **Experimental.** The core-shader technique, this API and its behaviour may change; not guaranteed
 * across clients or with shader packs.
 * Registers itself on construction under
 * [getNamespacedName] (`<plugin>:<name>`) and refuses duplicates; its PNG is added to the plugin's
 * generated pack as a font glyph ([glyph]).
 *
 * The PNG may be any shape — it is padded to a square automatically. Coordinates are screen fractions
 * from the top-left corner (`0.0..1.0`); [getSize] is the width as a fraction of the screen width.
 *
 * ```kotlin
 * object Logo : EffectiveScreenImage() {
 *     override fun getNamespacedData() = ExamplePlugin.instance to "logo"
 *     override fun getTexturePath() = "textures/screen/logo.png"
 *     override fun getX() = 0.05f
 *     override fun getY() = 0.05f
 *     override fun getSize() = 0.25f
 *     fun init() {}
 * }
 *
 * Logo.show(player)                        // until hide()
 * Logo.show(player, durationTicks = 100)
 * Logo.hide(player)
 * ```
 *
 * Notes:
 * - Rendering goes through the framework's `text.vsh` core shader in the plugin's pack, so the plugin
 *   must enable its pack (`EffectiveResourcepack.addServerResourcepack` in `onEnable`, after `init()`).
 * - [show] uses the title slot: one element per player at a time — an image, a text or a fade replaces
 *   the previous one. To show several at once, join their [getComponent]s into a single title.
 * - Shader packs (Iris, OptiFine) replace core shaders: for those players the image lands at the raw glyph
 *   position instead. [EffectiveScreenEffects] fade and shake are unaffected.
 */
abstract class EffectiveScreenImage : EffectiveScreenElement() {
    companion object {
        private const val SIZE_STEPS = 32
        private const val GLYPH_HEIGHT = 8
        private const val ALMOST_TRANSPARENT_BLACK = 0x01000000

        private val _namespacedKeyToScreenImage = hashMapOf<String, EffectiveScreenImage>()

        /** Read-only registry of all constructed screen images, keyed by [getNamespacedName]. */
        val namespacedKeyToScreenImage: Map<String, EffectiveScreenImage> get() = _namespacedKeyToScreenImage

        /** Registered screen image for a namespaced name, or null. */
        fun getScreenImageByNamespacedName(namespacedName: String): EffectiveScreenImage? {
            return _namespacedKeyToScreenImage[namespacedName]
        }
    }

    override val marker = 0b110_00000

    private val plugin: JavaPlugin get() = getNamespacedData().first

    /** The font glyph backing this image; its [EffectiveGlyph.charGlyph] is the character to send. */
    val glyph: EffectiveGlyph = EffectiveGlyph(
        texturePath = "screen/${getNamespacedName().replace(':', '/')}.png",
        height = GLYPH_HEIGHT,
        ascent = GLYPH_HEIGHT,
        textureBytes = buildSquareTexture()
    )

    init {
        register()
        EffectiveResourcepack.addGlyph(plugin, glyph)
    }

    private fun register() {
        val namespacedName = getNamespacedName()
        if (_namespacedKeyToScreenImage.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.screen_images.already_registered", namespacedName))
        }
        _namespacedKeyToScreenImage[namespacedName] = this
    }

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Resource path of the PNG inside the plugin jar. */
    abstract fun getTexturePath(): String

    /** Default left edge, screen fraction `0..1`. */
    open fun getX(): Float = 0f

    /** Default top edge, screen fraction `0..1`. */
    open fun getY(): Float = 0f

    /** Default box side (the image's larger dimension), screen fraction `0..1`, quantized to 1/32. */
    open fun getSize(): Float = 0.25f

    private fun buildSquareTexture(): ByteArray {
        val source = readTexture()
        val square = padToSquare(source)
        markBottomRightCorner(square)
        return toPngBytes(square)
    }

    private fun readTexture(): BufferedImage {
        val stream = plugin.getResource(getTexturePath())
            ?: throw IllegalArgumentException(Locale.getMessage("errors.resourcepack.texture_not_found", getTexturePath(), plugin.name))
        return stream.use { ImageIO.read(it) }
    }

    private fun padToSquare(source: BufferedImage): BufferedImage {
        val side = maxOf(source.width, source.height)
        val square = BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB)
        val graphics = square.createGraphics()
        graphics.drawImage(source, 0, 0, null)
        graphics.dispose()
        return square
    }

    private fun markBottomRightCorner(square: BufferedImage) {
        val x = square.width - 1
        val y = square.height - 1
        val alpha = square.getRGB(x, y) ushr 24
        if (alpha == 0) square.setRGB(x, y, ALMOST_TRANSPARENT_BLACK)
    }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    /** The glyph with the default placement encoded in its color; put it in a title, action bar, boss bar, … */
    fun getComponent(): Component = getComponent(getX(), getY(), getSize())

    /** The glyph with an explicit placement encoded in its color (screen fractions, top-left origin). */
    fun getComponent(x: Float, y: Float, size: Float): Component {
        var step = Math.round(size * SIZE_STEPS)
        if (step < 1) step = 1
        if (step > SIZE_STEPS) step = SIZE_STEPS
        return encode(Component.text(glyph.charGlyph.string), x, y, step - 1)
    }

    /** Shows the image to [player] at the default placement until [hide]. */
    fun show(player: Player) = show(player, FOREVER_TICKS)

    /** Shows the image to [player] for [durationTicks]. */
    fun show(player: Player, durationTicks: Int) = showTitle(player, getComponent(), durationTicks)

    /** Unique identity as `"<plugin-name>:<id>"`, both lowercased. */
    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
