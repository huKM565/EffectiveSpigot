package ru.hukm.effectiveSpigot.utils

import java.util.UUID

/**
 * Maps longs to/from UUIDs using a fixed high-bits [getPrefix] as a namespace, so ids can be encoded as
 * UUIDs and recognized back (e.g. zone-box render handles).
 */
abstract class EffectiveUUIDConverter {
    /** Encodes [number] into a UUID under this converter's prefix. */
    fun toUUID(number: Long): UUID {
        return UUID(getPrefix(), number)
    }

    /** Decodes the long from [uuid], or null if its prefix doesn't match this converter. */
    fun fromUUID(uuid: UUID): Long? {
        if (uuid.mostSignificantBits != getPrefix()) return null

        return uuid.leastSignificantBits
    }

    /** The fixed high-64-bits namespace identifying UUIDs produced by this converter. */
    abstract fun getPrefix(): Long
}