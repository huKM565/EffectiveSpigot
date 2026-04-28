package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.Plugin
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.utils.EffectiveCombinator

interface EffectiveCraftable {
    companion object {
        fun addShapelessCraft(
            result: ItemStack,
            ingredients: List<Any>,
            plugin: Plugin,
            name: String,
        ) {
            val newIngredients: List<List<Any>> = ingredients.map { ingredient ->
                when (ingredient) {
                    is Material -> listOf(ingredient)
                    is ItemStack -> listOf(ingredient)
                    is Tag<*> -> {
                        val firstElement = ingredient.values.firstOrNull()
                        if (firstElement != null && firstElement !is Material) {
                            throw ClassCastException(LanguageModule.getMessage("errors.tag_invalid_material", firstElement::class.simpleName ?: "unknown"))
                        }
                        @Suppress("UNCHECKED_CAST")
                        (ingredient as Tag<Material>).values.toList()
                    }
                    is List<*> -> {
                        if (ingredient.isEmpty()) {
                            throw IllegalArgumentException(LanguageModule.getMessage("errors.ingredient_empty"))
                        }

                        val first = ingredient.first()!!

                        if (first !is Material && first !is ItemStack) {
                            throw IllegalArgumentException(LanguageModule.getMessage("errors.list_invalid_material", first::class.simpleName ?: "unknown"))
                        }

                        @Suppress("UNCHECKED_CAST")
                        ingredient as List<Any>
                    }
                    else -> throw IllegalArgumentException(LanguageModule.getMessage("errors.ingredient_invalid_type", ingredient::class.simpleName ?: "unknown"))
                }
            }

            val combinations = EffectiveCombinator.getAllCombinations(newIngredients)

            for (combinationIndex in combinations.indices) {
                val combination = combinations[combinationIndex]

                val shapelessRecipe = ShapelessRecipe(NamespacedKey(plugin, name + combinationIndex), result)

                combination.forEach { item ->
                    when (item) {
                        is Material -> shapelessRecipe.addIngredient(item)
                        is ItemStack -> shapelessRecipe.addIngredient(RecipeChoice.ExactChoice(item))
                    }
                }

                Bukkit.addRecipe(shapelessRecipe)
            }
        }
    }
}