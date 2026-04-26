package ru.hukm.effectiveSpigot.utils

import java.util.UUID

abstract class EffectiveUUIDConverter {
    fun toUUID(number: Long): UUID {
        return UUID(getPrefix(), number)
    }

    fun fromUUID(uuid: UUID): Long? {
        if (uuid.mostSignificantBits != getPrefix()) return null

        return uuid.leastSignificantBits
    }

    abstract fun getPrefix(): Long
}