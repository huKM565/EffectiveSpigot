package ru.hukm.effectiveSpigot.minecraft.zone

import ru.hukm.effectiveSpigot.utils.EffectiveUUIDConverter

object EffectiveZoneUUID : EffectiveUUIDConverter() {
    override fun getPrefix() = 0xABCDEFL
}