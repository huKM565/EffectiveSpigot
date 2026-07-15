package ru.hukm.effectiveSpigot.minecraft.items

import net.md_5.bungee.api.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click
import ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable

abstract class SummoningEggItem : EffectiveItem() {

    enum class SpawnPlacement {
        TOP,     // на верхней грани (сущность стоит на блоке)
        BOTTOM,  // под блоком
        LEFT,    // слева от игрока
        RIGHT,   // справа от игрока
        CENTER,  // в середине самого блока (x+0.5, y+0.5, z+0.5)
        EXACT    // в углу блока — целые координаты
    }

    companion object {

        /** Возвращает точку спавна относительно блока, по которому кликнули. */
        fun resolveSpawnLocation(block: Block, placement: SpawnPlacement, player: Player): Location {
            val base = block.location // угол блока: целые x, y, z
            return when (placement) {
                SpawnPlacement.EXACT  -> base.clone()
                SpawnPlacement.CENTER -> base.clone().add(0.5, 0.5, 0.5)
                SpawnPlacement.TOP    -> base.clone().add(0.5, 1.0, 0.5)
                SpawnPlacement.BOTTOM -> base.clone().add(0.5, -1.0, 0.5)
                SpawnPlacement.LEFT   -> centerOfNeighbor(block, leftOf(horizontalFacing(player)))
                SpawnPlacement.RIGHT  -> centerOfNeighbor(block, rightOf(horizontalFacing(player)))
            }.apply {
                yaw = player.location.yaw   // сущность смотрит туда же, куда игрок
                pitch = 0f
            }
        }

        private fun centerOfNeighbor(block: Block, face: BlockFace): Location =
            block.getRelative(face).location.add(0.5, 0.0, 0.5)

        private fun horizontalFacing(player: Player): BlockFace = when (player.facing) {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST -> player.facing
            else -> BlockFace.NORTH
        }

        private fun leftOf(face: BlockFace): BlockFace = when (face) {
            BlockFace.NORTH -> BlockFace.WEST
            BlockFace.WEST  -> BlockFace.SOUTH
            BlockFace.SOUTH -> BlockFace.EAST
            BlockFace.EAST  -> BlockFace.NORTH
            else -> BlockFace.NORTH
        }

        private fun rightOf(face: BlockFace): BlockFace = leftOf(face).oppositeFace
    }

    init {
        addClickHandler(Click.RIGHT) { handleClick(it) }
    }

    /** Какую кастомную сущность призывает это яйцо. */
    abstract fun getEffectiveEntity(): EffectiveEntity

    /** С какой грани спавнить. По умолчанию — на верхней. */
    open fun getSpawnPlacement(): SpawnPlacement = SpawnPlacement.TOP

    override fun getMaterial(): Material = Material.PIG_SPAWN_EGG

    override fun editMeta(meta: ItemMeta) {
        meta.setDisplayName("${ChatColor.YELLOW}Яйцо призыва ${getEffectiveEntity().getNamespacedKey()}")
    }

    private fun handleClick(options: EffectiveClickable.EventsCallOptions): EffectiveAbstractInteract.Result {
        val block = options.clickedBlock ?: return EffectiveAbstractInteract.Result.ALLOW_EVENT

        val location = resolveSpawnLocation(block, getSpawnPlacement(), options.player)
        getEffectiveEntity().spawnEntity(location)

        // отменяем эвент, чтобы ванильный спавн-яйцо не заспавнил свинью поверх нашей сущности
        return EffectiveAbstractInteract.Result.CANCEL_EVENT
    }
}
