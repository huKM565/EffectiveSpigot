package ru.hukm.effectiveSpigot.minecraft.resourcepack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveModelAnimation

internal object ItemModelDefinition {
    fun build(namespace: String, itemName: String, data: EffectiveItem.ResourcePackData): JsonObject {
        val base = model("$namespace:item/$itemName")
        val rest = if (data.modelStates.isEmpty()) base else select(
            1,
            data.modelStates.associate { it.name to model("$namespace:item/$itemName/state/${it.name}") },
            base
        )
        val root = if (data.modelAnimations.isEmpty()) rest else select(
            0,
            data.modelAnimations.associate { it.name to frames("$namespace:item/$itemName", it, rest) },
            rest
        )
        return JsonObject().apply {
            add("model", root)
            if (data.isAnimated) addProperty("hand_animation_on_swap", false)
        }
    }

    fun extraModels(namespace: String, itemName: String, data: EffectiveItem.ResourcePackData): Map<String, String> {
        val prefix = "assets/$namespace/models/item/$itemName"
        val states = data.modelStates.map { "$prefix/state/${it.name}.json" to it.model }
        val frames = data.modelAnimations.flatMap { animation ->
            animation.frames.mapIndexed { i, path -> "$prefix/${animation.name}/%02d.json".format(i + 1) to path }
        }
        return (states + frames).toMap()
    }

    private fun model(name: String) = JsonObject().apply {
        addProperty("type", "minecraft:model")
        addProperty("model", name)
    }

    private fun select(index: Int, cases: Map<String, JsonObject>, fallback: JsonObject) = JsonObject().apply {
        addProperty("type", "minecraft:select")
        addProperty("property", "minecraft:custom_model_data")
        addProperty("index", index)
        add("cases", JsonArray().apply {
            cases.forEach { (value, model) ->
                add(JsonObject().apply {
                    addProperty("when", value)
                    add("model", model)
                })
            }
        })
        add("fallback", fallback)
    }

    private fun frames(prefix: String, animation: EffectiveModelAnimation, rest: JsonObject) = JsonObject().apply {
        val n = animation.frames.size
        addProperty("type", "minecraft:range_dispatch")
        addProperty("property", "minecraft:cooldown")
        add("entries", JsonArray().apply {
            for (k in 1..n) {
                add(JsonObject().apply {
                    addProperty("threshold", (n - k).toFloat() / n + 0.0001f)
                    add("model", model("$prefix/${animation.name}/%02d".format(k)))
                })
            }
        })
        add("fallback", rest)
    }
}
