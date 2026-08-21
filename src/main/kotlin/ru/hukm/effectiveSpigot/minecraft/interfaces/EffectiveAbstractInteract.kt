package ru.hukm.effectiveSpigot.minecraft.interfaces

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
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

    /**
     * Scope a cooldown applies to.
     * - [ON_CURRENT_PLAYER] — keyed by item/entity type in the acting player's PDC. Blocks any stack
     *   of the same type in that player's hands (including copies picked up later), independent of
     *   other players.
     * - [ON_THIS_INSTANCE] — written to the exact clicked item/entity's own PDC. Follows the stack
     *   around; other stacks of the same type are unaffected.
     */
    enum class CooldownType { ON_CURRENT_PLAYER, ON_THIS_INSTANCE }

    /** What was interacted with: an item or an entity. */
    sealed class Target {
        data class Item(val itemStack: ItemStack) : Target()
        data class Entity(val entity: org.bukkit.entity.Entity) : Target()
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
     * @property conditionForSkipCall optional predicate: if it returns true for a call, the callback
     *   is skipped entirely and the event is allowed to proceed (so neither the interaction nor the
     *   cooldown fires)
     * @property cooldownType scope the cooldown is tracked against
     */
    data class CooldownData<T : EventsCallOptions<out Target>>(
        val cooldownToUseInTicks: Int = 0,
        val conditionForSkipCall: ((T) -> Boolean)? = null,
        val cooldownType: CooldownType = CooldownType.ON_CURRENT_PLAYER
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

            val cd = data.cooldownData
            if (cd == null || cd.cooldownToUseInTicks <= 0) return data.callback(eventsCallOptions)
            if (cd.conditionForSkipCall?.invoke(eventsCallOptions) == true) return Result.ALLOW_EVENT

            val target = eventsCallOptions.target
            val instanceNamespacedKeyOrName = when (target) {
                is Target.Item -> EffectiveItem.getNamespacedKeyByItemElseMaterial(target.itemStack)
                is Target.Entity -> EffectiveEntity.getNamespacedKeyByEntity(target.entity)
            } ?: return data.callback(eventsCallOptions)

            val timeLatestUsed = if (cd.cooldownType == CooldownType.ON_CURRENT_PLAYER) {
                val namespacedKey = NamespacedKey(EffectiveSpigot.instance, instanceNamespacedKeyOrName)

                EffectiveDataContainerUtils.getContainer(eventsCallOptions.player, COOLDOWN_KEY) { container ->
                    EffectiveDataContainerUtils.getContainerValue(container, namespacedKey, PersistentDataType.LONG)
                }
            } else {
                when (target) {
                    is Target.Item -> EffectiveDataContainerUtils.getContainerValue(target.itemStack, COOLDOWN_KEY, PersistentDataType.LONG)
                    is Target.Entity -> EffectiveDataContainerUtils.getContainerValue(target.entity, COOLDOWN_KEY, PersistentDataType.LONG)
                }
            }

            if (timeLatestUsed != null) {
                val cooldownToUseInMillis = cd.cooldownToUseInTicks * 50

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

                when (cd.cooldownType) {
                    CooldownType.ON_CURRENT_PLAYER -> {
                        EffectiveDataContainerUtils.setContainer(eventsCallOptions.player, COOLDOWN_KEY) {
                            EffectiveDataContainerUtils.setContainerValue(
                                it,
                                NamespacedKey(EffectiveSpigot.instance, instanceNamespacedKeyOrName),
                                PersistentDataType.LONG,
                                System.currentTimeMillis()
                            )
                        }
                    }
                    CooldownType.ON_THIS_INSTANCE -> {
                        setLatestTimeUsed(target)
                    }
                }
            }
        }

        private fun setLatestTimeUsed(target: Target) {
            val obj: Any? = when (target) {
                is Target.Item -> target.itemStack
                is Target.Entity -> target.entity
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