package ru.hukm.effectiveSpigot.minecraft.interfaces

import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.entities.EffectiveEntity
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveDataContainerUtils
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils


interface EffectiveAbstractInteract {
    enum class Result { CANCEL_EVENT, ALLOW_EVENT }
    enum class Click { LEFT, RIGHT }

    enum class CooldownType { ON_CURRENT_PLAYER, ON_THIS_INSTANCE, ON_ALL_INSTANCES }

    sealed class Target {
        data class Item(val itemStack: ItemStack) : Target()
        data class Entity(val entity: org.bukkit.entity.Entity) : Target()
        data class Block(val material: Material, val block: org.bukkit.block.Block?, val itemDisplay: ItemDisplay?) : Target()
    }

    interface EventsCallOptions<out T : Target> {
        val player: Player
        val click: Click
        val target: T
        val hand: EquipmentSlot
    }

    data class CooldownData<T : EventsCallOptions<out Target>>(
        val cooldownToUseInTicks: Int = 0,
        val conditionForSkipCooldown: ((T) -> Boolean)? = null,
        val cooldownType: CooldownType? = null
    )

    interface Data<T : EventsCallOptions<out Target>> {
        val target: Target
        val click: Click
        val callback: (T) -> Result
        val cooldownData: CooldownData<T>?
    }

    companion object {
        private val COOLDOWN_KEY = NamespacedKey(EffectiveSpigot.instance, "cooldown")

        private fun <T : EventsCallOptions<out Target>> checkCooldownAndRunCall(data: Data<T>, eventsCallOptions: T): Result {
            if (data.click != eventsCallOptions.click) return Result.ALLOW_EVENT

            val target = eventsCallOptions.target
            val instanceNamespacedKeyOrName = when (target) {
                is Target.Item -> EffectiveItem.getNamespacedKeyByItem(target.itemStack)
                is Target.Entity -> EffectiveEntity.getNamespacedKeyByEntity(target.entity)
                is Target.Block -> EffectiveEntity.getNamespacedKeyByEntity(target.itemDisplay)
            }!!

            if (data.cooldownData == null || data.cooldownData!!.cooldownToUseInTicks <= 0) return data.callback(eventsCallOptions)
            if (data.cooldownData!!.conditionForSkipCooldown?.invoke(eventsCallOptions) == true) return Result.ALLOW_EVENT
            val timeLatestUsed = if (data.cooldownData!!.cooldownType == CooldownType.ON_CURRENT_PLAYER) {
                val namespacedKey = NamespacedKey(
                    EffectiveSpigot.instance,
                    instanceNamespacedKeyOrName
                )

                EffectiveDataContainerUtils.getContainerValue(
                    EffectiveDataContainerUtils.getContainerValue(
                        eventsCallOptions.player,
                        COOLDOWN_KEY,
                        PersistentDataType.TAG_CONTAINER
                    )!!,
                    namespacedKey,
                    PersistentDataType.LONG
                )
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
                    val remainingSeconds = remainingMillis / 1000
                    EffectiveMinecraftUtils.sendMessageToActionBar(
                        eventsCallOptions.player,
                        LanguageModule.getMessage("errors.cooldown.wait", remainingSeconds),
                        ChatColor.RED
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
                                EffectiveEntity.entities.forEach {
                                    if (EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(it, target.entity)) {
                                        setLatestTimeUsed(it)
                                    }
                                }
                            }

                            is Target.Block -> {
                                EffectiveEntity.entities.forEach {
                                    if (EffectiveEntity.equalByNamespacedKeyIfExistElseByEntityType(it, target.itemDisplay)) {
                                        setLatestTimeUsed(it)
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


        fun <T : EventsCallOptions<out Target>> runCallAndUpdateResult(currentResult: Boolean, data: Data<T>, options: T): Boolean {
            val callResult = checkCooldownAndRunCall(data, options)
            return currentResult || (callResult == Result.CANCEL_EVENT)
        }
    }
}