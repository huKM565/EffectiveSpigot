package ru.hukm.effectiveSpigot.minecraft.utils

data class EffectiveBlockPos(val x: Int, val y: Int, val z: Int) {
    fun serialize(): String {
        return "$x:$y:$z"
    }

    companion object {
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