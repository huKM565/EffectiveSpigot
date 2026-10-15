package ru.hukm.effectiveSpigot.minecraft.posteffect

import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils

/**
 * A full-screen post-processing effect (a fragment shader run over the whole frame after the world is drawn
 * and before the GUI), shown per player. Use it for screen-wide tints, vignettes, blur and the like; images or
 * text placed at a screen position belong to the text core shader instead.
 *
 * You write only the fragment shader, a `.fsh` file inside the plugin jar at [fragmentShaderPath]. When the
 * plugin's resource pack is built (`EffectiveResourcepack.addServerResourcepack` in `onEnable`, after the
 * effects are constructed), the framework adds it as `assets/<namespace>/shaders/post/<name>.fsh` together with
 * the `assets/<namespace>/post_effect/<name>.json` pipeline: your shader draws the frame into a temporary target,
 * which is then blitted back — a pass can't read and write the same target.
 *
 * The shader is a Minecraft 26.x post shader: `#version 330`, the frame in `uniform sampler2D InSampler`, the
 * position in `layout(location = 0) in vec2 texCoord` (0..1), the result in `layout(location = 0) out vec4
 * fragColor`. `#include <minecraft:globals.glsl>` (26.x name; older versions used `#moj_import`) gives the `Globals` block with `GameTime` (0..1 over a
 * 20-minute day, i.e. `GameTime * 1200.0` seconds) for animation, and `ScreenSize`. The server can't pass values
 * into the shader, so different strengths are separate effects.
 *
 * Per-player post effects exist only since Minecraft 26.3: constructing an effect on an older server throws
 * [IllegalStateException] (check [isSupported] first in a plugin that also runs on older versions). Paper exposes
 * them through `Player#postEffects()` since 26.3, but the framework compiles against 1.21.4, so the calls go
 * through reflection proxies ([PostEffectProxies]). Several effects on one player stack in the order they were applied.
 *
 * ```kotlin
 * val RageVignette = EffectivePostEffect(plugin, "rage_vignette", "shaders/rage_vignette.fsh")
 * RageVignette.apply(player)
 * RageVignette.remove(player)
 * ```
 *
 * @property plugin the plugin whose resource pack carries the effect
 * @property name effect id inside the plugin's namespace (lowercase `a-z0-9_.-`)
 * @property fragmentShaderPath path of the `.fsh` inside the plugin jar
 */
class EffectivePostEffect(val plugin: JavaPlugin, val name: String, val fragmentShaderPath: String) {

    /** Full id `<namespace>:<name>` the client resolves in the resource pack. */
    val id: String = "${EffectiveMinecraftUtils.getNamespace(plugin)}:$name"

    private val key: Key by lazy { Key.key(EffectiveMinecraftUtils.getNamespace(plugin), name) }

    init {
        check(isSupported) { Locale.getMessage("errors.post_effects.unsupported_version", Bukkit.getMinecraftVersion()) }
        require(name.matches(Regex("[a-z0-9_.-]+"))) { "Post effect name must match [a-z0-9_.-]+: $name" }
        require(!registry.containsKey(id)) { Locale.getMessage("errors.post_effects.already_registered", id) }
        registry[id] = this
    }

    /**
     * Turns the effect on for [player]. Idempotent and cheap enough to call every tick: nothing is sent when the
     * player already has it, and it is re-added if the server dropped it (e.g. after respawn). No-op on servers
     * older than 26.3.
     */
    fun apply(player: Player) {
        val getter = PostEffectProxies.playerPostEffects ?: return
        val proxy = PostEffectProxies.postEffects ?: return
        val effects = getter.postEffects(player)
        if (!proxy.values(effects).contains(key)) proxy.add(effects, key)
    }

    /** Turns the effect off for [player]; false if it wasn't on or the server is older than 26.3. */
    fun remove(player: Player): Boolean {
        val getter = PostEffectProxies.playerPostEffects ?: return false
        val proxy = PostEffectProxies.postEffects ?: return false
        return proxy.remove(getter.postEffects(player), key)
    }

    /** Whether [player] currently has this effect on. */
    fun isApplied(player: Player): Boolean {
        val getter = PostEffectProxies.playerPostEffects ?: return false
        val proxy = PostEffectProxies.postEffects ?: return false
        return proxy.values(getter.postEffects(player)).contains(key)
    }

    companion object {
        private val registry = linkedMapOf<String, EffectivePostEffect>()

        /** Whether the running server can show per-player post effects (Minecraft 26.3+). */
        @JvmStatic
        val isSupported: Boolean get() = EffectiveMinecraftUtils.isVersionAtLeast(26, 3)

        internal fun ofPlugin(plugin: JavaPlugin): List<EffectivePostEffect> = registry.values.filter { it.plugin === plugin }
    }
}
