package ru.hukm.effectiveSpigot.minecraft.zone

import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData
import java.awt.Color as AwtColor
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

abstract class EffectiveZone {
    enum class ActivationType {
        ENTER,
        INSIDE,
        EXIT
    }

    data class ZoneBox(
        val id: Int,
        val firstPos: EffectiveBlockPos,
        val secondPos: EffectiveBlockPos,
        val worldUUID: UUID,
        val owner: UUID? = null
    ) {
        fun isInside(location: Location): Boolean {
            if (location.world?.uid != worldUUID) {
                return false
            }

            val minX = kotlin.math.min(firstPos.x, secondPos.x)
            val maxX = kotlin.math.max(firstPos.x, secondPos.x)
            val minY = kotlin.math.min(firstPos.y, secondPos.y)
            val maxY = kotlin.math.max(firstPos.y, secondPos.y)
            val minZ = kotlin.math.min(firstPos.z, secondPos.z)
            val maxZ = kotlin.math.max(firstPos.z, secondPos.z)

            return location.blockX >= minX && location.blockX <= maxX &&
                   location.blockY >= minY && location.blockY <= maxY &&
                   location.blockZ >= minZ && location.blockZ <= maxZ
        }

        fun getCenter(): Location {
            val centerX = (firstPos.x + secondPos.x) / 2.0 + 0.5
            val centerY = (firstPos.y + secondPos.y) / 2.0 + 0.5
            val centerZ = (firstPos.z + secondPos.z) / 2.0 + 0.5

            return Location(Bukkit.getWorld(worldUUID), centerX, centerY, centerZ)
        }

        fun getBlocksInside(): List<EffectiveBlockData> {
            val blocks = mutableListOf<EffectiveBlockData>()

            val minX = min(firstPos.x, secondPos.x)
            val maxX = max(firstPos.x, secondPos.x)
            val minY = min(firstPos.y, secondPos.y)
            val maxY = max(firstPos.y, secondPos.y)
            val minZ = min(firstPos.z, secondPos.z)
            val maxZ = max(firstPos.z, secondPos.z)

            val world = Bukkit.getWorld(worldUUID)!!
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        blocks.add(
                            EffectiveWorld.getBlock(world, EffectiveBlockPos(
                                x,
                                y,
                                z
                            ))!!
                        )
                    }
                }
            }

            return blocks
        }

        fun getEntitiesInside(): List<Entity> {
            val minX = min(firstPos.x, secondPos.x)
            val maxX = max(firstPos.x, secondPos.x)
            val minY = min(firstPos.y, secondPos.y)
            val maxY = max(firstPos.y, secondPos.y)
            val minZ = min(firstPos.z, secondPos.z)
            val maxZ = max(firstPos.z, secondPos.z)


            return Bukkit.getWorld(worldUUID)!!.entities.filter {
                val loc = it.location

                minX < loc.blockX && loc.blockX < maxX &&
                        minY < loc.blockY && loc.blockY < maxY &&
                        minZ < loc.blockZ && loc.blockZ < maxZ
            }

        }
        
        fun serialize(): String {
            return "${id};${firstPos.serialize()};${secondPos.serialize()};${owner ?: ""}"
        }

        fun getUUIDFromID() = EffectiveZoneUUID.toUUID(id.toLong())

        companion object {
            fun randomColor(): Color =
                Color.fromRGB(AwtColor.HSBtoRGB(Math.random().toFloat(), 0.85f, 1.0f) and 0xFFFFFF)

            fun deserialize(data: String, worldUUID: UUID): ZoneBox {
                val parts = data.split(";")
                return ZoneBox(
                    parts[0].toInt(),
                    EffectiveBlockPos.deserialize(parts[1]),
                    EffectiveBlockPos.deserialize(parts[2]),
                    worldUUID,
                    if (parts.size > 3 && parts[3].isNotEmpty()) UUID.fromString(parts[3]) else null
                )
            }
        }
    }

    abstract class WalkTriggerData {
        abstract fun getEntityTypesForActivationType(): List<Class<out LivingEntity>>
        abstract fun trigger(livingEntity: LivingEntity, zoneBox: ZoneBox, activationType: ActivationType)
    }

    companion object {
        val namespacedKeyToZone = hashMapOf<String, EffectiveZone>()

        private var nextZoneBoxId: Int = 0

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun getZoneByNamespacedKey(namespacedKey: String): EffectiveZone? {
            return namespacedKeyToZone[namespacedKey]
        }

        fun getCountZoneBoxes(): Int {
            var count = 0
            namespacedKeyToZone.values.forEach {
                count += it.zoneBoxes.count()
            }
            return count
        }

        fun getZoneBoxById(id: Int): ZoneBox? {
            for (zone in namespacedKeyToZone.values) {
                for (zoneBox in zone.zoneBoxes) {
                    if (zoneBox.id == id) return zoneBox
                }
            }

            return null
        }

        fun getZoneBoxesByOwner(ownerUUID: UUID): List<Pair<String, ZoneBox>> {
            val result = mutableListOf<Pair<String, ZoneBox>>()
            namespacedKeyToZone.forEach { (key, zone) ->
                zone.zoneBoxes.forEach { box ->
                    if (box.owner == ownerUUID) result.add(key to box)
                }
            }
            return result
        }

        fun deleteZoneBoxById(id: Int): Boolean {
            for (zone in namespacedKeyToZone.values) {
                val zoneBox = zone.zoneBoxes.firstOrNull { it.id == id } ?: continue
                zone.deleteBoxInMemory(zoneBox)
                return true
            }

            return false
        }

        fun registerSelection(selection: Triple<EffectiveBlockPos, EffectiveBlockPos, UUID>, namespacedKey: String, ownerUUID: UUID? = null): ZoneBox {
            val zone = getZoneByNamespacedKey(namespacedKey)!!

            val zoneBox = ZoneBox(
                nextZoneBoxId++,
                selection.first,
                selection.second,
                selection.third,
                owner = if (zone.doRememberOwner()) ownerUUID else null
            )

            zone.saveBoxInMemory(zoneBox)
            EffectiveZoneRenderer.startRendering(selection, EffectiveZoneUUID.toUUID(zoneBox.id.toLong()), )

            zone.selectionRegistered(zoneBox)
            return zoneBox
        }

        fun tryTrigger(entity: LivingEntity, from: Location, to: Location) {
            for (zone in namespacedKeyToZone.values) {
                val allowedTypes = zone.getWalkTriggerData().getEntityTypesForActivationType()
                val entityClass = entity::class.java

                if (allowedTypes.any { it.isAssignableFrom(entityClass) }) {
                    for (zoneBox in zone.zoneBoxes) {
                        val fromIsInside = zoneBox.isInside(from)
                        val toIsInside = zoneBox.isInside(to)

                        val activationType = when {
                            !fromIsInside && toIsInside -> ActivationType.ENTER
                            fromIsInside && !toIsInside -> ActivationType.EXIT
                            fromIsInside && toIsInside  -> ActivationType.INSIDE
                            else -> null
                        }

                        if (activationType != null) {
                            zone.getWalkTriggerData().trigger(entity, zoneBox, activationType)
                        }
                    }
                }
            }
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedKeyToZone.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.zones.already_registered", namespacedName))
        }
        namespacedKeyToZone[namespacedName] = this
    }

    var zoneBoxes: ArrayList<ZoneBox> = arrayListOf()

    abstract fun getWalkTriggerData(): WalkTriggerData
    abstract fun selectionRegistered(zoneBox: ZoneBox)
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>
    abstract fun doRememberOwner(): Boolean
    abstract fun getZoneColor(): Color

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }

    private fun deleteBoxInMemory(zoneBox: ZoneBox) {
        val namespacedKey = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val world = Bukkit.getWorld(zoneBox.worldUUID)!!

        val serializeAllBoxes = getSerializeBoxesFromMemory(world, namespacedKey) ?: return
        val targetSerialized = zoneBox.serialize()
        val remaining = serializeAllBoxes
            .split("||")
            .filter { it.isNotEmpty() && it != targetSerialized }

        EffectiveZoneRenderer.stopRendering(zoneBox.getUUIDFromID())

        zoneBoxes.remove(zoneBox)

        EffectiveDataContainerUtils.setContainerValue(
            world,
            namespacedKey,
            PersistentDataType.STRING,
            remaining.joinToString("||")
        )
    }

    private fun saveBoxInMemory(zoneBox: ZoneBox) {
        val namespacedKey = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val world = Bukkit.getWorld(zoneBox.worldUUID)!!

        val beforeBoxes = EffectiveDataContainerUtils.getContainerValue(
            world,
            namespacedKey,
            PersistentDataType.STRING
        )

        val newBoxData = zoneBox.serialize()
        val updatedBoxes = if (beforeBoxes.isNullOrEmpty()) {
            newBoxData
        } else {
            "$beforeBoxes||$newBoxData"
        }

        EffectiveDataContainerUtils.setContainerValue(
            world,
            namespacedKey,
            PersistentDataType.STRING,
            updatedBoxes
        )

        zoneBoxes.add(zoneBox)
    }

    private fun loadBoxesFromMemory() {
        val namespacedKey = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val allBoxes = arrayListOf<ZoneBox>()

        Bukkit.getWorlds().forEach { world ->
            val savedBoxes = getSerializeBoxesFromMemory(world, namespacedKey) ?: return@forEach

            savedBoxes.split("||")
                .filter { it.isNotEmpty() }
                .forEach { boxData ->
                    val zoneBox = ZoneBox.deserialize(boxData, world.uid)
                    allBoxes.add(zoneBox)
                    EffectiveZoneRenderer.startRendering(
                        Triple(zoneBox.firstPos, zoneBox.secondPos, zoneBox.worldUUID),
                        zoneBox.getUUIDFromID(),
                        getZoneColor()
                    )
                }
        }

        zoneBoxes = allBoxes

        val maxId = allBoxes.maxOfOrNull { it.id } ?: -1
        if (maxId + 1 > nextZoneBoxId) nextZoneBoxId = maxId + 1
    }

    private fun getSerializeBoxesFromMemory(world: World, namespacedKey: NamespacedKey): String? {
        return EffectiveDataContainerUtils.getContainerValue(
            world,
            namespacedKey,
            PersistentDataType.STRING
        )
    }

    class Events : Listener {
        @EventHandler
        fun onServerLoad(event: ServerLoadEvent) {
            namespacedKeyToZone.values.forEach {
                it.loadBoxesFromMemory()
            }
        }

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            tryTrigger(event.player, event.from, event.to)
        }

        @EventHandler
        fun onEntityMove(event: EntityMoveEvent) {
            tryTrigger(event.entity, event.from, event.to)
        }
    }
}