package ru.hukm.effectiveSpigot.minecraft.resourcepack

import ru.hukm.effectiveSpigot.Locale

/**
 * A validated handle to one glyph slot in the generated resource pack: an opaque wrapper around a
 * Supplementary Private Use Area codepoint ([codePoint], planes 15–16, `U+F0000` upward) that is guaranteed
 * to map to a real, registered glyph. The supplementary PUA is used instead of the BMP PUA so glyphs never
 * clash with the many resource packs that occupy `U+E000+`; SPUA-A is filled first, then SPUA-B (~131k slots).
 *
 * Instances cannot be created from outside the framework — the constructor is `internal` and `copy()`
 * follows it (`@ConsistentCopyVisibility`), so the token can't be forged by ordinary means. The only source
 * is [getNextFree], which auto-assigns the next free codepoint; callers never pick it themselves.
 *
 * Because codepoints come from an in-memory counter, a glyph's [codePoint] is stable only within a single
 * server session and may differ across restarts (it depends on allocation order). That is why the resource
 * pack is regenerated on every start, so its font providers always match the current session. Never persist
 * a raw glyph character (item lore saved in the world, books) — store a way to re-obtain the token instead.
 *
 * Because it can only originate from a real registration, an [EffectiveFontChar] is a proof that its
 * [codePoint] is a distinct, existing glyph. The codepoint is outside the BMP, so its text form ([string],
 * also returned by [invoke]) is a UTF-16 surrogate pair — embed it in chat/GUI text, lore or
 * [net.kyori.adventure.text.Component]s to draw the glyph.
 *
 * @property codePoint the reserved Supplementary Private Use Area codepoint (plane 15 or 16) that renders as this glyph
 */
@ConsistentCopyVisibility
data class EffectiveFontChar internal constructor(
    val codePoint: Int
) {
    /** The glyph as text — a UTF-16 surrogate pair, since [codePoint] is outside the BMP. */
    val string: String = String(Character.toChars(codePoint))

    operator fun invoke() = string

    companion object {
        // Supplementary Private Use Area, filled A (plane 15) then B (plane 16). The two ranges are NOT
        // contiguous — U+FFFFE/U+FFFFF between them are noncharacters, so the counter skips over them.
        private const val SPUA_A_START = 0xF0000
        private const val SPUA_A_SIZE = 0xFFFFD - 0xF0000 + 1   // 65534
        private const val SPUA_B_START = 0x100000
        private const val SPUA_B_SIZE = 0x10FFFD - 0x100000 + 1 // 65534
        private const val TOTAL_SIZE = SPUA_A_SIZE + SPUA_B_SIZE // 131068

        private var nextIndex = 0

        /**
         * Allocates and returns the next free glyph, auto-assigning a Supplementary Private Use Area
         * codepoint — SPUA-A (`U+F0000`+) first, then SPUA-B (`U+100000`+). Callers never pick it. Throws
         * once both areas are exhausted ([TOTAL_SIZE] glyphs).
         */
        fun getNextFree(): EffectiveFontChar {
            if (nextIndex >= TOTAL_SIZE)
                throw IllegalStateException(Locale.getMessage("errors.resourcepack.glyphs_exhausted", TOTAL_SIZE))
            val i = nextIndex++
            val codePoint = if (i < SPUA_A_SIZE) SPUA_A_START + i else SPUA_B_START + (i - SPUA_A_SIZE)
            return EffectiveFontChar(codePoint)
        }
    }
}
