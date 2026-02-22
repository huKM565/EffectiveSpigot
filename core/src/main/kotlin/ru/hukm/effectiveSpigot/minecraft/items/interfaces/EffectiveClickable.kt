package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.block.Action
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveInventoryUtils
import ru.hukm.effectiveSpigot.minecraft.utils.EffectiveMinecraftUtils
import java.util.UUID

typealias InteractCallback = (EffectiveClickable.EventsCallOptions) -> EffectiveClickable.Result
typealias ConditionForSkipCooldown = (EffectiveClickable.EventsCallOptions) -> Boolean

interface EffectiveClickable {
    enum class Click { LEFT, RIGHT }

    enum class Result { CANCEL_EVENT, ALLOW_EVENT }

    data class CooldownData(
        val namespacedKeyOrMaterial: Any,
        var lastUsedTick: Int
    )

    data class Data(
        val item: ItemStack,
        val click: Click,
        val callback: InteractCallback,
        val ifRightClickOpenContainer: Boolean = false,
        val cooldownToUseInTicks: Int = 0,
        val conditionForSkipCooldown: ConditionForSkipCooldown? = null
    )

    data class EventsCallOptions(
        val player: Player,
        val item: ItemStack,
        val hand: EquipmentSlot,
        val click: Click,
        val clickedBlock: Block?,
        val clickedEntity: Entity?,
    )

    companion object{
        private val clickableItems = arrayListOf<Data>()
        private val playerUUIDInteractedWithEntity = arrayListOf<UUID>()
        private val cooldownItems = hashMapOf<UUID, ArrayList<CooldownData>>()

        fun resetPlayerUUIDInteractedWithEntity() {
            playerUUIDInteractedWithEntity.clear()
        }

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun addClickHandler(
            item: ItemStack,
            click: Click,
            callback: InteractCallback,
            ifRightClickOpenContainer: Boolean = false,
            cooldownToUseInTicks: Int = 0,
            conditionForSkipCooldown: ConditionForSkipCooldown? = null
        ) {
            clickableItems.add(Data(
                item,
                click,
                callback,
                ifRightClickOpenContainer,
                cooldownToUseInTicks,
                conditionForSkipCooldown
            ))
        }

        private fun checkCooldownAndRunCall(data: Data, eventsCallOptions: EventsCallOptions): Result {
            if (data.cooldownToUseInTicks <= 0) return data.callback(eventsCallOptions)
            if (data.conditionForSkipCooldown?.invoke(eventsCallOptions) == true) return Result.ALLOW_EVENT

            val playerUuid = eventsCallOptions.player.uniqueId
            val cooldownDates = cooldownItems.getOrPut(playerUuid) { arrayListOf() }
            val currentTick = EffectiveSpigot.mcvModule.getCurrentTick()

            val namespacedKey = EffectiveItem.getNamespacedKeyByItem(eventsCallOptions.item)
            val identifier: Any = namespacedKey ?: eventsCallOptions.item.type

            val existingCooldown = cooldownDates.find { it.namespacedKeyOrMaterial == identifier }

            if (existingCooldown != null) {
                val ticksPassed = currentTick - existingCooldown.lastUsedTick
                if (ticksPassed < data.cooldownToUseInTicks) {
                    val remainingTicks = data.cooldownToUseInTicks - ticksPassed
                    val remainingSeconds = remainingTicks / 20.0
                    EffectiveMinecraftUtils.sendMessageToActionBar(
                        eventsCallOptions.player,
                        LanguageModule.getMessage("errors.cooldown.wait", remainingSeconds),
                        ChatColor.RED
                    )
                    return Result.CANCEL_EVENT
                }
            }

            return data.callback(eventsCallOptions).also { result ->
                if (result == Result.CANCEL_EVENT) {
                    existingCooldown?.let { it.lastUsedTick = currentTick } ?: cooldownDates.add(CooldownData(identifier, currentTick))
                }
            }
        }

        private fun runCallAndUpdateResult(currentResult: Boolean, data: Data, options: EventsCallOptions): Boolean {
            val callResult = checkCooldownAndRunCall(data, options)
            return currentResult || (callResult == Result.CANCEL_EVENT)
        }

        fun tryCall(eventsCallOptions: EventsCallOptions): Boolean {
            val item = eventsCallOptions.item

            if (item.type == Material.AIR) return false

            var result = false

            for (clickableItem in clickableItems) {
                val isEqual = EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(clickableItem.item, item)

                if (isEqual) {
                    if (clickableItem.click == Click.RIGHT && eventsCallOptions.click == Click.RIGHT) {
                        if (!(eventsCallOptions.clickedBlock is Container && !clickableItem.ifRightClickOpenContainer)) {
                            result = runCallAndUpdateResult(result, clickableItem, eventsCallOptions)
                        }
                    } else if (clickableItem.click == Click.LEFT && eventsCallOptions.click == Click.LEFT) {
                        result = runCallAndUpdateResult(result, clickableItem, eventsCallOptions)
                    }
                }
            }

            return result
        }
    }

    //TODO(Добавить эвент разрушения блока)
    class Events() : Listener {
        @EventHandler
        fun onPlayerInteractEvent(event: PlayerInteractEvent) {
            if (playerUUIDInteractedWithEntity.contains(event.player.uniqueId)) return
            val click = if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) Click.LEFT else Click.RIGHT
            println(click)
            if (tryCall(EventsCallOptions(event.player, event.item ?: ItemStack(Material.AIR), event.hand ?: EquipmentSlot.HAND, click, event.clickedBlock, null))) {
                event.isCancelled = true
            }

        }

        @EventHandler
        fun onPlayerInteractWithEntity(event: PlayerInteractAtEntityEvent) {
            playerUUIDInteractedWithEntity.add(event.player.uniqueId)
            if (tryCall(EventsCallOptions(event.player, EffectiveInventoryUtils.getItemFromEquipmentSlot(event.player, event.hand) ?: ItemStack(Material.AIR), event.hand, Click.RIGHT, null , event.rightClicked))) {
                event.isCancelled = true
            }
        }

        @EventHandler
        fun onPlayerHitEntity(event: EntityDamageByEntityEvent) {
            if (
                event.damager is Player &&
                event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                ) {
                tryCall(EventsCallOptions(event.damager as Player, EffectiveInventoryUtils.getUsedItemFromHands(event.damager as Player) ?: ItemStack(Material.AIR), EquipmentSlot.HAND, Click.LEFT, null, event.entity))
            }
        }
    }
}