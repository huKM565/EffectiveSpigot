package ru.hukm.effectiveSpigot.minecraft.blocks.interfaces

import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import ru.hukm.effectiveSpigot.minecraft.blocks.EffectiveBlock
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click

typealias InteractCallback = (EffectiveBlockInteractable.EventsCallOptions) -> EffectiveAbstractInteract.Result

interface EffectiveBlockInteractable {
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Block,
        override val click: Click,
        override val callback: InteractCallback,
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        val block = target.block
        val itemDisplay = target.itemDisplay
    }

    data class EventsCallOptions(
        override val player: Player,
        override val target: EffectiveAbstractInteract.Target.Block,
        override val click: Click,
        override val hand: EquipmentSlot,
    ) : EffectiveAbstractInteract.EventsCallOptions<EffectiveAbstractInteract.Target.Block> {
        val block = target.block
        val itemDisplay = target.itemDisplay
    }

    companion object {
        val interactableBlocks = arrayListOf<Data>()

        fun addInteractHandler(
            block: Block?,
            effectiveBlock: EffectiveBlock?,
            click: Click,
            callback: InteractCallback,
            cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null
        ) {
            interactableBlocks.add(
                Data(
                    if (effectiveBlock == null) {
                        EffectiveAbstractInteract.Target.Block(block!!.type, null, null)
                    } else {
                        EffectiveAbstractInteract.Target.Block(
                            effectiveBlock.item.getMaterial(),
                            null,
                            effectiveBlock.itemDisplay.createEntity(null) as ItemDisplay
                        )
                    },
                    click,
                    callback,
                    cooldownData
                )
            )
        }

        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val block = eventsCallOptions.block
            var result = false

            for (interactableBlock in interactableBlocks) {
                val isEqual = EffectiveBlock.equalByNamespacedKeyIfExistElseByMaterial(
                    EffectiveBlock.getItemDisplayByBlock(block!!) to block,
                    interactableBlock.itemDisplay to interactableBlock.block!!
                )

                if (isEqual) {
                    result = EffectiveAbstractInteract.runCallAndUpdateResult(
                        result,
                        interactableBlock,
                        eventsCallOptions
                    )
                }
            }

            return result
        }
    }
}