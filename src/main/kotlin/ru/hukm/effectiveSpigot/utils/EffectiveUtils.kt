package ru.hukm.effectiveSpigot.utils

object EffectiveUtils {
    fun twoIntToLong(first: Int, second: Int): Long {
        return (first.toLong() shl 32) or (second.toLong() and 0xFFFFFFFFL)
    }
}
