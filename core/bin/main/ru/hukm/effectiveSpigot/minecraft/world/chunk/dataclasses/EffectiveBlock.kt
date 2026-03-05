package ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses

import org.bukkit.Material

data class EffectiveBlock(
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material
)