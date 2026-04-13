package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.Player


abstract class EffectiveZone {
    abstract class TriggerData {
        abstract fun getName(): String
        abstract fun getCallback(): Player
    }

    abstract fun getTriggerData(): TriggerData

    fun registerZoneFromSelection()
}