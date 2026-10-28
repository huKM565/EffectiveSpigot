package ru.hukm.effectiveSpigot.minecraft.resourcepack

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveModelAnimation
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveModelState

class ItemModelDefinitionTest {
    private fun animated() = EffectiveItem.ResourcePackData(
        modelPath = "models/musket.json",
        modelAnimations = listOf(
            EffectiveModelAnimation("a", EffectiveModelAnimation.frames("models/musket/a", 3), 10),
            EffectiveModelAnimation("b", EffectiveModelAnimation.frames("models/musket/b", 2), 4),
        ),
        modelStates = listOf(EffectiveModelState("cocked", "models/musket/cocked.json")),
    )

    @Test
    fun framesNamesFilesFromOne() {
        assertEquals(listOf("m/s/01.json", "m/s/02.json", "m/s/03.json"), EffectiveModelAnimation.frames("m/s", 3))
    }

    @Test
    fun plainItemKeepsSingleModel() {
        val json = ItemModelDefinition.build("firearms", "stick", EffectiveItem.ResourcePackData(modelPath = "x"))
        assertEquals(JsonParser.parseString("""{"model":{"type":"minecraft:model","model":"firearms:item/stick"}}"""), json)
    }

    @Test
    fun animatedItemTree() {
        val json = ItemModelDefinition.build("firearms", "musket", animated())
        assertEquals(false, json["hand_animation_on_swap"].asBoolean)
        val root = json["model"].asJsonObject
        assertEquals("minecraft:select", root["type"].asString)
        assertEquals(0, root["index"].asInt)
        val cases = root["cases"].asJsonArray.map { it.asJsonObject }
        assertEquals(listOf("a", "b"), cases.map { it["when"].asString })
        val a = cases[0]["model"].asJsonObject
        assertEquals("minecraft:range_dispatch", a["type"].asString)
        assertEquals("minecraft:cooldown", a["property"].asString)
        val entries = a["entries"].asJsonArray.map { it.asJsonObject }
        assertEquals(listOf(2f / 3 + 0.0001f, 1f / 3 + 0.0001f, 0.0001f), entries.map { it["threshold"].asFloat })
        assertEquals(
            listOf("firearms:item/musket/a/01", "firearms:item/musket/a/02", "firearms:item/musket/a/03"),
            entries.map { it["model"].asJsonObject["model"].asString }
        )
        val rest = root["fallback"].asJsonObject
        assertEquals(rest, a["fallback"].asJsonObject)
        assertEquals(1, rest["index"].asInt)
        assertEquals(
            "firearms:item/musket/state/cocked",
            rest["cases"].asJsonArray[0].asJsonObject["model"].asJsonObject["model"].asString
        )
        assertEquals("firearms:item/musket", rest["fallback"].asJsonObject["model"].asString)
    }

    @Test
    fun extraModelsMapJarPathsToPackPaths() {
        assertEquals(
            mapOf(
                "assets/firearms/models/item/musket/state/cocked.json" to "models/musket/cocked.json",
                "assets/firearms/models/item/musket/a/01.json" to "models/musket/a/01.json",
                "assets/firearms/models/item/musket/a/02.json" to "models/musket/a/02.json",
                "assets/firearms/models/item/musket/a/03.json" to "models/musket/a/03.json",
                "assets/firearms/models/item/musket/b/01.json" to "models/musket/b/01.json",
                "assets/firearms/models/item/musket/b/02.json" to "models/musket/b/02.json",
            ),
            ItemModelDefinition.extraModels("firearms", "musket", animated())
        )
    }

    @Test
    fun statesOnlyItemHasRestAsRoot() {
        val data = EffectiveItem.ResourcePackData(modelPath = "x", modelStates = listOf(EffectiveModelState("cocked", "y")))
        val root = ItemModelDefinition.build("firearms", "musket", data)["model"].asJsonObject
        assertEquals(1, root["index"].asInt)
    }
}
