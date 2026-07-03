package ru.hukm.effectiveSpigot.minecraft.advancements

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule

abstract class EffectiveAdvancement {
    data class DisplayData(
        val title: String,
        val description: String,
        val icon: IconData,
        val frame: FrameType,
        val background: String? = null,
        val showToast: Boolean = true,
        val announceToChat: Boolean = true,
        val hidden: Boolean = false
    )

    data class IconData(
        val material: Material,
        val components: HashMap<String, String>
    ) {
        companion object {
            fun fromItem(item: ItemStack): IconData {
                val components = hashMapOf<String, String>()
                item.itemMeta?.itemModel?.let { components["minecraft:item_model"] = it.toString() }
                return IconData(item.type, components)
            }
        }
    }

    enum class FrameType(val frame: String) {
        TASK("task"),
        GOAL("goal")            ,
        CHALLENGE("challenge")
    }

    companion object {
        val namespacedNameToEffectiveAdvancements = hashMapOf<String, EffectiveAdvancement>()

        private fun loadAll() {
            val all = namespacedNameToEffectiveAdvancements.values.toList()
            val byKey = all.associateBy { it.getNamespacedName() }
            val visited = hashSetOf<String>()
            val ordered = arrayListOf<EffectiveAdvancement>()

            fun visit(advancement: EffectiveAdvancement) {
                if (!visited.add(advancement.getNamespacedName())) return
                val parent = advancement.getParent()?.let { byKey[it.toString()] }
                if (parent != null) visit(parent)
                ordered.add(advancement)
            }

            for (advancement in all) visit(advancement)

            for (advancement in ordered) {
                advancement.load()
            }
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedNameToEffectiveAdvancements.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.advancement.already_registered", namespacedName))
        }

        namespacedNameToEffectiveAdvancements[namespacedName] = this
    }

    internal fun load() {
        Bukkit.getUnsafe()
            .loadAdvancement(NamespacedKey(getNamespacedData().first, getNamespacedData().second), buildJson())
    }

    fun isGrantedTo(player: Player): Boolean {
        val key = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val advancement = Bukkit.getAdvancement(key) ?: return false
        return player.getAdvancementProgress(advancement).isDone
    }

    fun grant(player: Player) {
        val key = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val advancement = Bukkit.getAdvancement(key) ?: return
        val progress = player.getAdvancementProgress(advancement)
        if (progress.isDone) return
        for (criterion in progress.remainingCriteria) {
            progress.awardCriteria(criterion)
        }
    }

    fun buildJson(): String {
        val display = getDisplay()

        val iconJson = linkedMapOf<String, Any>("id" to display.icon.material.key.toString())
        if (display.icon.components.isNotEmpty()) {
            iconJson["components"] = display.icon.components
        }

        val displayJson = linkedMapOf<String, Any>(
            "title" to display.title,
            "description" to display.description,
            "icon" to iconJson,
            "frame" to display.frame.frame
        )
        display.background?.let { displayJson["background"] = it }
        if (!display.showToast) displayJson["show_toast"] = false
        if (!display.announceToChat) displayJson["announce_to_chat"] = false
        if (display.hidden) displayJson["hidden"] = true

        val root = linkedMapOf<String, Any>()
        getParent()?.let { root["parent"] = it.toString() }
        root["display"] = displayJson
        root["criteria"] = mapOf("granted" to mapOf("trigger" to "minecraft:impossible"))

        return GSON.toJson(root)
    }

    abstract fun getParent(): NamespacedKey?
    abstract fun getDisplay(): DisplayData
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>


    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
    }

    class Events : Listener {
        @EventHandler
        fun onServerLoad(event: ServerLoadEvent) {
            loadAll()
        }
    }
}

private val GSON = Gson()
