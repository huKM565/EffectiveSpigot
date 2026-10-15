package ru.hukm.effectiveSpigot.minecraft.screen

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveGlyph
import ru.hukm.effectiveSpigot.minecraft.resourcepack.EffectiveResourcepack
import java.awt.image.BufferedImage
import java.io.File
import java.util.jar.JarFile
import javax.imageio.ImageIO

/**
 * An animation on a player's screen that the client plays by itself.
 * ⚠️ **Experimental.** See [EffectiveScreenImage] — same caveats.
 *
 * The frames are the PNG files in the [getFramesPath] folder of the plugin jar, played in natural file-name order
 * (`2.png` before `10.png`), in a loop at [getFramesPerSecond] frames per second. Every frame is cut into tile glyphs like an
 * [EffectiveScreenImage] ([EffectiveScreenTiles]) and placed the same way: [getX]/[getY] are the top-left corner and
 * [getSize] the width as screen fractions, the height follows the frames' aspect ratio. All frames must be the same
 * size.
 *
 * [show] sends the tiles of every frame once. Each tile carries the animation marker `(37, 13, 70, 1)` and, next to
 * the picture data in its border, its frame index, the frame count and the frame rate. The shader takes the time from
 * the client's own game clock (`GameTime`, which the client advances every rendered frame including the fraction of
 * the current tick, so it is not limited to 20 steps a second) and collapses the tiles of every frame but the current
 * one into a point, so no packets are sent per frame and ping or its jitter cannot make the animation stutter. Frame
 * rates above 20 (e.g. 60) work, limited only by the client's own FPS.
 *
 * ```kotlin
 * object Intro : EffectiveScreenAnimation() {
 *     override fun getNamespacedData() = ExamplePlugin.instance to "intro"
 *     override fun getFramesPath() = "textures/screen/intro/"
 *     override fun getFramesPerSecond() = 60
 *     override fun getSize() = 1f
 *     fun init() {}
 * }
 *
 * Intro.show(player)
 * Intro.hide(player)
 * ```
 *
 * The component takes no width in the text around it (negative-space characters cancel the tiles' advance), so it can
 * go into a dialog title: drawn by the dialog itself, it appears above the dialog's blurred, darkened background,
 * unlike [show], which goes through the HUD under it.
 *
 * Notes:
 * - The loop phase comes from the world's game time, so the animation starts at whatever frame the clock is on, not
 *   necessarily the first one.
 * - The client's `GameTime` wraps every 24000 ticks (1200 seconds); unless the frame count divides
 *   `1200 × frames per second`, the animation jumps once per wrap.
 * - At most 4095 frames.
 * - Every tile of every frame is a glyph in the pack and in the client's font sheets: long or large animations cost
 *   client memory and loading time.
 * - In a paused singleplayer game the clock, and so the animation, stops.
 */
abstract class EffectiveScreenAnimation : EffectiveScreenElement() {
    companion object {
        private const val SIZE_STEPS = 64
        private const val GLYPH_HEIGHT = 8
        private const val MAX_FRAMES = 4095

        private val _namespacedKeyToScreenAnimation = hashMapOf<String, EffectiveScreenAnimation>()

        /** Read-only registry of all constructed screen animations, keyed by [getNamespacedName]. */
        val namespacedKeyToScreenAnimation: Map<String, EffectiveScreenAnimation> get() = _namespacedKeyToScreenAnimation

        /** Registered screen animation for a namespaced name, or null. */
        fun getScreenAnimationByNamespacedName(namespacedName: String): EffectiveScreenAnimation? {
            return _namespacedKeyToScreenAnimation[namespacedName]
        }

        private val naturalOrder = Comparator<String> { a, b ->
            val chunksA = Regex("\\d+|\\D+").findAll(a).map { it.value }.toList()
            val chunksB = Regex("\\d+|\\D+").findAll(b).map { it.value }.toList()
            for (i in 0 until minOf(chunksA.size, chunksB.size)) {
                val x = chunksA[i]
                val y = chunksB[i]
                val order = if (x[0].isDigit() && y[0].isDigit()) x.toBigInteger().compareTo(y.toBigInteger()) else x.compareTo(y)
                if (order != 0) return@Comparator order
            }
            chunksA.size - chunksB.size
        }
    }

    private val plugin: JavaPlugin get() = getNamespacedData().first

    private val built: Pair<List<EffectiveGlyph>, Int> = buildTiles()

    /** The font glyphs (tiles of all frames) backing this animation; send all their characters together. */
    val glyphs: List<EffectiveGlyph> get() = built.first

    init {
        register()
        glyphs.forEach { EffectiveResourcepack.addGlyph(plugin, it) }
    }

    private fun register() {
        val namespacedName = getNamespacedName()
        if (_namespacedKeyToScreenAnimation.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.screen_animations.already_registered", namespacedName))
        }
        _namespacedKeyToScreenAnimation[namespacedName] = this
    }

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Folder inside the plugin jar with the frames as PNG files, e.g. `"textures/screen/intro/"`. */
    abstract fun getFramesPath(): String

    /** Frames played per second; may be above 20 (the server tick rate), e.g. 60. */
    open fun getFramesPerSecond(): Int = 20

    /** Default left edge of the animation, screen fraction `0..1`. */
    open fun getX(): Float = 0f

    /** Default top edge of the animation, screen fraction `0..1`. */
    open fun getY(): Float = 0f

    /** Default animation width, fraction of the screen width `0..1`, quantized to 1/64; the height keeps the aspect. */
    open fun getSize(): Float = 0.25f

    private fun buildTiles(): Pair<List<EffectiveGlyph>, Int> {
        val frames = readFrames()
        val framesPerSecond = getFramesPerSecond()
        require(framesPerSecond >= 1 && frames.size <= MAX_FRAMES) {
            Locale.getMessage("errors.screen_animations.too_long", getNamespacedName(), frames.size, MAX_FRAMES)
        }
        val width = frames.first().width
        val height = frames.first().height
        require(frames.all { it.width == width && it.height == height }) {
            Locale.getMessage("errors.screen_animations.frame_size_mismatch", getNamespacedName())
        }
        require(width <= EffectiveScreenTiles.MAX_PICTURE_SIDE && height <= EffectiveScreenTiles.MAX_PICTURE_SIDE) {
            Locale.getMessage("errors.screen_images.too_large", getNamespacedName(), width, height)
        }

        val folder = "screen/${getNamespacedName().replace(':', '/')}"
        val tiles = ArrayList<EffectiveGlyph>()
        var advance = 0
        frames.forEachIndexed { frameIndex, frame ->
            val timing = listOf((frameIndex shl 12) or frames.size, framesPerSecond)
            EffectiveScreenTiles.cut(frame).forEachIndexed { tileIndex, tile ->
                advance += EffectiveScreenTiles.advance(tile, timing.size, GLYPH_HEIGHT)
                tiles += EffectiveGlyph(
                    texturePath = "$folder/${frameIndex}_$tileIndex.png",
                    height = GLYPH_HEIGHT,
                    ascent = GLYPH_HEIGHT,
                    textureBytes = EffectiveScreenTiles.texture(frame, tile, ANIMATION_MARKER_ARGB, timing),
                )
            }
        }
        return tiles to advance
    }

    private fun readFrames(): List<BufferedImage> {
        val folder = getFramesPath().trimStart('/').let { if (it.endsWith("/")) it else "$it/" }
        val jar = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        val names = JarFile(jar).use { file ->
            file.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith(folder) && it.endsWith(".png") && it.indexOf('/', folder.length) < 0 }
                .sortedWith(naturalOrder)
                .toList()
        }
        require(names.isNotEmpty()) {
            Locale.getMessage("errors.screen_animations.no_frames", getFramesPath(), plugin.name)
        }
        return names.map { name -> plugin.getResource(name)!!.use { ImageIO.read(it) } }
    }

    /** All frames with the default placement encoded in their color; put it in a title, action bar, boss bar, … */
    fun getComponent(): Component = getComponent(getX(), getY(), getSize())

    /** All frames with an explicit placement encoded in their color (screen fractions, top-left origin). */
    fun getComponent(x: Float, y: Float, size: Float): Component {
        val step = Math.round(size * SIZE_STEPS)
        val text = glyphs.joinToString("") { it.charGlyph.string } + EffectiveScreenTiles.backSpaces(built.second)
        return withShaderData(Component.text(text), packPlacement(x, y, step))
    }

    /** Shows the animation to [player] at the default placement until [hide]. */
    fun show(player: Player) = show(player, FOREVER_TICKS)

    /** Shows the animation to [player] for [durationTicks]. */
    fun show(player: Player, durationTicks: Int) = showBar(player, getComponent(), durationTicks)

    /** Unique identity as `"<plugin-name>:<id>"`, both lowercased. */
    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
