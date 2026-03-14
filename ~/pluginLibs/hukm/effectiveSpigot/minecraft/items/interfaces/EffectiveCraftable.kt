package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.Plugin
import ru.hukm.effectiveSpigot.language.LanguageModule
import ru.hukm.effectiveSpigot.utils.EffectiveAlphabets
import ru.hukm.effectiveSpigot.utils.EffectiveCombinator

interface EffectiveCraftable {
    companion object {
        fun addShapelessCraft(
            result: ItemStack,
            ingredients: List<Any>,
            plugin: Plugin,
            name: String,
        ) {
            val newIngredients = ingredients.map { ingredient ->
                when (ingredient) {
                    is Material -> listOf(ingredient)
                    is Tag<*> -> {
                        val firstElement = ingredient.values.firstOrNull()
                        if (firstElement != null && firstElement !is Material) {
                            throw ClassCastException(LanguageModule.getMessage("errors.tag_invalid_material", firstElement::class.simpleName ?: "unknown"))
                        }
                        @Suppress("UNCHECKED_CAST")
                        (ingredient as Tag<Material>).values.toList()
                    }
                    is List<*> -> {
                        if (ingredient.isNotEmpty() && ingredient.first() !is Material) {
                            throw IllegalArgumentException(LanguageModule.getMessage("errors.list_invalid_material", ingredient.first()!!::class.simpleName ?: "unknown"))
                        }

                        if (ingredient.isEmpty()) {
                            throw IllegalArgumentException(LanguageModule.getMessage("errors.ingredient_empty"))
                        }

                        @Suppress("UNCHECKED_CAST")
                        ingredient as List<Material>
                    }
                    else -> throw IllegalArgumentException(LanguageModule.getMessage("errors.ingredient_invalid_type", ingredient::class.simpleName ?: "unknown"))
                }
            }

            val combinations = EffectiveCombinator.getAllCombinations(newIngredients)

            for (combinationIndex in combinations.indices) {
                val combination = combinations[combinationIndex]

                val shapelessRecipe = ShapelessRecipe(NamespacedKey(plugin, name + combinationIndex), result)

                combination.forEach { material ->
                    shapelessRecipe.addIngredient(material)
                }

                Bukkit.addRecipe(shapelessRecipe)
            }
        }
    }
}