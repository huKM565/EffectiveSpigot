package ru.hukm.effectiveSpigot.minecraft.screen

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Builds the [EffectiveScreenText] font into the framework pack from the vanilla bitmap fonts bundled under
 * `vanilla/font/` (the `include/default` providers of the client and their textures).
 *
 * Every glyph gets the screen-text marker in its four corner pixels without changing how it looks or how wide it is:
 * - glyphs are regrouped into one bitmap provider per actual width, so each cell is exactly as wide as its glyph and
 *   the right-hand marker column is already part of the glyph's width (the client measures the width as the rightmost
 *   column with any alpha);
 * - every cell gets a transparent row above and below for the markers, and the provider's height and ascent grow by
 *   one row each, so the glyph keeps its scale and baseline;
 * - the top corners carry the distance from the glyph's top edge up to the text line and the bottom corners the
 *   distance down from the line to the bottom edge ([EffectiveScreenElement.textMarkerArgb]), so the shader finds the
 *   line from any single vertex. The client puts a glyph's top at `7 - ascent` from the line.
 *
 * Glyphs with no visible pixels are skipped; the space comes from a `space` provider as in vanilla.
 */
internal object EffectiveScreenTextFont {
    const val NAMESPACE = "effectivespigot"
    const val NAME = "screen_text"

    private const val SOURCE_DIRECTORY = "vanilla/font/"
    private const val DEFINITION_FILE = "default.json"
    private const val DEFAULT_HEIGHT = 8
    private const val SPACE_ADVANCE = 4
    private const val CELLS_PER_ROW = 16
    private const val MARKER_ROWS = 2
    private const val GLYPH_TOP_BASE = 7

    private class Glyph(val codepoint: Int, val sourceX: Int, val sourceY: Int)

    /** Writes the font definition and its marked textures into [files]. */
    fun addTo(files: MutableMap<String, ByteArray>) {
        val definition = JsonParser.parseString(readSource(DEFINITION_FILE).toString(Charsets.UTF_8)).asJsonObject
        val providers = JsonArray()
        providers.add(spaceProvider())
        val taken = HashSet<Int>()
        for (element in definition.getAsJsonArray("providers")) {
            val provider = element.asJsonObject
            if (provider["type"].asString == "bitmap") addBitmapProvider(files, providers, provider, taken)
        }
        val font = JsonObject().apply { add("providers", providers) }
        files["assets/$NAMESPACE/font/$NAME.json"] = font.toString().toByteArray(Charsets.UTF_8)
    }

    private fun spaceProvider(): JsonObject = JsonObject().apply {
        addProperty("type", "space")
        add("advances", JsonObject().apply {
            addProperty(" ", SPACE_ADVANCE)
            addProperty("‌", 0)
        })
    }

    private fun addBitmapProvider(
        files: MutableMap<String, ByteArray>,
        providers: JsonArray,
        provider: JsonObject,
        taken: MutableSet<Int>,
    ) {
        val fileName = provider["file"].asString.substringAfterLast('/')
        val source = ImageIO.read(readSource(fileName).inputStream())
        val grid = provider.getAsJsonArray("chars").map { it.asString.codePoints().toArray() }
        val cellWidth = source.width / grid[0].size
        val cellHeight = source.height / grid.size
        val height = provider["height"]?.asInt ?: DEFAULT_HEIGHT
        val ascent = provider["ascent"].asInt

        val glyphsByWidth = sortedMapOf<Int, MutableList<Glyph>>()
        grid.forEachIndexed { row, codepoints ->
            codepoints.forEachIndexed { column, codepoint ->
                if (codepoint != 0 && taken.add(codepoint)) {
                    val glyph = Glyph(codepoint, column * cellWidth, row * cellHeight)
                    val width = visibleWidth(source, glyph, cellWidth, cellHeight)
                    if (width > 0) glyphsByWidth.getOrPut(width) { mutableListOf() }.add(glyph)
                }
            }
        }

        val markedHeight = cellHeight + MARKER_ROWS
        val markedGlyphHeight = height * markedHeight / cellHeight
        val markedAscent = ascent + height / cellHeight
        val topToLine = markedAscent - GLYPH_TOP_BASE
        val lineToBottom = markedGlyphHeight - topToLine
        for ((width, glyphs) in glyphsByWidth) {
            val textureName = "${fileName.removeSuffix(".png")}_$width"
            files["assets/$NAMESPACE/textures/font/$NAME/$textureName.png"] =
                toPngBytes(buildSheet(source, glyphs, width, cellHeight, topToLine, lineToBottom))
            providers.add(JsonObject().apply {
                addProperty("type", "bitmap")
                addProperty("file", "$NAMESPACE:font/$NAME/$textureName.png")
                addProperty("height", markedGlyphHeight)
                addProperty("ascent", markedAscent)
                add("chars", charsGrid(glyphs))
            })
        }
    }

    private fun visibleWidth(source: BufferedImage, glyph: Glyph, cellWidth: Int, cellHeight: Int): Int {
        for (x in cellWidth - 1 downTo 0) {
            for (y in 0 until cellHeight) {
                if (source.getRGB(glyph.sourceX + x, glyph.sourceY + y) ushr 24 != 0) return x + 1
            }
        }
        return 0
    }

    private fun buildSheet(
        source: BufferedImage,
        glyphs: List<Glyph>,
        width: Int,
        cellHeight: Int,
        topToLine: Int,
        lineToBottom: Int,
    ): BufferedImage {
        val markedHeight = cellHeight + MARKER_ROWS
        val columns = minOf(CELLS_PER_ROW, glyphs.size)
        val rows = (glyphs.size + columns - 1) / columns
        val sheet = BufferedImage(columns * width, rows * markedHeight, BufferedImage.TYPE_INT_ARGB)
        glyphs.forEachIndexed { index, glyph ->
            val cellX = index % columns * width
            val cellY = index / columns * markedHeight
            for (x in 0 until width) {
                for (y in 0 until cellHeight) {
                    sheet.setRGB(cellX + x, cellY + 1 + y, source.getRGB(glyph.sourceX + x, glyph.sourceY + y))
                }
            }
            EffectiveScreenElement.stampCorners(
                sheet, cellX, cellY, width, markedHeight,
                EffectiveScreenElement.textMarkerArgb(topToLine),
                EffectiveScreenElement.textMarkerArgb(lineToBottom),
            )
        }
        return sheet
    }

    private fun charsGrid(glyphs: List<Glyph>): JsonArray {
        val columns = minOf(CELLS_PER_ROW, glyphs.size)
        val grid = JsonArray()
        glyphs.chunked(columns).forEach { row ->
            val line = StringBuilder()
            row.forEach { line.appendCodePoint(it.codepoint) }
            repeat(columns - row.size) { line.append('\u0000') }
            grid.add(line.toString())
        }
        return grid
    }

    private fun readSource(name: String): ByteArray =
        javaClass.classLoader.getResourceAsStream(SOURCE_DIRECTORY + name)!!.use { it.readBytes() }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }
}
