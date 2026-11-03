package ru.hukm.effectiveSpigot.minecraft.world

import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class EffectiveWorldCreatorTest {
    private val lobby = NamespacedKey("eworldprobe", "lobby")

    @Test fun legacyNameJoinsNamespaceAndKey() =
        assertEquals("eworldprobe_game_one", EffectiveWorldCreator.legacyLevelName(NamespacedKey("eworldprobe", "game_one")))

    @Test fun legacyCreatorUsesPrefixedFolder() {
        val creator = EffectiveWorldCreator.creatorFor(lobby, keyedStorage = false)
        assertEquals("eworldprobe_lobby", creator.name())
        assertEquals(lobby, creator.key())
    }

    @Test fun keyedCreatorKeepsKey() =
        assertEquals(lobby, EffectiveWorldCreator.creatorFor(lobby, keyedStorage = true).key())

    @Test fun samePathInTwoPluginsGetsTwoLegacyFolders() = assertNotEquals(
        EffectiveWorldCreator.creatorFor(NamespacedKey("a", "lobby"), false).name(),
        EffectiveWorldCreator.creatorFor(NamespacedKey("b", "lobby"), false).name()
    )
}
