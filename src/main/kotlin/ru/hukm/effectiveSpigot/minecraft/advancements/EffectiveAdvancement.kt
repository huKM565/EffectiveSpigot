package ru.hukm.effectiveSpigot.minecraft.advancements

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event

/**
 * Base class for a custom advancement.
 *
 * A subclass defines its [getDisplay] (title, icon, frame, …), optional [getParent] to place it in a
 * tree, and identity via [getNamespacedData]. Advancements register on construction and are (re)loaded
 * in parent-before-child order; grant them with [grant] and check with [isGrantedTo].
 *
 * This is purely the client-side visual layer — the entry in the advancements screen, the toast on
 * grant, its tree placement. It has no criteria and never fires itself; deciding *when* a player
 * earns it is the child plugin's job (listen for the relevant event and call [grant]).
 */
abstract class EffectiveAdvancement {
    /**
     * Visual/behavioral display of an advancement.
     * @property background background texture path (root advancements only)
     * @property showToast whether the toast pops on grant
     * @property announceToChat whether the grant is broadcast to chat
     * @property hidden whether it stays hidden until unlocked
     */
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

    /** The advancement icon: a [Material] plus optional item components (e.g. custom item model). */
    data class IconData(
        val material: Material,
        val components: HashMap<String, String>
    ) {
        companion object {
            /** Builds an [IconData] from an [ItemStack], carrying over its item-model component. */
            fun fromItem(item: ItemStack): IconData {
                val components = hashMapOf<String, String>()
                item.itemMeta?.itemModel?.let { components["minecraft:item_model"] = it.toString() }
                return IconData(item.type, components)
            }
        }
    }

    /** Advancement frame style shown in the UI. */
    enum class FrameType(val frame: String) {
        TASK("task"),
        GOAL("goal"),
        CHALLENGE("challenge")
    }

    companion object {
        private val _namespacedNameToEffectiveAdvancements = hashMapOf<String, EffectiveAdvancement>()

        /** Read-only registry of all constructed advancements, keyed by their namespaced name. */
        val namespacedNameToEffectiveAdvancements: Map<String, EffectiveAdvancement> get() = _namespacedNameToEffectiveAdvancements

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
                    event<ServerLoadEvent> {
                        loadAll()
                    }
                }
            }
        }
    }

    init {
        val namespacedName = getNamespacedName()
        if (namespacedNameToEffectiveAdvancements.containsKey(namespacedName)) {
            throw IllegalArgumentException(Locale.getMessage("errors.advancement.already_registered", namespacedName))
        }

        _namespacedNameToEffectiveAdvancements[namespacedName] = this
    }

    internal fun load() {
        Bukkit.getUnsafe()
            .loadAdvancement(NamespacedKey(getNamespacedData().first, getNamespacedData().second), buildJson())
    }

    /** Whether [player] has completed this advancement. */
    fun isGrantedTo(player: Player): Boolean {
        val key = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val advancement = Bukkit.getAdvancement(key) ?: return false
        return player.getAdvancementProgress(advancement).isDone
    }

    /** Awards all remaining criteria to [player], completing this advancement (no-op if already done). */
    fun grant(player: Player) {
        val key = NamespacedKey(getNamespacedData().first, getNamespacedData().second)
        val advancement = Bukkit.getAdvancement(key) ?: return
        val progress = player.getAdvancementProgress(advancement)
        if (progress.isDone) return
        for (criterion in progress.remainingCriteria) {
            progress.awardCriteria(criterion)
        }
    }

    private fun buildJson(): String {
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

    /** Parent advancement key to attach under, or null for a root advancement. */
    abstract fun getParent(): NamespacedKey?

    /** Display data (title, description, icon, frame, flags). */
    abstract fun getDisplay(): DisplayData

    /** Owning plugin and a plugin-unique id; together they form the advancement key. */
    abstract fun getNamespacedData(): Pair<JavaPlugin, String>


    fun getNamespacedName(): String {
        return getNamespacedData().first.description.name.lowercase() + ":" + getNamespacedData().second.lowercase().trim()
    }
}

private val GSON = Gson()
