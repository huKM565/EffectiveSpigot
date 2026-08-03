package ru.hukm.effectiveSpigot.minecraft.utils

/** A world-less integer block position `(x, y, z)` with `x:y:z` string serialization. */
data class EffectiveBlockPos(val x: Int, val y: Int, val z: Int) {
    /** Serializes to `"x:y:z"`. */
    fun serialize(): String {
        return "$x:$y:$z"
    }

    companion object {
        /** Parses a position from its `"x:y:z"` [serialize] form. */
        fun deserialize(data: String): EffectiveBlockPos {
            val parts = data.split(":")
            return EffectiveBlockPos(
                parts[0].toInt(),
                parts[1].toInt(),
                parts[2].toInt()
            )
        }
    }
}