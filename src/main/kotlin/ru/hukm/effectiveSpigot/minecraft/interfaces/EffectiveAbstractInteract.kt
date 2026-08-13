package ru.hukm.effectiveSpigot.minecraft.interfaces

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils


/**
 * Shared vocabulary for click/interact behaviours on items, entities and blocks (used by
 * [ru.hukm.effectiveSpigot.minecraft.items.interfaces.EffectiveClickable] and
 * [ru.hukm.effectiveSpigot.minecraft.entities.interfaces.EffectiveEntityInteractable]).
 *
 * Defines the [Click] type, the callback [Result], the interaction [Target], the call context
 * [EventsCallOptions] and reusable [CooldownData] with per-player / per-instance / all-instances scope.
 *
 * @suppress
 */
interface EffectiveAbstractInteract {
    /** Whether a handler cancels the underlying Bukkit event or lets it proceed. */
    enum class Result { CANCEL_EVENT, ALLOW_EVENT }

    /**
     * The interaction button plus an optional sneak modifier.
     *
     * Base [LEFT]/[RIGHT] fire on that button **regardless** of sneaking (backwards compatible).
     * [LEFT_SHIFT]/[RIGHT_SHIFT] fire only while sneaking; [LEFT_PLAIN]/[RIGHT_PLAIN] only while not
     * sneaking. So a sneaking left-click triggers both [LEFT] and [LEFT_SHIFT] handlers, and a
     * non-sneaking one triggers both [LEFT] and [LEFT_PLAIN].
     */
    enum class Click { LEFT, RIGHT, LEFT_SHIFT, RIGHT_SHIFT, LEFT_PLAIN, RIGHT_PLAIN }

    /** Scope a cooldown applies to: the acting player, this exact instance, or all instances of the item/entity. */
    enum class CooldownType { ON_CURRENT_PLAYER, ON_THIS_INSTANCE, ON_ALL_INSTANCES }

    /** What was interacted with: an item, an entity, or a block (optionally backed by an item display). */
    sealed class Target {
        data class Item(val itemStack: ItemStack) : Target()
        data class Entity(val entity: org.bukkit.entity.Entity) : Target()
        data class Block(val material: Material, val block: org.bukkit.block.Block?, val itemDisplay: ItemDisplay?) : Target()
    }

    /** Context shared by all interaction callbacks: who, which click, on what target, with which hand. */
    interface EventsCallOptions<out T : Target> {
        val player: Player
        val click: Click
        val target: T
        val hand: EquipmentSlot
    }

    /**
     * Cooldown configuration for a handler.
     * @property cooldownToUseInTicks cooldown length in ticks (≤0 disables it)
     * @property conditionForSkipCooldown optional predicate to bypass the cooldown for a given call
     * @property cooldownType scope the cooldown is tracked against
     */
    data class CooldownData<T : EventsCallOptions<out Target>>(
        val cooldownToUseInTicks: Int = 0,
        val conditionForSkipCooldown: ((T) -> Boolean)? = null,
        val cooldownType: CooldownType? = null
    )

    /** A registered interaction: its [target], [click], [callback] returning a [Result], and optional cooldown. */
    interface Data<T : EventsCallOptions<out Target>> {
        val target: Target
        val click: Click
        val callback: (T) -> Result
        val cooldownData: CooldownData<T>?
    }

    companion object {
        private val COOLDOWN_KEY = NamespacedKey(EffectiveSpigot.instance, "cooldown")

        /**
         * Whether a handler [registered] for a click should fire for the [actual] resolved click.
         * Base [Click.LEFT]/[Click.RIGHT] match both plain and sneaking variants; the `_SHIFT` / `_PLAIN`
         * forms match only the sneaking / non-sneaking case. [actual] is only ever a base or `_SHIFT`
         * value — resolution never produces a `_PLAIN` click.
         */
        private fun clickMatches(registered: Click, actual: Click): Boolean = when (registered) {
            Click.LEFT        -> actual == Click.LEFT || actual == Click.LEFT_SHIFT
            Click.RIGHT       -> actual == Click.RIGHT || actual == Click.RIGHT_SHIFT
            Click.LEFT_SHIFT  -> actual == Click.LEFT_SHIFT
            Click.RIGHT_SHIFT -> actual == Click.RIGHT_SHIFT
            Click.LEFT_PLAIN  -> actual == Click.LEFT
            Click.RIGHT_PLAIN -> actual == Click.RIGHT
        }

        /**
         * Resolves a raw interaction into a [Click]: [isRight] picks the button, [sneaking] the shift
         * modifier. Never returns a `_PLAIN` value — those exist only for registration.
         */
        internal fun resolveClick(isRight: Boolean, sneaking: Boolean): Click = when {
            isRight && sneaking -> Click.RIGHT_SHIFT
            isRight             -> Click.RIGHT
            sneaking            -> Click.LEFT_SHIFT
            else                -> Click.LEFT
        }

        /** [resolveClick] taking the sneak-state from [player]. */
        internal fun resolveClick(player: Player, isRight: Boolean): Click =
            resolveClick(isRight, player.isSneaking)

        private fun <T : EventsCallOptions<out Target>> checkCooldownAndRunCall(data: Data<T>, eventsCallOptions: T): Result {
            if (!clickMatches(data.click, eventsCallOptions.click)) return Result.ALLOW_EVENT

            if (data.cooldownData == null || data.cooldownData!!.cooldownToUseInTicks <= 0) return data.callback(eventsCallOptions)
            if (data.cooldownData!!.conditionForSkipCooldown?.invoke(eventsCallOptions) == true) return Result.ALLOW_EVENT

            val target = eventsCallOptions.target
            val instanceNamespacedKeyOrName = when (target) {
                is Target.Item -> EffectiveItem.getNamespacedKeyByItemElseMaterial(target.itemStack)
                is Target.Entity -> EffectiveEntity.getNamespacedKeyByEntity(target.entity)
                is Target.Block -> EffectiveEntity.getNamespacedKeyByEntity(target.itemDisplay)
            } ?: return data.callback(eventsCallOptions)
            val timeLatestUsed = if (data.cooldownData!!.cooldownType == CooldownType.ON_CURRENT_PLAYER) {
                val namespacedKey = NamespacedKey(
                    EffectiveSpigot.instance,
                    instanceNamespacedKeyOrName
                )

                EffectiveDataContainerUtils.getContainer(eventsCallOptions.player, COOLDOWN_KEY) { container ->
                    EffectiveDataContainerUtils.getContainerValue(container, namespacedKey, PersistentDataType.LONG)
                }
            } else  {
                if (target is Target.Block && target.itemDisplay == null) {
                    0L
                } else {
                    when (target) {
                        is Target.Item -> {
                            EffectiveDataContainerUtils.getContainerValue(
                                target.itemStack,
                                COOLDOWN_KEY,
                                PersistentDataType.LONG
                            )
                        }

                        is Target.Entity -> {
                            EffectiveDataContainerUtils.getContainerValue(
                                target.entity,
                                COOLDOWN_KEY,
                                PersistentDataType.LONG
                            )
                        }

                        is Target.Block -> {
                            EffectiveDataContainerUtils.getContainerValue(
                                target.itemDisplay!!,
                                COOLDOWN_KEY,
                                PersistentDataType.LONG
                            )
                        }
                    }
                }
            }

            if (timeLatestUsed != null) {
                val cooldownToUseInMillis = data.cooldownData!!.cooldownToUseInTicks * 50

                val millisPassed = System.currentTimeMillis() - timeLatestUsed
                if (millisPassed < cooldownToUseInMillis) {
                    val remainingMillis = cooldownToUseInMillis - millisPassed
                    val remainingSeconds = remainingMillis / 1000.0
                    EffectiveMinecraftUtils.sendMessageToActionBar(
                        eventsCallOptions.player,
                        Locale.getComponent("errors.cooldown.wait", remainingSeconds)
                            .color(NamedTextColor.RED)
                    )
                    return Result.CANCEL_EVENT
                }
            }

            return data.callback(eventsCallOptions).also { result ->
                if (result != Result.CANCEL_EVENT) return result
                if (eventsCallOptions.target is Target.Block) {
                    val target = eventsCallOptions.target as Target.Block
                    if (target.itemDisplay == null) return result
                }

                when (data.cooldownData!!.cooldownType) {
                    CooldownType.ON_CURRENT_PLAYER -> {
                        EffectiveDataContainerUtils.setContainer(
                            eventsCallOptions.player,
                            COOLDOWN_KEY
                        ) {
                            EffectiveDataContainerUtils.setContainerValue(
                                it,
                                NamespacedKey(
                                    EffectiveSpigot.instance, instanceNamespacedKeyOrName
                                ),
                                PersistentDataType.LONG,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    CooldownType.ON_THIS_INSTANCE -> {
                        setLatestTimeUsed(data.target)
                    }
                    CooldownType.ON_ALL_INSTANCES -> {
                        when (target) {
                            is Target.Item -> {
                                eventsCallOptions.player.inventory.forEach {
                                    if (it != null && EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(it, target.itemStack)) {
                                        setLatestTimeUsed(it)
                                    }
                                }
                            }

                            is Target.Entity -> {
                                val effectiveEntities = EffectiveEntity.namespacedKeyToEffectiveEntity.values
                                for (effectiveEntity in effectiveEntities) {
                                    effectiveEntity.getEntities().forEach {
                                        if (EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(it, target.entity)) {
                                            setLatestTimeUsed(it)
                                        }
                                    }
                                }
                            }

                            is Target.Block -> {
                                val effectiveEntities = EffectiveEntity.namespacedKeyToEffectiveEntity.values
                                for (effectiveEntity in effectiveEntities) {
                                    effectiveEntity.getEntities().forEach {
                                        if (EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(it, target.itemDisplay)) {
                                            setLatestTimeUsed(it)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        private fun setLatestTimeUsed(target: Any) {
            val obj = when (target) {
                is Target.Item -> target.itemStack
                is Target.Entity -> target.entity
                is Target.Block -> target.itemDisplay
                else -> target
            }

            when (obj) {
                is ItemStack -> EffectiveDataContainerUtils.setContainerValue(obj, COOLDOWN_KEY, PersistentDataType.LONG, System.currentTimeMillis())
                is PersistentDataHolder -> EffectiveDataContainerUtils.setContainerValue(obj, COOLDOWN_KEY, PersistentDataType.LONG, System.currentTimeMillis())
            }
        }


        /**
         * Runs [data]'s callback for [options] (applying its cooldown) and folds the outcome into
         * [currentResult]: returns true if this or any prior handler requested cancellation.
         */
        fun <T : EventsCallOptions<out Target>> runCallAndUpdateResult(currentResult: Boolean, data: Data<T>, options: T): Boolean {
            val callResult = checkCooldownAndRunCall(data, options)
            return currentResult || (callResult == Result.CANCEL_EVENT)
        }
    }
}