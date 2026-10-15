package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Base class for an image shown on a player's screen.
 * ⚠️ **Experimental.** The core-shader technique, this API and its behaviour may change; not guaranteed
 * across clients or with shader packs.
 * Registers itself on construction under
 * [getNamespacedName] (`<plugin>:<name>`) and refuses duplicates; its PNG is added to the plugin's
 * generated pack as font glyphs ([glyphs]).
 *
 * The PNG may be any size up to 4090×4090 and is never downscaled; the framework cuts it into tile glyphs
 * ([EffectiveScreenTiles]: a grid over the non-transparent part, every tile trimmed to its content). [getX]/[getY]
 * place the picture's top-left corner as screen fractions (`0.0..1.0`, steps of 1/512) and [getSize] is the picture's
 * width as a fraction of the screen width (steps of 1/64); the height follows the PNG's aspect ratio, so the picture is
 * never stretched. The placement travels in the text color shared by all tiles, so it can change per [getComponent]
 * call. The component takes no width in the text around it: negative-space characters after the tiles cancel their
 * advance, so it can go into a dialog title or next to other text without moving it. The tiles carry the image marker `(37, 13, 71, 1)` and their position in the picture, so the shader puts every
 * corner at «picture origin + picture pixels × one shared scale» and the tiles meet exactly at any window size.
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
 * - Every tile is a separate 256×256 font sheet in the client's memory: a full-screen 1920×1080 picture is up to 40
 *   tiles. Keep the number of large pictures low.
 * - [show] puts the image into the shared screen boss bar ([EffectiveScreenElement]): any number of images and a text
 *   stay on screen together, each until [hide] or its duration. [getComponent] lands at the same spot whatever
 *   carries it (boss bar, title, action bar).
 * - Shader packs (Iris, OptiFine) replace core shaders: for those players the image lands at the raw glyph
 *   position instead. [EffectiveScreenEffects] fade and shake are unaffected.
 */
abstract class EffectiveScreenImage : EffectiveScreenElement() {
    companion object {
        private const val SIZE_STEPS = 64
        private const val GLYPH_HEIGHT = 8

        private val _namespacedKeyToScreenImage = hashMapOf<String, EffectiveScreenImage>()

        /** Read-only registry of all constructed screen images, keyed by [getNamespacedName]. */
        val namespacedKeyToScreenImage: Map<String, EffectiveScreenImage> get() = _namespacedKeyToScreenImage

        /** Registered screen image for a namespaced name, or null. */
        fun getScreenImageByNamespacedName(namespacedName: String): EffectiveScreenImage? {
            return _namespacedKeyToScreenImage[namespacedName]
        }
    }

    private val plugin: JavaPlugin get() = getNamespacedData().first

    private val built: Pair<List<EffectiveGlyph>, Int> = buildTiles()

    /** The font glyphs (tiles) backing this image, in drawing order; send all their characters together. */
    val glyphs: List<EffectiveGlyph> get() = built.first

    init {
        register()
        glyphs.forEach { EffectiveResourcepack.addGlyph(plugin, it) }
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

    /** Default left edge of the picture, screen fraction `0..1`. */
    open fun getX(): Float = 0f

    /** Default top edge of the picture, screen fraction `0..1`. */
    open fun getY(): Float = 0f

    /** Default picture width, fraction of the screen width `0..1`, quantized to 1/64; the height keeps the aspect. */
    open fun getSize(): Float = 0.25f

    private fun buildTiles(): Pair<List<EffectiveGlyph>, Int> {
        val source = readTexture()
        require(source.width <= EffectiveScreenTiles.MAX_PICTURE_SIDE && source.height <= EffectiveScreenTiles.MAX_PICTURE_SIDE) {
            Locale.getMessage("errors.screen_images.too_large", getNamespacedName(), source.width, source.height)
        }
        val folder = "screen/${getNamespacedName().replace(':', '/')}"
        val tiles = EffectiveScreenTiles.cut(source)
        val glyphs = tiles.mapIndexed { index, tile ->
            EffectiveGlyph(
                texturePath = "$folder/$index.png",
                height = GLYPH_HEIGHT,
                ascent = GLYPH_HEIGHT,
                textureBytes = EffectiveScreenTiles.texture(source, tile, IMAGE_MARKER_ARGB),
            )
        }
        return glyphs to tiles.sumOf { EffectiveScreenTiles.advance(it, 0, GLYPH_HEIGHT) }
    }

    private fun readTexture(): BufferedImage {
        val stream = plugin.getResource(getTexturePath())
            ?: throw IllegalArgumentException(Locale.getMessage("errors.resourcepack.texture_not_found", getTexturePath(), plugin.name))
        return stream.use { ImageIO.read(it) }
    }

    /** All tiles with the default placement encoded in their color; put it in a title, action bar, boss bar, … */
    fun getComponent(): Component = getComponent(getX(), getY(), getSize())

    /** All tiles with an explicit placement encoded in their color (screen fractions, top-left origin). */
    fun getComponent(x: Float, y: Float, size: Float): Component {
        val step = Math.round(size * SIZE_STEPS)
        val text = glyphs.joinToString("") { it.charGlyph.string } + EffectiveScreenTiles.backSpaces(built.second)
        return withShaderData(Component.text(text), packPlacement(x, y, step))
    }

    /** Shows the image to [player] at the default placement until [hide]. */
    fun show(player: Player) = show(player, FOREVER_TICKS)

    /** Shows the image to [player] for [durationTicks]. */
    fun show(player: Player, durationTicks: Int) = showBar(player, getComponent(), durationTicks)

    /** Unique identity as `"<plugin-name>:<id>"`, both lowercased. */
    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
