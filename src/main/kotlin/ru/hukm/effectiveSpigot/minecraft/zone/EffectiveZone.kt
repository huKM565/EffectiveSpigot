package ru.hukm.effectiveSpigot.minecraft.zone

import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.world.EffectiveWorld
import ru.hukm.effectiveSpigot.minecraft.world.chunk.dataclasses.EffectiveBlockData
import java.awt.Color as AwtColor
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Base class for a named spatial zone — a logical region made up of one or more box selections.
 *
 * A subclass declares identity ([getNamespacedData]), whether boxes remember who created them
 * ([doRememberOwner]) and the render color ([getZoneColor]). Like [ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem],
 * a zone registers itself on construction and must be instantiated (e.g. from `onEnable`) to become active.
 *
 * Boxes are added at runtime via [registerSelection] (usually from an in-game selection), persisted in
 * the world's persistent data, and restored on server load. As entities move, the framework fires
 * [EffectiveZoneEnterEvent], [EffectiveZoneExitEvent] and [EffectiveZoneInsideEvent]; listen for these to
 * implement zone behaviour.
 *
 * A built-in `/ezone` command manages zone selections and boxes in-game.
 */
abstract class EffectiveZone {
    /**
     * One axis-aligned box belonging to a zone.
     *
     * @property id unique box id across all zones
     * @property firstPos one corner (inclusive)
     * @property secondPos opposite corner (inclusive)
     * @property worldUUID world the box lives in
     * @property owner creator, or null when the zone does not remember owners
     */
    data class ZoneBox(
        val id: Int,
        val firstPos: EffectiveBlockPos,
        val secondPos: EffectiveBlockPos,
        val worldUUID: UUID,
        val owner: UUID? = null
    ) {
        /** Whether [location] lies within this box (inclusive bounds, same world). */
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

        /** Center of the box as a [Location] (block centers, `+0.5` offset). */
        fun getCenter(): Location {
            val centerX = (firstPos.x + secondPos.x) / 2.0 + 0.5
            val centerY = (firstPos.y + secondPos.y) / 2.0 + 0.5
            val centerZ = (firstPos.z + secondPos.z) / 2.0 + 0.5

            return Location(Bukkit.getWorld(worldUUID), centerX, centerY, centerZ)
        }

        /** All blocks within the box (inclusive). Iterates the whole volume — costly for large boxes. */
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

        /** Entities currently inside the box. Note: bounds here are exclusive (strictly between corners). */
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
        
        /** Serializes the box to the string form used in persistent data. */
        fun serialize(): String {
            return "${id};${firstPos.serialize()};${secondPos.serialize()};${owner ?: ""}"
        }

        /** Deterministic UUID derived from the box [id] (used as the render handle). */
        fun getUUIDFromID() = EffectiveZoneUUID.toUUID(id.toLong())

        companion object {
            /** A random bright, saturated color — handy as a per-zone render color. */
            fun randomColor(): Color =
                Color.fromRGB(AwtColor.HSBtoRGB(Math.random().toFloat(), 0.85f, 1.0f) and 0xFFFFFF)

            /** Reconstructs a box from its [serialize] form for the given world. */
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

    companion object {
        private val _namespacedKeyToZone = hashMapOf<String, EffectiveZone>()

        /** Read-only registry of all constructed zones, keyed by [getNamespacedName]. */
        val namespacedKeyToZone: Map<String, EffectiveZone> get() = _namespacedKeyToZone

        private var nextZoneBoxId: Int = 0

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    event<ServerLoadEvent> {
                        namespacedKeyToZone.values.forEach { zone ->
                            zone.loadBoxesFromMemory()
                        }
                    }

                    event<PlayerMoveEvent> {
                        tryTrigger(it.player, it.from, it.to)
                    }

                    event<EntityMoveEvent> {
                        tryTrigger(it.entity, it.from, it.to)
                    }
                }
            }
        }

        /** Registered zone for a namespaced name, or null if none. */
        fun getZoneByNamespacedKey(namespacedKey: String): EffectiveZone? {
            return namespacedKeyToZone[namespacedKey]
        }

        /** Total number of boxes across all registered zones. */
        fun getCountZoneBoxes(): Int {
            var count = 0
            namespacedKeyToZone.values.forEach {
                count += it.zoneBoxes.count()
            }
            return count
        }

        /** Finds a box by its global [id] across all zones, or null. */
        fun getZoneBoxById(id: Int): ZoneBox? {
            for (zone in namespacedKeyToZone.values) {
                for (zoneBox in zone.zoneBoxes) {
                    if (zoneBox.id == id) return zoneBox
                }
            }

            return null
        }

        /** All boxes created by [ownerUUID], paired with their zone's namespaced key. */
        fun getZoneBoxesByOwner(ownerUUID: UUID): List<Pair<String, ZoneBox>> {
            val result = mutableListOf<Pair<String, ZoneBox>>()
            namespacedKeyToZone.forEach { (key, zone) ->
                zone.zoneBoxes.forEach { box ->
                    if (box.owner == ownerUUID) result.add(key to box)
                }
            }
            return result
        }

        /** Deletes the box with [id] (from memory and persistent data). Returns false if not found. */
        fun deleteZoneBoxById(id: Int): Boolean {
            for (zone in namespacedKeyToZone.values) {
                val zoneBox = zone.zoneBoxes.firstOrNull { it.id == id } ?: continue
                zone.deleteBoxInMemory(zoneBox)
                return true
            }

            return false
        }

        /**
         * Adds a new box to the zone [namespacedKey] from a corner/corner/world selection, persists it,
         * starts rendering it and fires [EffectiveZoneRegisteredEvent].
         *
         * @param ownerUUID recorded only if the zone [doRememberOwner]
         * @return the created [ZoneBox]
         */
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

            Bukkit.getPluginManager().callEvent(EffectiveZoneRegisteredEvent(zone, zoneBox))
            return zoneBox
        }

        /**
         * Fires the appropriate enter/exit/inside zone event(s) for [entity] moving [from] → [to].
         * Called by the movement listeners; rarely needed directly.
         */
        fun tryTrigger(entity: LivingEntity, from: Location, to: Location) {
            for (zone in namespacedKeyToZone.values) {
                for (zoneBox in zone.zoneBoxes) {
                    val fromIsInside = zoneBox.isInside(from)
                    val toIsInside = zoneBox.isInside(to)

                    val event = when {
                        !fromIsInside && toIsInside -> EffectiveZoneEnterEvent(entity, zone, zoneBox)
                        fromIsInside && !toIsInside -> EffectiveZoneExitEvent(entity, zone, zoneBox)
                        fromIsInside && toIsInside  -> EffectiveZoneInsideEvent(entity, zone, zoneBox)
                        else -> null
                    }

                    if (event != null) {
                        Bukkit.getPluginManager().callEvent(event)
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
        _namespacedKeyToZone[namespacedName] = this
    }

    /** Boxes currently belonging to this zone (loaded from persistent data on server load). */
    var zoneBoxes: ArrayList<ZoneBox> = arrayListOf()

    /** Owning plugin and a plugin-unique id; together they form the [getNamespacedName]. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    /** Whether new boxes should record the player who created them. */
    abstract fun doRememberOwner(): Boolean

    /** Color used when rendering this zone's boxes. */
    abstract fun getZoneColor(): Color

    /** Unique identity as `"<plugin-name>/<id>"`, both lowercased. */
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
}