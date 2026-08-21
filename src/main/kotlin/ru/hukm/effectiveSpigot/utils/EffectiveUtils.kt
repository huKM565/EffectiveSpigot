package ru.hukm.effectiveSpigot.utils

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Misc general-purpose helpers. */
object EffectiveUtils {
    /** Packs two ints into one long ([first] in the high 32 bits, [second] in the low 32). */
    fun twoIntToLong(first: Int, second: Int): Long {
        return (first.toLong() shl 32) or (second.toLong() and 0xFFFFFFFFL)
    }

    /** A fully transparent [size]×[size] PNG — used to hide a vanilla texture or as an invisible item texture. */
    fun transparentPng(size: Int): ByteArray {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        return ByteArrayOutputStream().use { ImageIO.write(image, "png", it); it.toByteArray() }
    }
}
