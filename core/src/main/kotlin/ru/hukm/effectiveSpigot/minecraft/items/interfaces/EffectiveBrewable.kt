package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BrewingStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.BrewerInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import ru.hukm.effectiveSpigot.EffectiveSpigot
import ru.hukm.effectiveSpigot.interfaces.IModule
import ru.hukm.effectiveSpigot.minecraft.items.EffectiveItem

interface EffectiveBrewable {
    data class Data(
        val result: ItemStack,
        val inputIngredient: ItemStack,
        val inputBase: ItemStack,
        val fuelUse: Int,
        val cookingTime: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is Data) return false
            return EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(inputIngredient, other.inputIngredient) &&
                    inputIngredient.amount == other.inputIngredient.amount &&
                    EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(inputBase, other.inputBase) &&
                    inputBase.amount == other.inputBase.amount &&
                    EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(result, other.result) &&
                    result.amount == other.result.amount &&
                    fuelUse == other.fuelUse &&
                    cookingTime == other.cookingTime
        }

        override fun hashCode(): Int {
            var result1 = fuelUse
            result1 = 31 * result1 + cookingTime
            result1 = 31 * result1 + result.hashCode()
            result1 = 31 * result1 + inputIngredient.hashCode()
            result1 = 31 * result1 + inputBase.hashCode()
            return result1
        }

    }

    companion object {
        private val brewRecipes = arrayListOf<Data>()

        internal fun getModule(): IModule {
            return object : IModule {
                override fun init() {
                    EffectiveSpigot.instance.server.pluginManager.registerEvents(Events(), EffectiveSpigot.instance)
                }
            }
        }

        fun getRecipeFromBlock(brewingStand: BrewingStand): Data? {
            val inventory = brewingStand.inventory
            val ingredient = inventory.ingredient ?: return null
            val bases = (0..2).mapNotNull { inventory.getItem(it) }.filter { it.type != Material.AIR }
            if (bases.isEmpty()) return null
            val base = bases[0]
            val allSame = bases.all {
                EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(it, base) && it.amount == base.amount
            }
            if (!allSame) return null
            return getRecipeBy(ingredient, base)
        }

        fun registerRecipe(data: Data) {
            brewRecipes.add(data)
        }

        fun getRecipeBy(inputIngredient: ItemStack, inputBase: ItemStack): Data? {
            for (recipe in brewRecipes) {
                if (EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(recipe.inputIngredient, inputIngredient) && inputIngredient.amount >= recipe.inputIngredient.amount &&
                    EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(recipe.inputBase, inputBase) && recipe.inputBase.amount == inputBase.amount
                    ) {
                        return recipe
                    }
            }

            return null
        }
    }

    class Events : Listener {
        private fun manageBrewerInventory(event: InventoryClickEvent) {
            event.isCancelled = true

            val p = event.whoClicked as Player
            val slot = event.currentItem ?: ItemStack(Material.AIR)
            var held = event.cursor ?: ItemStack(Material.AIR)
            val empty = ItemStack(Material.AIR)

            val slotC = slot.amount
            val heldC = held.amount

            when (event.click) {
                ClickType.LEFT -> {
                    if (slot.isSimilar(held)) {
                        if (slotC + heldC > slot.maxStackSize) {
                            slot.amount = slot.maxStackSize
                            event.currentItem = slot
                            held.amount = heldC - (slot.maxStackSize - slotC)
                            p.itemOnCursor = held
                        } else {
                            slot.amount = slotC + heldC
                            event.currentItem = slot
                            p.itemOnCursor = empty
                        }
                    } else {
                        event.currentItem = held
                        p.itemOnCursor = slot
                    }
                }
                ClickType.RIGHT -> {
                    if (heldC > 0 && (slot.isSimilar(held) || slotC == 0) && slotC + 1 <= held.maxStackSize) {
                        event.currentItem = held.clone().also { it.amount = slotC + 1 }
                        p.itemOnCursor = if (heldC - 1 > 0) held.also { it.amount = heldC - 1 } else empty
                    } else if (heldC == 0) {
                        event.currentItem = slot.clone().also { it.amount = slotC / 2 }
                        p.itemOnCursor = slot.also { it.amount = slotC - slotC / 2 }
                    } else {
                        event.currentItem = held
                        p.itemOnCursor = slot
                    }
                }
                ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                    val overflow = p.inventory.addItem(slot)
                    event.currentItem = if (overflow.isNotEmpty()) overflow.values.first() else empty
                }
                ClickType.DROP, ClickType.CONTROL_DROP -> {
                    val dropAll = event.click == ClickType.CONTROL_DROP
                    if (slotC == 0 || heldC > 0) return
                    val hand = p.inventory.itemInMainHand
                    p.inventory.setItemInMainHand(slot)
                    p.dropItem(dropAll)
                    p.inventory.setItemInMainHand(hand)
                    val drop = if (dropAll) slotC else 1
                    event.currentItem = if (slotC - drop > 0) slot.also { it.amount = slotC - drop } else empty
                }
                ClickType.SWAP_OFFHAND -> {
                    event.currentItem = p.inventory.itemInOffHand
                    p.inventory.setItemInOffHand(slot)
                }
                ClickType.NUMBER_KEY -> {
                    event.currentItem = p.inventory.getItem(event.hotbarButton)
                    p.inventory.setItem(event.hotbarButton, slot)
                }
                ClickType.DOUBLE_CLICK -> {
                    for ((i, stack) in event.inventory.contents.withIndex()) {
                        if (stack == null || !stack.isSimilar(held)) continue
                        val stackC = stack.amount
                        if (stackC + held.amount > held.maxStackSize) {
                            stack.amount = held.amount - (held.maxStackSize - stackC)
                            held.amount = held.maxStackSize
                            event.inventory.setItem(i, stack)
                        } else {
                            held.amount += stackC
                            event.inventory.setItem(i, empty)
                        }
                        p.itemOnCursor = held
                    }
                    if (held.amount < held.maxStackSize) {
                        for ((i, stack) in p.inventory.contents.withIndex()) {
                            if (stack == null || !stack.isSimilar(held)) continue
                            val stackC = stack.amount
                            if (stackC + held.amount > held.maxStackSize) {
                                stack.amount = held.amount - (held.maxStackSize - stackC)
                                held.amount = held.maxStackSize
                                p.inventory.setItem(i, stack)
                            } else {
                                held.amount += stackC
                                p.inventory.setItem(i, empty)
                            }
                            p.itemOnCursor = held
                        }
                    }
                }
                else -> {}
            }

            ((event.inventory as BrewerInventory).holder as BrewingStand).update(true)
        }

        @EventHandler
        fun onInventoryClickEvent(event: InventoryClickEvent) {
            if (event.view.topInventory !is BrewerInventory) return

            if (event.rawSlot == 3) {
                val cursor = event.cursor
                val current = event.currentItem
                val cursorIsCustom = cursor != null && cursor.type != Material.AIR &&
                    brewRecipes.any { EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(it.inputIngredient, cursor) }
                val slotIsCustom = current != null && current.type != Material.AIR &&
                    brewRecipes.any { EffectiveItem.equalByNamespacedKeyIfExistElseByMaterial(it.inputIngredient, current) }
                if (cursorIsCustom || slotIsCustom) {
                    manageBrewerInventory(event)
                }
            }

            Bukkit.getScheduler().runTaskLater(EffectiveSpigot.instance, Runnable {
                val brewerInventory = event.view.topInventory as BrewerInventory

                brewerInventory.holder ?: return@Runnable

                val recipe = getRecipeFromBlock(brewerInventory.holder!!)
                if (recipe != null) {
                    brewerInventory.holder!!.let {
                        it.recipeBrewTime = recipe.cookingTime
                        it.brewingTime = recipe.cookingTime
                        it.fuelLevel = (it.fuelLevel - recipe.fuelUse).coerceAtLeast(0)
                        it.update()
                    }

                    object : BukkitRunnable() {
                        var ticksLeft = recipe.cookingTime
                        override fun run() {
                            val brewingStand = brewerInventory.holder
                            println("OK1")
                            if (brewingStand == null) {
                                cancel()
                                return
                            }
                            println("OK2")
                            val currentRecipe = getRecipeFromBlock(brewingStand)
                            if (currentRecipe == null || currentRecipe != recipe) {
                                brewingStand.brewingTime = 0
                                brewingStand.update()
                                cancel()
                                return
                            }
                            println("OK3")

                            if (ticksLeft <= 0) {
                                for (i in 0..2) {
                                    val base = brewerInventory.getItem(i)
                                    if (base != null && base.type != Material.AIR) {
                                        brewerInventory.setItem(i, recipe.result.clone())
                                        brewingStand.update()
                                    }
                                }
                                val ingredient = brewerInventory.ingredient!!

                                if (ingredient.amount <= recipe.inputIngredient.amount) {
                                    brewerInventory.ingredient = ItemStack(Material.AIR)
                                } else {
                                    ingredient.amount -= recipe.inputIngredient.amount
                                    brewerInventory.ingredient = ingredient
                                }

                                brewingStand.brewingTime = 0
                                brewingStand.update()
                                cancel()
                                return
                            }

                            println(ticksLeft)
                            //TODO(сделать, чтобы заново варка начиналась, если ингридиенты есть* 2.) при добавлении айтемов варка начинается заново/

                            brewingStand.brewingTime = ticksLeft
                            brewingStand.update()
                            
                            ticksLeft--
                        }
                    }.runTaskTimer(EffectiveSpigot.instance, 0L, 1L)
                }

                println("inv: ${(0..2).map { brewerInventory.getItem(it) }} ingredient=${brewerInventory.ingredient} fuel=${brewerInventory.fuel}")
            }, 1)
        }
    }
}
