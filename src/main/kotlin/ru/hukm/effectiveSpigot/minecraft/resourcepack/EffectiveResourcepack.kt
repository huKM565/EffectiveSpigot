package ru.hukm.effectiveSpigot.minecraft.resourcepack

import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Config
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.http.EffectiveHttpServer
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EffectiveResourcepack {

    private lateinit var resourcePackRequest: ResourcePackRequest
    private val resourcepacksInfo = arrayListOf<ResourcePackInfo>()
    private var toBuild = arrayListOf<JavaPlugin>()

    private val glyphs = mutableMapOf<JavaPlugin, MutableList<EffectiveGlyph>>()
    private val spaces = mutableMapOf<JavaPlugin, MutableMap<Char, Int>>()

    private fun addToBuild(instance: JavaPlugin) {
        if (instance !in toBuild) toBuild.add(instance)
    }

    fun addGlyph(instance: JavaPlugin, glyph: EffectiveGlyph) {
        if (Config.isResourcepackHttpServerEnabled()) {
            addToBuild(instance)
            glyphs.getOrPut(instance) { mutableListOf() }.add(glyph)
        }
    }

    fun addSpaceProvider(instance: JavaPlugin, advances: Map<Char, Int>) {
        if (Config.isResourcepackHttpServerEnabled()) {
            addToBuild(instance)
            spaces.getOrPut(instance) { mutableMapOf() }.putAll(advances)
        }
    }

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

    private fun tryBuild(instance: JavaPlugin) {
        val jarFile = File(instance.javaClass.protectionDomain.codeSource.location.toURI())
        val jarHash = MessageDigest.getInstance("SHA-1").digest(jarFile.readBytes())
            .take(8).joinToString("") { "%02x".format(it) }
        val packFile = File(instance.dataFolder, "resourcepack-$jarHash.zip")

        instance.dataFolder.listFiles { _, name ->
            name.startsWith("resourcepack-") && name.endsWith(".zip") && name != packFile.name
        }?.forEach { it.delete() }

        if (!packFile.exists()) {
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

            addFont(resourcepackFiles, instance)

            packFile.parentFile?.mkdirs()
            ZipOutputStream(packFile.outputStream().buffered()).use { zip ->
                for ((path, bytes) in resourcepackFiles) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }

        val packBytes = packFile.readBytes()
        val sha1Hex = MessageDigest.getInstance("SHA-1").digest(packBytes)
            .joinToString("") { "%02x".format(it) }

        val safeName = instance.name.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
        val path = "/resourcepack/$safeName"
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
        val namespace = instance.name.lowercase()
        val itemName = effectiveItem.getNamespacedData().second
        val data = effectiveItem.getResourcePackData() ?: return

        resourcepackFiles["assets/$namespace/items/$itemName.json"] = """
            {
              "model": {
                "type": "minecraft:model",
                "model": "$namespace:item/$itemName"
              }
            }
        """.trimIndent().toByteArray()

        resourcepackFiles["assets/$namespace/models/item/$itemName.json"] = if (data.modelPath == null) {
            """
                {
                    "parent": "minecraft:item/generated",
                    "textures": { "layer0": "$namespace:item/$itemName" }
                }
            """.trimIndent().toByteArray()
        } else {
            instance.getResource(data.modelPath)?.use { it.readBytes() }
                ?: error(Locale.getMessage("errors.resourcepack.model_not_found", data.modelPath, instance.name))
        }

        resourcepackFiles["assets/$namespace/textures/item/$itemName.png"] =
            instance.getResource(data.texturePath)?.use { it.readBytes() }
                ?: error(Locale.getMessage("errors.resourcepack.texture_not_found", data.texturePath, instance.name))
    }

    private fun addFont(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin
    ) {
        val pluginGlyphs = glyphs[instance].orEmpty()
        val pluginSpaces = spaces[instance].orEmpty()
        if (pluginGlyphs.isEmpty() && pluginSpaces.isEmpty()) return

        val namespace = instance.name.lowercase()

        val providers = mutableListOf<String>()

        for (g in pluginGlyphs) {
            val bytes = instance.getResource(g.texturePath)?.use { it.readBytes() }
                ?: error(Locale.getMessage("errors.resourcepack.texture_not_found", g.texturePath, instance.name))
            resourcepackFiles["assets/$namespace/textures/${g.texturePath}"] = bytes

            val charEscape = "\\u${"%04X".format(g.char.code)}"
            providers.add("""{ "type": "bitmap", "file": "$namespace:${g.texturePath}", "chars": ["$charEscape"], "height": ${g.height}, "ascent": ${g.ascent} }""")
        }

        if (pluginSpaces.isNotEmpty()) {
            val advances = pluginSpaces.entries.joinToString(", ") { (c, a) ->
                "\"\\u${"%04X".format(c.code)}\": $a"
            }
            providers.add("""{ "type": "space", "advances": { $advances } }""")
        }

        resourcepackFiles["assets/$namespace/font/default.json"] = """
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
                        tryBuild(instance)
                    }
                }
            }
        }
    }

    fun init() {}
}

