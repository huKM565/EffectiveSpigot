package ru.hukm.effectiveSpigot.utils

/** Misc general-purpose helpers. */
object EffectiveUtils {
    /** Packs two ints into one long ([first] in the high 32 bits, [second] in the low 32). */
    fun twoIntToLong(first: Int, second: Int): Long {
        return (first.toLong() shl 32) or (second.toLong() and 0xFFFFFFFFL)
    }
}
