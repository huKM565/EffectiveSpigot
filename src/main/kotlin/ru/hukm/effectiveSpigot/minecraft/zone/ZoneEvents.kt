package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class EffectiveZoneEvent(
    val entity: LivingEntity,
    val zone: EffectiveZone,
    val zoneBox: EffectiveZone.ZoneBox
) : Event()

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
