package ru.hukm.effectiveSpigot.minecraft.zone

import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveBlockPos
import java.util.UUID
import kotlin.collections.set

abstract class EffectiveZone {
    enum class ActivationType {
        ENTER,
        INSIDE,
        EXIT
    }

    data class ZoneBox(
        val id: UUID,
        val selection: Pair<EffectiveBlockPos, EffectiveBlockPos>
    )

    companion object {
        val namespacedKeyToEffectiveZone = hashMapOf<String, EffectiveZone>()
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedKeyToEffectiveZone.containsKey(namespacedName)) {
            throw IllegalArgumentException(LanguageModule.getMessage("errors.zones.already_registered", namespacedName))
        }
        namespacedKeyToEffectiveZone[namespacedName] = this
    }

    private val boxes = listOf<Pair<EffectiveBlockPos, EffectiveBlockPos>>()

    abstract fun getTriggerCallback(): (Player, ActivationType) -> Unit
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>

    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + "/" + getNamespacedData().second.lowercase()
    }

    fun registerSelection(selection: Pair<EffectiveBlockPos, EffectiveBlockPos>): ZoneBox {
        if ()
    }

    private fun saveBoxInMemory() {

    }
}