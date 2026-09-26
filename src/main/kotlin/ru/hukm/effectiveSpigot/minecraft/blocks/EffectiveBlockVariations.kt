package ru.hukm.effectiveSpigot.minecraft.blocks

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import ru.hukm.effectiveSpigot.Locale
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Persistent registry of note-block variations: block namespaced name → variation (`1..799`).
 *
 * A variation is baked into every placed block, so once a block has a number it must keep it for as long
 * as the world exists. The registry lives at the save root (`<level>/data/effectivespigot/block_variations.json`,
 * next to the scoreboard and maps), shared by every dimension: resetting one dimension keeps it, and it
 * only disappears together with the whole save — and with it every placed block. Before 26.x (no
 * `Server.getLevelDirectory`, found by reflection) the main world's folder is used instead; there the nether
 * and the end live in their own folders, so deleting only the main world folder loses the registry.
 *
 * Numbers are never reused: a block whose plugin was removed keeps its number, so its already placed blocks
 * don't turn into another block. Numbers start at 1 — variation 0 is the default state of a
 * freshly placed vanilla note block.
 */
internal object EffectiveBlockVariations {
    private const val MAX_VARIATION = 799

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val file: Path by lazy {
        val server = Bukkit.getServer()
        val root = server.javaClass.methods
            .firstOrNull { it.name == "getLevelDirectory" && it.parameterCount == 0 }
            ?.invoke(server) as Path?
            ?: Bukkit.getWorlds().first().worldFolder.toPath()
        root.resolve("data").resolve("effectivespigot").resolve("block_variations.json")
    }

    private val variations: MutableMap<String, Int> by lazy { load() }

    /** The variation of the block [name]: the recorded one, or the smallest free one, assigned and saved. */
    fun resolve(name: String): Int {
        variations[name]?.let { return it }
        val taken = variations.values.toHashSet()
        val free = (1..MAX_VARIATION).firstOrNull { it !in taken }
            ?: error(Locale.getMessage("errors.blocks.variations_exhausted", name))
        variations[name] = free
        save()
        return free
    }

    private fun load(): MutableMap<String, Int> {
        if (!Files.isRegularFile(file)) return hashMapOf()
        val json = JsonParser.parseString(Files.readString(file)).asJsonObject
        return json.entrySet().associateTo(hashMapOf()) { it.key to it.value.asInt }
    }

    private fun save() {
        val json = JsonObject()
        for ((name, variation) in variations.entries.sortedBy { it.value }) json.addProperty(name, variation)
        Files.createDirectories(file.parent)
        val temp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.writeString(temp, gson.toJson(json))
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
