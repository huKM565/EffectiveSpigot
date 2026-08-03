package ru.hukm.effectiveSpigot.minecraft.zone

import ru.hukm.effectiveSpigot.utils.EffectiveUUIDConverter

/** [EffectiveUUIDConverter] with a fixed prefix, encoding zone-box ids as render-handle UUIDs. */
object EffectiveZoneUUID : EffectiveUUIDConverter() {
    override fun getPrefix() = 0xABCDEFL
}