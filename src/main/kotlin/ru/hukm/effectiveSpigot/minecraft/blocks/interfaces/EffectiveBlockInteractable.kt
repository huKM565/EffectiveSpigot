package ru.hukm.effectiveSpigot.minecraft.blocks.interfaces

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.blocks.EffectiveBlock
import ru.hukm.effectiveSpigot.minecraft.events.event
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract
import ru.hukm.effectiveSpigot.minecraft.interfaces.EffectiveAbstractInteract.Click

/** Callback for a block interact handler; its returned [EffectiveAbstractInteract.Result] cancels or allows the event. */
typealias InteractCallback = (EffectiveBlockInteractable.EventsCallOptions) -> EffectiveAbstractInteract.Result

/**
 * Behaviour that dispatches left/right interactions on custom blocks, mirroring
 * [ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable] for items and
 * [ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable] for entities.
 *
 * `RIGHT` = right-click on the block, `LEFT` = left-click (attack) it. Blocks are matched by their
 * custom-block type (note-block state) via [EffectiveBlock.getEffectiveBlock]. Register through
 * [EffectiveBlock.addInteractHandler] on the block instance.
 *
 * Callback result:
 * - [EffectiveAbstractInteract.Result.CANCEL_EVENT] cancels the underlying [PlayerInteractEvent].
 * - [EffectiveAbstractInteract.Result.ALLOW_EVENT] lets the vanilla interaction proceed.
 */
interface EffectiveBlockInteractable {
    /** A registered block interact handler: target block type, [Click] and callback. Blocks have no cooldown. */
    data class Data(
        override val target: EffectiveAbstractInteract.Target.Block,
        override val click: Click,
        override val callback: InteractCallback,
    ) : EffectiveAbstractInteract.Data<EventsCallOptions> {
        override val cooldownData: EffectiveAbstractInteract.CooldownData<EventsCallOptions>? = null
        val effectiveBlock = target.effectiveBlock
    }

    /** Context passed to an [InteractCallback]: the player, the clicked block, the click type, hand and face. */
    data class EventsCallOptions(
        override val player: Player,
        override val target: EffectiveAbstractInteract.Target.Block,
        override val click: Click,
        override val hand: EquipmentSlot,
        val blockFace: BlockFace,
    ) : EffectiveAbstractInteract.EventsCallOptions<EffectiveAbstractInteract.Target.Block> {
        val effectiveBlock = target.effectiveBlock
        val clickedBlock: Block = target.block!!
    }

    companion object {
        val interactableBlocks = arrayListOf<Data>()

        internal fun getModule(): IModule = object : IModule {
            override fun init() {
                event<PlayerInteractEvent> {
                    val clicked = it.clickedBlock ?: return@event
                    val effectiveBlock = EffectiveBlock.getEffectiveBlock(clicked) ?: return@event
                    val isRight = when (it.action) {
                        Action.RIGHT_CLICK_BLOCK -> true
                        Action.LEFT_CLICK_BLOCK -> false
                        else -> return@event
                    }

                    val cancel = tryCall(
                        EventsCallOptions(
                            it.player,
                            EffectiveAbstractInteract.Target.Block(effectiveBlock, clicked),
                            EffectiveAbstractInteract.resolveClick(it.player, isRight),
                            it.hand ?: EquipmentSlot.HAND,
                            it.blockFace
                        )
                    )
                    if (cancel) it.isCancelled = true
                }
            }
        }

        /** Registers an interact handler matching blocks of [effectiveBlock]'s custom type. */
        fun addInteractHandler(
            effectiveBlock: EffectiveBlock,
            click: Click,
            callback: InteractCallback
        ) {
            interactableBlocks.add(
                Data(
                    EffectiveAbstractInteract.Target.Block(effectiveBlock),
                    click,
                    callback
                )
            )
        }

        /** Runs matching handlers for the interacted block; returns whether the event should be cancelled. */
        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val effectiveBlock = eventsCallOptions.effectiveBlock

            var result = false

            for (interactableBlock in interactableBlocks) {
                if (interactableBlock.effectiveBlock === effectiveBlock) {
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
