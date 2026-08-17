package ru.hukm.effectiveSpigot.minecraft.resourcepack

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.Instrument
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Config
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.http.EffectiveHttpServer
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.blocks.EffectiveBlock
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.ImageIO
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Assembles and serves a per-plugin resource pack.
 *
 * Register content ([addGlyph] for bitmap glyphs, [addSpaceProvider] for negative-space advances) and,
 * when EffectiveSpigot's built-in HTTP server is enabled, a pack is generated from the plugin's jar
 * resources, hashed and hosted automatically; otherwise register an external pack with
 * [addServerResourcepack]. All calls are no-ops unless the HTTP server is enabled in config.
 */
object EffectiveResourcepack {
    private lateinit var resourcePackRequest: ResourcePackRequest
    private val resourcepacksInfo = arrayListOf<ResourcePackInfo>()
    private var toBuild = arrayListOf<JavaPlugin>()

    private val glyphs = mutableMapOf<JavaPlugin, MutableList<EffectiveGlyph>>()
    private val spaces = mutableMapOf<JavaPlugin, MutableMap<EffectiveFontChar, Int>>()

    private fun addToBuild(instance: JavaPlugin) {
        if (instance !in toBuild) toBuild.add(instance)
    }

    /** Adds a bitmap [glyph] to [instance]'s generated pack. No-op if the HTTP server is disabled. */
    fun addGlyph(instance: JavaPlugin, glyph: EffectiveGlyph) {
        if (Config.isResourcepackHttpServerEnabled()) {
            addToBuild(instance)
            glyphs.getOrPut(instance) { mutableListOf() }.add(glyph)
        }
    }

    /**
     * Registers negative/positive space [advances] ([EffectiveFontChar] → pixel width) for [instance]'s
     * pack, used to position glyphs precisely. No-op if the HTTP server is disabled.
     */
    fun addSpaceProvider(instance: JavaPlugin, advances: Map<EffectiveFontChar, Int>) {
        if (Config.isResourcepackHttpServerEnabled()) {
            addToBuild(instance)
            spaces.getOrPut(instance) { mutableMapOf() }.putAll(advances)
        }
    }

    /**
     * Registers an externally hosted resource pack by [url] and its [sha1Hex] hash. When the built-in
     * HTTP server is enabled the local generated pack is used instead.
     */
    fun addServerResourcepack(instance: JavaPlugin, url: String, sha1Hex: String) {
        if (Config.isResourcepackHttpServerEnabled()) addToBuild(instance)
        else register(url, sha1Hex)
    }

    private fun register(url: String, sha1Hex: String) {
        val bytes = ByteArray(sha1Hex.length / 2)

        for (index in bytes.indices) {
            bytes[index] = sha1Hex.substring(index * 2..(index * 2 + 1)).toInt(16).toByte()
        }

        val bb = ByteBuffer.wrap(bytes)
        val uuid = UUID(bb.getLong(), bb.getLong())

        resourcepacksInfo.add(
            ResourcePackInfo.resourcePackInfo()
                .id(uuid)
                .uri(URI.create(url))
                .hash(sha1Hex)
                .build()
        )
    }

    /** Pixel width of the image at [texturePath] inside [instance]'s jar resources, or 0 if it can't be read. */
    fun getImageWidth(instance: JavaPlugin, texturePath: String): Int {
        val image = instance.getResource(texturePath)?.use { ImageIO.read(it) } ?: return 0
        return image.width
    }

    private fun tryBuild(instance: JavaPlugin) {
        instance.dataFolder.listFiles { _, name -> name.startsWith("resourcepack-") && name.endsWith(".zip") }
            ?.forEach { it.delete() }

        val resourcepackFiles = mutableMapOf<String, ByteArray>()

        resourcepackFiles["pack.mcmeta"] = """
            {
              "pack": {
                "pack_format": 46,
                "description": "${instance.name} resource pack"
              }
            }
        """.trimIndent().toByteArray()

        EffectiveItem.namespacedKeyToItem.values.filter {
            it.getNamespacedData().first == instance && it.getResourcePackData() != null
        }.forEach {
            addItemModel(resourcepackFiles, instance, it)
        }

        EffectiveBlock.namespacedKeyToBlock.values.filter {
            it.getNamespacedData().first == instance && it.getResourcePackData() != null
        }.forEach {
            addBlockModel(resourcepackFiles, instance, it)
        }

        // Shared noteblock blockstate file — merged across ALL registered EffectiveBlocks in
        // every plugin, not just [instance]. Each per-plugin pack ships an identical copy;
        // whichever loads last wins on the client, but the content is the same.
        if (EffectiveBlock.namespacedKeyToBlock.values.any { it.getResourcePackData() != null }) {
            resourcepackFiles["assets/minecraft/blockstates/note_block.json"] =
                buildMergedNoteBlockStatesJson()
        }

        addFont(resourcepackFiles, instance)

        val packBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                for ((path, bytes) in resourcepackFiles) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            baos.toByteArray()
        }

        val sha1Hex = MessageDigest.getInstance("SHA-1").digest(packBytes)
            .joinToString("") { "%02x".format(it) }

        val path = "/resourcepack/${EffectiveMinecraftUtils.getNamespace(instance)}"
        EffectiveHttpServer.serve(path, packBytes)

        val ip = Config.getResourcepackHttpServerIp()
            .ifBlank { error(Locale.getMessage("errors.resourcepack.ip_not_set")) }
        val url = "http://$ip:${Config.getResourcepackHttpServerPort()}$path"
        register(url, sha1Hex)
    }

    private fun addItemModel(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin,
        effectiveItem: EffectiveItem
    ) {
        val namespace = EffectiveMinecraftUtils.getNamespace(instance)
        val itemName = effectiveItem.getNamespacedData().second
        val data = effectiveItem.getResourcePackData() ?: return

        val textureBytes = instance.getResource(data.texturePath)?.use { it.readBytes() }
        if (textureBytes == null) {
            instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", data.texturePath, instance.name))
            return
        }

        val modelBytes: ByteArray = if (data.modelPath == null) {
            """
                {
                    "parent": "minecraft:item/generated",
                    "textures": { "layer0": "$namespace:item/$itemName" }
                }
            """.trimIndent().toByteArray()
        } else {
            instance.getResource(data.modelPath)?.use { it.readBytes() } ?: run {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.model_not_found", data.modelPath, instance.name))
                return
            }
        }

        resourcepackFiles["assets/$namespace/items/$itemName.json"] = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "$namespace:item/$itemName"
              }
            }
        """.trimIndent().toByteArray()

        resourcepackFiles["assets/$namespace/models/item/$itemName.json"] = modelBytes
        resourcepackFiles["assets/$namespace/textures/item/$itemName.png"] = textureBytes
    }

    private fun addBlockModel(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin,
        effectiveBlock: EffectiveBlock
    ) {
        val namespace = EffectiveMinecraftUtils.getNamespace(instance)
        val blockName = effectiveBlock.getNamespacedData().second
        val data = effectiveBlock.getResourcePackData() ?: return

        // Collect only the distinct texture paths actually referenced — a single-texture block ships
        // one PNG, a six-face block ships up to six.
        val perFace = mapOf(
            "up" to data.effectiveTop,
            "down" to data.effectiveBottom,
            "north" to data.effectiveNorth,
            "south" to data.effectiveSouth,
            "east" to data.effectiveEast,
            "west" to data.effectiveWest,
        )

        for (path in perFace.values.toSet()) {
            val bytes = instance.getResource(path)?.use { it.readBytes() }
            if (bytes == null) {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", path, instance.name))
                return
            }
            val safeName = path.substringAfterLast('/').substringBeforeLast('.')
            resourcepackFiles["assets/$namespace/textures/block/${blockName}_$safeName.png"] = bytes
        }

        // Map each face to the texture ref generated above.
        fun texRef(path: String): String {
            val safeName = path.substringAfterLast('/').substringBeforeLast('.')
            return "$namespace:block/${blockName}_$safeName"
        }

        val modelBytes: ByteArray = """
            {
              "parent": "minecraft:block/cube",
              "textures": {
                "particle": "${texRef(data.effectiveTop)}",
                "up": "${texRef(data.effectiveTop)}",
                "down": "${texRef(data.effectiveBottom)}",
                "north": "${texRef(data.effectiveNorth)}",
                "south": "${texRef(data.effectiveSouth)}",
                "east": "${texRef(data.effectiveEast)}",
                "west": "${texRef(data.effectiveWest)}"
              }
            }
        """.trimIndent().toByteArray()

        resourcepackFiles["assets/$namespace/items/$blockName.json"] = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "$namespace:block/$blockName"
              }
            }
        """.trimIndent().toByteArray()

        resourcepackFiles["assets/$namespace/models/block/$blockName.json"] = modelBytes
    }

    /**
     * Bukkit's [Instrument] enum → the string used in the vanilla `note_block.json` blockstate keys.
     * Only the 16 playable ones ([EffectiveBlock.PLAYABLE_INSTRUMENTS]) are mapped; mob-head
     * instruments can't be set on a noteblock's blockdata so they never appear here.
     */
    private val VANILLA_INSTRUMENT_NAMES: Map<Instrument, String> = mapOf(
        Instrument.PIANO to "harp",
        Instrument.BASS_DRUM to "basedrum",
        Instrument.SNARE_DRUM to "snare",
        Instrument.STICKS to "hat",
        Instrument.BASS_GUITAR to "bass",
        Instrument.FLUTE to "flute",
        Instrument.BELL to "bell",
        Instrument.GUITAR to "guitar",
        Instrument.CHIME to "chime",
        Instrument.XYLOPHONE to "xylophone",
        Instrument.IRON_XYLOPHONE to "iron_xylophone",
        Instrument.COW_BELL to "cow_bell",
        Instrument.DIDGERIDOO to "didgeridoo",
        Instrument.BIT to "bit",
        Instrument.BANJO to "banjo",
        Instrument.PLING to "pling",
    )

    /**
     * Full `note_block.json` blockstate JSON with all 800 variants: registered custom blocks point
     * at their generated cube model, everything else falls back to `minecraft:block/note_block`
     * (so vanilla noteblocks placed by players/generation still render correctly).
     */
    private fun buildMergedNoteBlockStatesJson(): ByteArray {
        val customs = mutableMapOf<Triple<Instrument, Int, Boolean>, String>()
        for (block in EffectiveBlock.namespacedKeyToBlock.values) {
            if (block.getResourcePackData() == null) continue
            val triple = EffectiveBlock.encodeVariation(block.getCustomVariation())
            val ns = EffectiveMinecraftUtils.getNamespace(block.getNamespacedData().first)
            val name = block.getNamespacedData().second
            customs[triple] = "$ns:block/$name"
        }

        val entries = mutableListOf<String>()
        for (instrument in EffectiveBlock.PLAYABLE_INSTRUMENTS) {
            val instrumentName = VANILLA_INSTRUMENT_NAMES[instrument] ?: continue
            for (noteId in 0..24) {
                for (powered in listOf(false, true)) {
                    val model = customs[Triple(instrument, noteId, powered)]
                        ?: "minecraft:block/note_block"
                    entries.add(
                        """    "instrument=$instrumentName,note=$noteId,powered=$powered": { "model": "$model" }"""
                    )
                }
            }
        }

        return """
{
  "variants": {
${entries.joinToString(",\n")}
  }
}
""".trimIndent().toByteArray()
    }

    /** JSON `\uXXXX` escape for a glyph's codepoint — two units (surrogate pair) for supplementary planes. */
    private fun EffectiveFontChar.fontEscape() = string.map { "\\u%04X".format(it.code) }.joinToString("")

    private fun addFont(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin
    ) {
        val pluginGlyphs = glyphs[instance].orEmpty()
        val pluginSpaces = spaces[instance].orEmpty()
        if (pluginGlyphs.isEmpty() && pluginSpaces.isEmpty()) return

        val providers = mutableListOf<String>()

        for (g in pluginGlyphs) {
            val bytes = instance.getResource(g.texturePath)?.use { it.readBytes() }
            if (bytes == null) {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", g.texturePath, instance.name))
                continue
            }

            resourcepackFiles["assets/minecraft/textures/${g.texturePath}"] = bytes

            val charEscape = g.charGlyph.fontEscape()
            providers.add("""{ "type": "bitmap", "file": "minecraft:${g.texturePath}", "chars": ["$charEscape"], "height": ${g.height}, "ascent": ${g.ascent} }""")
        }

        if (pluginSpaces.isNotEmpty()) {
            val advances = pluginSpaces.entries.joinToString(", ") { (c, a) ->
                "\"${c.fontEscape()}\": $a"
            }
            providers.add("""{ "type": "space", "advances": { $advances } }""")
        }

        if (providers.isEmpty()) return

        resourcepackFiles["assets/minecraft/font/default.json"] = """
            {
              "providers": [
                ${providers.joinToString(",\n    ")}
              ]
            }
        """.trimIndent().toByteArray()
    }

    internal fun getModule(): IModule {
        return object : IModule {
            override fun init() {
                event<PlayerJoinEvent> {
                    val player = it.player

                    if (!::resourcePackRequest.isInitialized) {
                        resourcePackRequest = ResourcePackRequest.resourcePackRequest()
                            .packs(resourcepacksInfo)
                            .required(true)
                            .build()
                    }

                    player.sendResourcePacks(
                        resourcePackRequest
                    )
                }

                event<ServerLoadEvent> {
                    if (!Config.isResourcepackHttpServerEnabled()) return@event
                    for (instance in toBuild) {
                        try {
                            tryBuild(instance)
                        } catch (e: Exception) {
                            instance.logger.warning(
                                Locale.getMessage("errors.resourcepack.build_failed", instance.name, e.message ?: e.toString())
                            )
                        }
                    }
                }
            }
        }
    }

    fun init() {}
}

