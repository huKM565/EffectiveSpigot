package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import java.util.UUID

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
        val worldUUID: UUID
    ) {
        fun serialize(): String {
            return "${id};${firstPos.serialize()};${secondPos.serialize()}"
        }
        
        companion object {
            fun deserialize(data: String, worldUUID: UUID): ZoneBox {
                val parts = data.split(";")
                return ZoneBox(
                    parts[0].toInt(),
                    EffectiveBlockPos.deserialize(parts[1]),
                    EffectiveBlockPos.deserialize(parts[2]),
                    worldUUID
                )
            }
        }
    }

    abstract class TriggerData {
        abstract fun getEntityTypesForActivationType(): List<Entity>
        abstract fun trigger(player: Player, activationType: ActivationType)
    }

    companion object {
        val namespacedKeyToZone = hashMapOf<String, EffectiveZone>()

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


        fun registerSelection(selection: Triple<EffectiveBlockPos, EffectiveBlockPos, UUID>, namespacedKey: String): ZoneBox {
            val zone = getZoneByNamespacedKey(namespacedKey)!!

            val zoneBox = ZoneBox(
                getCountZoneBoxes(),
                selection.first,
                selection.second,
                selection.third
            )

            zone.saveBoxInMemory(zoneBox)
            zone.zoneBoxes.add(zoneBox)

            return zoneBox
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedKeyToZone.containsKey(namespacedName)) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.zones.already_registered", namespacedName))
        }
        namespacedKeyToZone[namespacedName] = this
    }

    lateinit var zoneBoxes: ArrayList<ZoneBox>

    abstract fun getTriggerData(): TriggerData
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
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

    fun loadBoxesFromMemory() {
        val namespacedKey = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val allBoxes = arrayListOf<ZoneBox>()
        
        Bukkit.getWorlds().forEach { world ->
            val savedBoxes = EffectiveDataContainerUtils.getContainerValue(
                world,
                namespacedKey,
                PersistentDataType.STRING
            ) ?: return@forEach

            savedBoxes.split("||").forEach { boxData ->
                val de = ZoneBox.deserialize(boxData, world.uid)
                allBoxes.add(de)
                EffectiveZoneRenderer.startRendering({
                    val zoneBox = allBoxes.find { it.id == de.id }
                    zoneBox?.firstPos to zoneBox?.secondPos
                })
            }
        }
        
        zoneBoxes = allBoxes
    }

    class Events : Listener {
        @EventHandler
        fun onServerLoad(event: ServerLoadEvent) {
            namespacedKeyToZone.values.forEach {
                it.loadBoxesFromMemory()
            }
        }
    }
}