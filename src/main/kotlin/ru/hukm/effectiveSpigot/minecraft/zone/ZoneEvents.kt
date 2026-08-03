package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Base Bukkit event for zone movement transitions.
 *
 * @property entity the entity that moved
 * @property zone the zone involved
 * @property zoneBox the specific box within the zone
 */
abstract class EffectiveZoneEvent(
    val entity: LivingEntity,
    val zone: EffectiveZone,
    val zoneBox: EffectiveZone.ZoneBox
) : Event()

/** Fired when an entity enters a zone box (was outside, now inside). */
class EffectiveZoneEnterEvent(
    entity: LivingEntity,
    zone: EffectiveZone,
    zoneBox: EffectiveZone.ZoneBox
) : EffectiveZoneEvent(entity, zone, zoneBox) {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}

/** Fired when an entity leaves a zone box (was inside, now outside). */
class EffectiveZoneExitEvent(
    entity: LivingEntity,
    zone: EffectiveZone,
    zoneBox: EffectiveZone.ZoneBox
) : EffectiveZoneEvent(entity, zone, zoneBox) {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}

/** Fired each move tick while an entity stays inside a zone box (both from and to are inside). */
class EffectiveZoneInsideEvent(
    entity: LivingEntity,
    zone: EffectiveZone,
    zoneBox: EffectiveZone.ZoneBox
) : EffectiveZoneEvent(entity, zone, zoneBox) {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}

/** Fired when a new box is registered into a zone via [EffectiveZone.registerSelection]. */
class EffectiveZoneRegisteredEvent(
    val zone: EffectiveZone,
    val zoneBox: EffectiveZone.ZoneBox
) : Event() {
    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}
