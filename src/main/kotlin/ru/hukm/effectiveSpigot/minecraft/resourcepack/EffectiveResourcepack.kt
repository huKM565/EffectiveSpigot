package ru.hukm.effectiveSpigot.minecraft.resourcepack

import com.google.gson.JsonParser
import org.bukkit.Bukkit
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.audience.Audience
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
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
import ru.hukm.effectiveSpigot.minecraft.posteffect.EffectivePostEffect
import ru.hukm.effectiveSpigot.minecraft.screen.EffectiveScreenTextFont
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
import ru.hukm.effectiveSpigot.utils.EffectiveUtils
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.imageio.ImageIO
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.logging.Level

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

    private const val PACK_LOAD_TIMEOUT_TICKS = 20 * 60

    /**
     * Pack ids each joined player has successfully loaded so far; a player leaves the map once all packs are loaded.
     *
     * The packs are required: a player is kicked when the client declines a pack or fails to download or apply it, and
     * also when not all packs are loaded within [PACK_LOAD_TIMEOUT_TICKS] of joining. `required(true)` alone does not
     * enforce anything on the server: it only makes the client show «Proceed / Disconnect», and a client that never
     * shows that prompt sends no answer at all, so the timeout is what catches it. Paper's own kick works only for the
     * `server.properties` pack (`require-resource-pack`).
     */
    private val pendingPacks = HashMap<UUID, MutableSet<UUID>>()

    private fun awaitPacks(player: Player) {
        val playerId = player.uniqueId
        val loaded = HashSet<UUID>()
        pendingPacks[playerId] = loaded
        EffectiveSpigot.instance.launch {
            delay(PACK_LOAD_TIMEOUT_TICKS.ticks)
            if (pendingPacks[playerId] !== loaded) return@launch
            pendingPacks.remove(playerId)
            Bukkit.getPlayer(playerId)?.kick(Locale.getComponent("errors.resourcepack.kick_timeout"))
        }
    }

    private fun onPackStatus(packId: UUID, status: ResourcePackStatus, audience: Audience) {
        val player = audience as? Player ?: return
        val loaded = pendingPacks[player.uniqueId] ?: return
        when (status) {
            ResourcePackStatus.SUCCESSFULLY_LOADED -> {
                loaded.add(packId)
                if (loaded.size >= resourcepacksInfo.size) pendingPacks.remove(player.uniqueId)
            }
            ResourcePackStatus.DECLINED,
            ResourcePackStatus.INVALID_URL,
            ResourcePackStatus.FAILED_DOWNLOAD,
            ResourcePackStatus.FAILED_RELOAD -> {
                pendingPacks.remove(player.uniqueId)
                player.kick(Locale.getComponent("errors.resourcepack.kick_failed"))
            }
            else -> {}
        }
    }

    /**
     * Logs a failed pack build of [instance] with its stack trace. [LinkageError]s are caught too: a plugin compiled
     * against an older framework (e.g. a changed `ResourcePackData` constructor) must not stop the other plugins' packs.
     */
    private fun logBuildFailure(instance: JavaPlugin, e: Throwable) {
        instance.logger.log(Level.WARNING, Locale.getMessage("errors.resourcepack.build_failed", instance.name, e.message ?: e.toString()), e)
    }

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
        else register(url, sha1Hex, UUID.nameUUIDFromBytes("url:$url".toByteArray()))
    }

    /**
     * Registers a pack to send on join under a **stable** [id] (per plugin for built packs, per URL for external
     * ones). The client keys server packs by id: with a stable one a changed pack replaces the previous version,
     * while an id derived from the content hash made every edit a new pack and the stale one kept overriding it.
     */
    private fun register(url: String, sha1Hex: String, id: UUID) {
        resourcepacksInfo.add(
            ResourcePackInfo.resourcePackInfo()
                .id(id)
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

    /**
     * Resource pack format of the running server as (major, minor), read from the server jar's `version.json`
     * (`pack_version.resource_major/resource_minor`, or the legacy single `resource` number). Falls back to 46.
     */
    private fun serverPackFormat(): Pair<Int, Int> {
        return try {
            val json = Bukkit::class.java.classLoader.getResourceAsStream("version.json")
                ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return 46 to 0
            val pack = com.google.gson.JsonParser.parseString(json).asJsonObject.getAsJsonObject("pack_version")
            when {
                pack.has("resource_major") -> pack["resource_major"].asInt to (pack["resource_minor"]?.asInt ?: 0)
                pack.has("resource") -> pack["resource"].asInt to 0
                else -> 46 to 0
            }
        } catch (_: Exception) {
            46 to 0
        }
    }

    private fun tryBuild(instance: JavaPlugin) {
        instance.dataFolder.listFiles { _, name -> name.startsWith("resourcepack-") && name.endsWith(".zip") }
            ?.forEach { it.delete() }

        val resourcepackFiles = mutableMapOf<String, ByteArray>()

        val (major, minor) = serverPackFormat()
        resourcepackFiles["pack.mcmeta"] = """
            {
              "pack": {
                "pack_format": $major,
                "min_format": [$major, 0],
                "max_format": [$major, $minor],
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

        addPostEffects(resourcepackFiles, instance)

        if (instance === EffectiveSpigot.instance) {
            resourcepackFiles["assets/minecraft/sounds.json"] = """
                {
                  "block.wood.place": { "replace": true, "sounds": [] },
                  "block.wood.break": { "replace": true, "sounds": [] },
                  "block.wood.step": { "replace": true, "sounds": [] },
                  "required.wood.place": {
                    "sounds": ["dig/wood1", "dig/wood2", "dig/wood3", "dig/wood4"]
                  },
                  "required.wood.break": {
                    "sounds": ["dig/wood1", "dig/wood2", "dig/wood3", "dig/wood4"]
                  },
                  "required.wood.step": {
                    "sounds": ["step/wood1", "step/wood2", "step/wood3", "step/wood4", "step/wood5", "step/wood6"]
                  },
                  "block.note_block.banjo": { "replace": true, "sounds": [] },
                  "block.note_block.basedrum": { "replace": true, "sounds": [] },
                  "block.note_block.bass": { "replace": true, "sounds": [] },
                  "block.note_block.bell": { "replace": true, "sounds": [] },
                  "block.note_block.bit": { "replace": true, "sounds": [] },
                  "block.note_block.chime": { "replace": true, "sounds": [] },
                  "block.note_block.cow_bell": { "replace": true, "sounds": [] },
                  "block.note_block.didgeridoo": { "replace": true, "sounds": [] },
                  "block.note_block.flute": { "replace": true, "sounds": [] },
                  "block.note_block.guitar": { "replace": true, "sounds": [] },
                  "block.note_block.harp": { "replace": true, "sounds": [] },
                  "block.note_block.hat": { "replace": true, "sounds": [] },
                  "block.note_block.iron_xylophone": { "replace": true, "sounds": [] },
                  "block.note_block.pling": { "replace": true, "sounds": [] },
                  "block.note_block.snare": { "replace": true, "sounds": [] },
                  "block.note_block.xylophone": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_creeper": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_ender_dragon": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_piglin": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_skeleton": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_wither_skeleton": { "replace": true, "sounds": [] },
                  "block.note_block.imitate_zombie": { "replace": true, "sounds": [] }
                }
            """.trimIndent().toByteArray()

            resourcepackFiles["assets/minecraft/textures/particle/note.png"] = EffectiveUtils.transparentPng(8)

            for (sprite in listOf("green_background", "green_progress")) {
                resourcepackFiles["assets/minecraft/textures/gui/sprites/boss_bar/$sprite.png"] = EffectiveUtils.transparentPng(16)
            }

            for (sprite in listOf("warning_button", "warning_button_disabled", "warning_button_highlighted")) {
                resourcepackFiles["assets/minecraft/textures/gui/sprites/dialog/$sprite.png"] = EffectiveUtils.transparentPng(20)
            }

            resourcepackFiles["assets/minecraft/textures/gui/inworld_menu_background.png"] = EffectiveUtils.transparentPng(32)
            resourcepackFiles["assets/minecraft/post_effect/blur.json"] = menuBlurChain()
            resourcepackFiles["assets/effectivespigot/shaders/post/menu_blur.fsh"] =
                instance.getResource("shaders/post/menu_blur.fsh")!!.use { it.readBytes() }

            resourcepackFiles["assets/minecraft/shaders/core/text.vsh"] =
                instance.getResource("shaders/core/text.vsh")!!.use { it.readBytes() }
            EffectiveScreenTextFont.addTo(resourcepackFiles)
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
        register(url, sha1Hex, UUID.nameUUIDFromBytes("effectivespigot:${EffectiveMinecraftUtils.getNamespace(instance)}".toByteArray()))
    }

    private fun addItemModel(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin,
        effectiveItem: EffectiveItem
    ) {
        val namespace = EffectiveMinecraftUtils.getNamespace(instance)
        val itemName = effectiveItem.getNamespacedData().second
        val data = effectiveItem.getResourcePackData() ?: return

        val textureBytes = data.textureBytes ?: data.texturePath?.let { path ->
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
        if (textureBytes != null) {
            resourcepackFiles["assets/$namespace/textures/item/$itemName.png"] = textureBytes
            data.animation?.let { resourcepackFiles["assets/$namespace/textures/item/$itemName.png.mcmeta"] = it.toMcmeta().toByteArray() }
        }
        addModelTextures(resourcepackFiles, instance, namespace, modelBytes)
    }

    /**
     * Copies the textures a custom item model references in [namespace] (`"$namespace:item/musket/barrel"`) from
     * [instance]'s jar (`textures/item/musket/barrel.png`) into the pack. References to other namespaces, such as
     * vanilla textures, and to texture variables (`#side`) are left as they are. Missing files are logged and skipped.
     */
    private fun addModelTextures(
        resourcepackFiles: MutableMap<String, ByteArray>,
        instance: JavaPlugin,
        namespace: String,
        modelBytes: ByteArray
    ) {
        val textures = runCatching {
            JsonParser.parseString(modelBytes.toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("textures")
        }.getOrNull() ?: return

        for ((_, value) in textures.entrySet()) {
            val reference = value.asString
            if (!reference.startsWith("$namespace:")) continue
            val path = reference.substringAfter(':')
            val target = "assets/$namespace/textures/$path.png"
            if (target in resourcepackFiles) continue
            val bytes = instance.getResource("textures/$path.png")?.use { it.readBytes() } ?: run {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.texture_not_found", "textures/$path.png", instance.name))
                continue
            }
            resourcepackFiles[target] = bytes
        }
    }

    /**
     * Adds [instance]'s [EffectivePostEffect]s: each fragment shader as `assets/<namespace>/shaders/post/<name>.fsh`
     * and a `post_effect/<name>.json` pipeline running it from `minecraft:main` into a `swap` target and blitting
     * `swap` back into `minecraft:main`. Missing shader files are logged and skipped.
     */
    private fun addPostEffects(resourcepackFiles: MutableMap<String, ByteArray>, instance: JavaPlugin) {
        val namespace = EffectiveMinecraftUtils.getNamespace(instance)

        for (effect in EffectivePostEffect.ofPlugin(instance)) {
            val shader = instance.getResource(effect.fragmentShaderPath)?.use { it.readBytes() } ?: run {
                instance.logger.warning(Locale.getMessage("errors.resourcepack.shader_not_found", effect.fragmentShaderPath, instance.name))
                continue
            }

            resourcepackFiles["assets/$namespace/shaders/post/${effect.name}.fsh"] = shader
            resourcepackFiles["assets/$namespace/post_effect/${effect.name}.json"] = """
                {
                  "targets": { "swap": {} },
                  "passes": [
                    {
                      "vertex_shader": "minecraft:core/screenquad",
                      "fragment_shader": "$namespace:post/${effect.name}",
                      "inputs": [ { "sampler_name": "In", "target": "minecraft:main" } ],
                      "output": "swap"
                    },
                    {
                      "vertex_shader": "minecraft:core/screenquad",
                      "fragment_shader": "minecraft:post/blit",
                      "inputs": [ { "sampler_name": "In", "target": "swap" } ],
                      "uniforms": {
                        "BlitConfig": [ { "name": "ColorModulate", "type": "vec4", "value": [1.0, 1.0, 1.0, 1.0] } ]
                      },
                      "output": "minecraft:main"
                    }
                  ]
                }
            """.trimIndent().toByteArray()
        }
    }

    /** JSON `\uXXXX` escape for a glyph's codepoint — two units (surrogate pair) for supplementary planes. */
    private fun EffectiveFontChar.fontEscape() = string.map { "\\u%04X".format(it.code) }.joinToString("")

    /** `.png.mcmeta` content for a texture with this animation. */
    private fun EffectiveTextureAnimation.toMcmeta(): String {
        val framesJson = frames?.let { """, "frames": [${it.joinToString(", ")}]""" } ?: ""
        return """{ "animation": { "frametime": $frameTime, "interpolate": $interpolate$framesJson } }"""
    }

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
                data.animation?.let { resourcepackFiles["assets/$namespace/textures/block/${blockName}_$face.png.mcmeta"] = it.toMcmeta().toByteArray() }
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

    /**
     * The menu blur chain of the framework pack: the vanilla `minecraft:blur` passes (three horizontal and vertical box
     * blur pairs) through `effectivespigot:post/menu_blur`, whose last pass also darkens like the vanilla
     * `inworld_menu_background.png` (black at alpha 64/255), unless the dialog flag is on the screen
     * (see `EffectiveDialogBackground`).
     */
    private fun menuBlurChain(): ByteArray {
        val vanillaDarkness = 64f / 255f
        val passes = (0 until 6).joinToString(",\n") { index ->
            val horizontal = index % 2 == 0
            val input = if (horizontal) "minecraft:main" else "swap"
            val output = if (horizontal) "swap" else "minecraft:main"
            val direction = if (horizontal) "1.0, 0.0" else "0.0, 1.0"
            val darkness = if (index == 5) vanillaDarkness else 0f
            """
                {
                  "vertex_shader": "minecraft:core/screenquad",
                  "fragment_shader": "effectivespigot:post/menu_blur",
                  "inputs": [ { "sampler_name": "In", "target": "$input", "bilinear": true } ],
                  "output": "$output",
                  "uniforms": {
                    "MenuBlurConfig": [
                      { "name": "BlurDir", "type": "vec2", "value": [ $direction ] },
                      { "name": "Radius", "type": "float", "value": 0.0 },
                      { "name": "Darkness", "type": "float", "value": $darkness }
                    ]
                  }
                }
            """.trimIndent()
        }
        return "{ \"targets\": { \"swap\": {} }, \"passes\": [\n$passes\n] }".toByteArray()
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
            val bytes = g.textureBytes ?: instance.getResource(g.texturePath)?.use { it.readBytes() }
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
                            .callback { packId, status, audience -> onPackStatus(packId, status, audience) }
                            .build()
                    }

                    player.sendResourcePacks(
                        resourcePackRequest
                    )

                    if (resourcepacksInfo.isNotEmpty()) awaitPacks(player)
                }

                event<PlayerQuitEvent> { pendingPacks.remove(it.player.uniqueId) }

                event<ServerLoadEvent> {
                    if (!Config.isResourcepackHttpServerEnabled()) return@event
                    for (instance in toBuild) {
                        try {
                            tryBuild(instance)
                        } catch (e: Exception) {
                            logBuildFailure(instance, e)
                        } catch (e: LinkageError) {
                            logBuildFailure(instance, e)
                        }
                    }
                }
            }
        }
    }

    fun init() {}
}

