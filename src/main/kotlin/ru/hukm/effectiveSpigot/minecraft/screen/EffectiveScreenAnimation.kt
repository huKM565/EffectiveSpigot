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
 * The frames are the PNG files in the [getFramesPath] folder of the plugin jar, in natural file-name order (`2.png`
 * before `10.png`), at [getFramesPerSecond] frames per second. Every frame is cut into tile glyphs like an
 * [EffectiveScreenImage] ([EffectiveScreenTiles]). All frames must be the same size.
 *
 * Unlike an image, the placement is fixed per animation and baked into the tiles: [getX]/[getY] are the top-left
 * corner and [getSize] the width as screen fractions, the height follows the frames' aspect ratio. The text color
 * carries the timing instead: the tick the animation starts at and its [Playback]. [getComponent] starts it from the
 * first frame about when the player receives it (the world's game time plus [START_DELAY_TICKS]).
 *
 * The shader takes the time from the client's own game clock (`GameTime`, which the client advances every rendered
 * frame including the fraction of the current tick, so it is not limited to 20 steps a second) and collapses the tiles
 * of every frame but the current one into a point, so no packets are sent per frame and ping or its jitter cannot make
 * the animation stutter. Frame rates above 20 (e.g. 60) work, limited only by the client's own FPS.
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
 * Intro.show(player, Playback.ONCE)
 * Intro.hide(player)
 * ```
 *
 * The component takes no width in the text around it (negative-space characters cancel the tiles' advance), so it can
 * go into a dialog title: drawn by the dialog itself, it appears above the dialog's blurred, darkened background,
 * unlike [show], which goes through the HUD under it.
 *
 * Notes:
 * - The client's `GameTime` wraps every 24000 ticks (20 minutes), and the start is a tick within that cycle: a
 *   [Playback.ONCE] animation plays again 20 minutes after its start, and a [Playback.LOOP] one jumps once per cycle
 *   unless its frame count divides `1200 × frames per second`.
 * - At most 4095 frames.
 * - Every tile of every frame is a glyph in the pack and in the client's font sheets: long or large animations cost
 *   client memory and loading time.
 * - In a paused singleplayer game the clock, and so the animation, stops.
 */
abstract class EffectiveScreenAnimation : EffectiveScreenElement() {
    /** How the animation plays from the tick [getComponent] starts it at. */
    enum class Playback {
        /** Over and over. */
        LOOP,

        /** Once, then it stays on the last frame. */
        ONCE,

        /** Not at all: it stays on the first frame. */
        PAUSED,

        /** Not at all: it stays on the last frame, as a [ONCE] animation looks after playing. */
        ENDED,

        /** Once backwards, from the last frame to the first, then it stays on the first frame. */
        REVERSE,
    }

    companion object {
        /** Ticks between sending the component and its first frame, so the start is not missed on arrival. */
        const val START_DELAY_TICKS = 2

        private const val SIZE_STEPS = 64
        private const val GLYPH_HEIGHT = 8
        private const val MAX_FRAMES = 4095
        private const val CLOCK_TICKS = 24000

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

    /** Number of frames read from [getFramesPath]; with [getFramesPerSecond] it gives the length of one playback. */
    var frameCount: Int = 0
        private set

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

    /** Left edge of the animation, screen fraction `0..1`; baked into the tiles. */
    open fun getX(): Float = 0f

    /** Top edge of the animation, screen fraction `0..1`; baked into the tiles. */
    open fun getY(): Float = 0f

    /** Animation width, fraction of the screen width `0..1`, quantized to 1/64; the height keeps the aspect. */
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

        frameCount = frames.size
        val folder = "screen/${getNamespacedName().replace(':', '/')}"
        val placement = packPlacement(getX(), getY(), Math.round(getSize() * SIZE_STEPS))
        val tiles = ArrayList<EffectiveGlyph>()
        var advance = 0
        frames.forEachIndexed { frameIndex, frame ->
            val timing = listOf((frameIndex shl 12) or frames.size, framesPerSecond, placement)
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

    /**
     * All frames, started about when [player] receives them (see [START_DELAY_TICKS]) and played as [playback]; put it
     * in a dialog title, a boss bar, …
     */
    @JvmOverloads
    fun getComponent(player: Player, playback: Playback = Playback.LOOP): Component {
        val start = ((player.world.gameTime + START_DELAY_TICKS) % CLOCK_TICKS).toInt()
        val text = glyphs.joinToString("") { it.charGlyph.string } + EffectiveScreenTiles.backSpaces(built.second)
        return withShaderData(Component.text(text), (playback.ordinal shl 15) or start)
    }

    /** Shows the animation to [player] on the HUD, played as [playback], until [hide]. */
    @JvmOverloads
    fun show(player: Player, playback: Playback = Playback.LOOP) = show(player, FOREVER_TICKS, playback)

    /** Shows the animation to [player] on the HUD, played as [playback], for [durationTicks]. */
    @JvmOverloads
    fun show(player: Player, durationTicks: Int, playback: Playback = Playback.LOOP) =
        showBar(player, getComponent(player, playback), durationTicks)

    /** Unique identity as `"<plugin-name>:<id>"`, both lowercased. */
    fun getNamespacedName(): String =
        getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
}
