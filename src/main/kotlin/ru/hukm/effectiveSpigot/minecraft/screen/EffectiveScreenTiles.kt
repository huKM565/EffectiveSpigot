package ru.hukm.effectiveSpigot.minecraft.screen

import ru.hukm.effectiveSpigot.minecraft.menu.EffectiveTextureMenu
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Cuts a picture into tile glyphs for [EffectiveScreenImage] and [EffectiveScreenAnimation].
 *
 * The client cannot draw a glyph larger than its 256×256 font sheet, so a picture is cut into tiles depending only
 * on the PNG (never on the player's screen): the grid covers just the box around the non-transparent pixels, splits it
 * evenly into `⌈width / 252⌉ × ⌈height / 252⌉` cells, and every cell is trimmed to its own non-transparent pixels or
 * dropped if it has none. Smaller glyphs share the client's font sheets, so trimming also saves client memory.
 *
 * Every tile texture gets a transparent 1-pixel border. Its four corners carry the marker, and the border pixels next
 * to each corner, going inwards along the top or bottom row, carry: where that corner of the tile lies in the picture
 * (`(x + 1) << 12 | (y + 1)`, picture pixels, +1 so the border's outer edge at −1 stays positive), the picture width,
 * then any [extra] values. They all have alpha 1, below the text shader's discard threshold, so they are never drawn.
 */
internal object EffectiveScreenTiles {
    const val MAX_TILE_CONTENT = 252
    const val MAX_PICTURE_SIDE = 4090

    private const val DATA_ALPHA = 0x01000000

    private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

    /** The tiles of [source] that hold any non-transparent pixel, as rectangles in picture pixels. */
    fun cut(source: BufferedImage): List<Rectangle> {
        val content = contentBounds(source, Rectangle(0, 0, source.width, source.height)) ?: return emptyList()
        val columns = ceilDiv(content.width, MAX_TILE_CONTENT)
        val rows = ceilDiv(content.height, MAX_TILE_CONTENT)
        val cellWidth = ceilDiv(content.width, columns)
        val cellHeight = ceilDiv(content.height, rows)

        val tiles = ArrayList<Rectangle>()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val cellX = content.x + column * cellWidth
                val cellY = content.y + row * cellHeight
                val cell = Rectangle(
                    cellX,
                    cellY,
                    minOf(cellWidth, content.x + content.width - cellX),
                    minOf(cellHeight, content.y + content.height - cellY),
                )
                if (cell.width <= 0 || cell.height <= 0) continue
                contentBounds(source, cell)?.let { tiles += it }
            }
        }
        return tiles
    }

    /**
     * PNG bytes of [tile] cut from [source], with the border, the [marker] in the corners and the data next to each
     * corner. [extra] values (24 bits each) follow the picture width; the texture is widened with transparent columns
     * so every value fits in the border row.
     */
    fun texture(source: BufferedImage, tile: Rectangle, marker: Int, extra: List<Int> = emptyList()): ByteArray {
        val textureWidth = textureWidth(tile, extra.size)
        val textureHeight = tile.height + 2
        val texture = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
        for (dy in 0 until tile.height) {
            for (dx in 0 until tile.width) texture.setRGB(dx + 1, dy + 1, source.getRGB(tile.x + dx, tile.y + dy))
        }
        EffectiveScreenElement.stampCorners(texture, 0, 0, textureWidth, textureHeight, marker)

        val right = textureWidth - 1
        val bottom = textureHeight - 1
        for ((cornerX, cornerY, step) in listOf(Triple(0, 0, 1), Triple(right, 0, -1), Triple(0, bottom, 1), Triple(right, bottom, -1))) {
            val pictureX = if (cornerX == 0) tile.x else tile.x + textureWidth
            val pictureY = if (cornerY == 0) tile.y else tile.y + textureHeight
            val values = listOf((pictureX shl 12) or pictureY, source.width) + extra
            values.forEachIndexed { index, value ->
                texture.setRGB(cornerX + (index + 1) * step, cornerY, DATA_ALPHA or (value and 0xFFFFFF))
            }
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(texture, "png", output)
        return output.toByteArray()
    }

    private fun textureWidth(tile: Rectangle, extraCount: Int) = maxOf(tile.width, 2 * (2 + extraCount)) + 2

    /**
     * How far the client moves the text cursor after the glyph of [tile] drawn at font height [glyphHeight]: the
     * client takes the rightmost column with any alpha (the corner markers make it the full texture width), scales it
     * by `glyphHeight / texture height`, rounds and adds 1.
     */
    fun advance(tile: Rectangle, extraCount: Int, glyphHeight: Int): Int {
        val scale = glyphHeight.toFloat() / (tile.height + 2).toFloat()
        return (0.5 + (textureWidth(tile, extraCount).toFloat() * scale).toDouble()).toInt() + 1
    }

    /**
     * Negative-space characters that move the cursor back by [width] pixels, so a component of tiles takes no room
     * in the text around it.
     */
    fun backSpaces(width: Int): String {
        val builder = StringBuilder()
        var left = width
        for (space in EffectiveTextureMenu.BackSpace.entries.sortedByDescending { it.size }) {
            builder.append(space * (left / space.size))
            left %= space.size
        }
        return builder.toString()
    }

    /** The smallest rectangle inside [area] holding every non-transparent pixel of [source], or null if none. */
    private fun contentBounds(source: BufferedImage, area: Rectangle): Rectangle? {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        for (y in area.y until area.y + area.height) {
            for (x in area.x until area.x + area.width) {
                if (source.getRGB(x, y) ushr 24 == 0) continue
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        return if (maxX < 0) null else Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
