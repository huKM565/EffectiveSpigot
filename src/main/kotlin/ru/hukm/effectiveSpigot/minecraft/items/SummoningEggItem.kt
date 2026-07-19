package ru.hukm.effectiveSpigot.minecraft.items

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgs
import ru.hukm.effectiveSpigot.minecraft.additional.AdditionalArgsSupport
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import kotlin.random.Random
import kotlin.random.nextInt

abstract class SummoningEggItem : EffectiveItem() {
    enum class SpawnPlacement {
        TOP,
        BOTTOM,
        VANILLA,
        CENTER,
        EXACT
    }

    companion object {
        fun resolveSpawnLocation(
            block: Block,
            placement: SpawnPlacement,
            clickedFace: BlockFace? = null
        ): Location {
            val base = block.location
            return when (placement) {
                SpawnPlacement.EXACT  -> base.clone()
                SpawnPlacement.CENTER -> base.clone().add(0.5, 0.5, 0.5)
                SpawnPlacement.TOP    -> base.clone().add(0.5, 1.0, 0.5)
                SpawnPlacement.BOTTOM -> base.clone().add(0.5, -1.0, 0.5)
                SpawnPlacement.VANILLA -> {
                    val face = clickedFace ?: BlockFace.UP
                    val target = if (block.isPassable) block else block.getRelative(face)
                    target.location.add(0.5, 0.0, 0.5)
                }
            }.apply {
                yaw = Random.nextInt(0..360).toFloat()
                pitch = 0f
            }
        }
    }

    init {
        addClickHandler(Click.RIGHT, { e ->
            val block = e.clickedBlock ?: return@addClickHandler EffectiveAbstractInteract.Result.ALLOW_EVENT

            val location = resolveSpawnLocation(block, getSpawnPlacement(), e.blockFace)

            val additionalArgs = AdditionalArgsSupport.readFromHolder(e.item.itemMeta, getAdditionalArgs())

            if (additionalArgs.isEmpty()) {
                getSpawnEffectiveEntity().spawnEntity(location)
            } else {
                getSpawnEffectiveEntity().spawnEntity(location, additionalArgs)
            }

            EffectiveAbstractInteract.Result.CANCEL_EVENT
        })
    }

    abstract fun getSpawnEffectiveEntity(): EffectiveEntity

    open fun getSpawnPlacement(): SpawnPlacement = SpawnPlacement.VANILLA

    override fun getAdditionalArgs(): AdditionalArgs? = getSpawnEffectiveEntity().getAdditionalArgs()

    override fun getMaterial(): Material = Material.PIG_SPAWN_EGG

    override fun editMeta(meta: ItemMeta) {
        meta.displayName(
            Locale.getComponent("items.summoning_egg.name", getSpawnEffectiveEntity().getNamespacedKey())
        )
    }
}
