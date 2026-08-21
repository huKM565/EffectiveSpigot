package ru.hukm.effectiveSpigot.minecraft.resourcepack

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.Instrument
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Config
import ru.hukm.effectiveSpigot.EffectiveSpigot
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
            glyphs.getOrPut(instance) { mutableListOf() }.add(glyph)
        }
    }

    /**
     * Registers negative/positive space [advances] ([EffectiveFontChar] → pixel width) for [instance]'s
     * pack, used to position glyphs precisely. No-op if the HTTP server is disabled.
     */
    fun addSpaceProvider(instance: JavaPlugin, advances: Map<EffectiveFontChar, Int>) {
        if (Config.isResourcepackHttpServerEnabled()) {
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
            it.getNamespacedData().first == instance && it.getResourcePackData()?.isEmpty == false
        }.forEach {
            addItemModel(resourcepackFiles, instance, it)
        }

        addBlocks(resourcepackFiles, instance)

        addFont(resourcepackFiles, instance)

        if (instance === EffectiveSpigot.instance) {
            resourcepackFiles["assets/minecraft/sounds.json"] = """
                {
                  "block.wood.place": { "replace": true, "sounds": [] },
                  "block.wood.break": { "replace": true, "sounds": [] },
                  "required.wood.place": {
                    "sounds": ["dig/wood1", "dig/wood2", "dig/wood3", "dig/wood4"]
                  },
                  "required.wood.break": {
                    "sounds": ["dig/wood1", "dig/wood2", "dig/wood3", "dig/wood4"]
                  }
                }
            """.trimIndent().toByteArray()
        }

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

        val textureBytes = data.texturePath?.let { path ->
            instance.getResource(path)?.use { it.readBytes() } ?: run {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", path, instance.name))
                return
            }
        }

        val modelBytes: ByteArray = when {
            data.modelJson != null -> data.modelJson.toByteArray()
            data.modelPath != null -> instance.getResource(data.modelPath)?.use { it.readBytes() } ?: run {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.model_not_found", data.modelPath, instance.name))
                return
            }
            else -> """
                {
                    "parent": "minecraft:item/generated",
                    "textures": { "layer0": "$namespace:item/$itemName" }
                }
            """.trimIndent().toByteArray()
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
        if (textureBytes != null) resourcepackFiles["assets/$namespace/textures/item/$itemName.png"] = textureBytes
    }

    /** JSON `\uXXXX` escape for a glyph's codepoint — two units (surrogate pair) for supplementary planes. */
    private fun EffectiveFontChar.fontEscape() = string.map { "\\u%04X".format(it.code) }.joinToString("")

    /**
     * Generates everything for custom blocks:
     * - for each of [instance]'s blocks: its six face textures (+ particle) under
     *   `assets/<namespace>/textures/block/<name>_<face>.png` and a `minecraft:block/cube` model;
     * - the shared `assets/minecraft/blockstates/note_block.json` binding **every registered** block's
     *   note-block state (`instrument`/`note`/`powered`, from [EffectiveBlock.getNoteBlockData]) to its
     *   model, with `""` falling back to the vanilla note block.
     *
     * The blockstate file is built from the global registry and written into every pack — blockstates
     * don't merge across stacked packs, so each pack carries the complete, identical mapping and whichever
     * wins still has every block. Missing source textures are logged and skipped.
     */
    private fun addBlocks(resourcepackFiles: MutableMap<String, ByteArray>, instance: JavaPlugin) {
        val namespace = EffectiveMinecraftUtils.getNamespace(instance)

        for (block in EffectiveBlock.namespacedKeyToBlock.values.filter { it.getNamespacedData().first == instance }) {
            val blockName = block.getNamespacedData().second
            val data = block.getResourcePackData()

            val faces = mapOf(
                "up" to data.upTexture,
                "down" to data.downTexture,
                "north" to data.northTexture,
                "south" to data.southTexture,
                "east" to data.eastTexture,
                "west" to data.westTexture,
                "particle" to data.particleTexture
            )

            for ((face, texturePath) in faces) {
                val bytes = instance.getResource(texturePath)?.use { it.readBytes() }
                if (bytes == null) {
                    instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", texturePath, instance.name))
                    continue
                }
                resourcepackFiles["assets/$namespace/textures/block/${blockName}_$face.png"] = bytes
            }

            resourcepackFiles["assets/$namespace/models/block/$blockName.json"] = """
                {
                  "parent": "minecraft:block/cube",
                  "textures": {
                    "up": "$namespace:block/${blockName}_up",
                    "down": "$namespace:block/${blockName}_down",
                    "north": "$namespace:block/${blockName}_north",
                    "south": "$namespace:block/${blockName}_south",
                    "east": "$namespace:block/${blockName}_east",
                    "west": "$namespace:block/${blockName}_west",
                    "particle": "$namespace:block/${blockName}_particle"
                  }
                }
            """.trimIndent().toByteArray()
        }

        val allBlocks = EffectiveBlock.namespacedKeyToBlock.values
        if (allBlocks.isEmpty()) return

        val variants = buildString {
            append("""    "": { "model": "minecraft:block/note_block" }""")
            for (block in allBlocks) {
                val ns = EffectiveMinecraftUtils.getNamespace(block.getNamespacedData().first)
                val blockName = block.getNamespacedData().second
                val stateKey = block.getNoteBlockData().asString.substringAfter('[').substringBefore(']')
                append(",\n    \"$stateKey\": { \"model\": \"$ns:block/$blockName\" }")
            }
        }

        resourcepackFiles["assets/minecraft/blockstates/note_block.json"] =
            "{\n  \"variants\": {\n$variants\n  }\n}".toByteArray()
    }

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

