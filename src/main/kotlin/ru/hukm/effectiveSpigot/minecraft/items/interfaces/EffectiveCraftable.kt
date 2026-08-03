package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.Plugin
import ru.hukm.effectiveSpigot.Locale
import ru.hukm.effectiveSpigot.utils.EffectiveCombinator

/**
 * Behaviour that registers shapeless crafting recipes for custom items.
 *
 * Usually reached via [EffectiveItem.addShapelessCraft]; use the companion directly to craft an
 * arbitrary result stack.
 */
interface EffectiveCraftable {
    companion object {
        /**
         * Registers shapeless recipe(s) producing [result].
         *
         * Each ingredient may be a [Material], an [ItemStack], a [org.bukkit.Tag] of materials, or a
         * list of alternatives; the cartesian product of all alternatives is registered as separate
         * recipes under `name` + index.
         *
         * ⚠️ The recipe count is the **product** of the sizes of every alternative-bearing ingredient
         * (tags and lists), so combinations blow up fast — e.g. two `Tag`s of 30 materials each yield
         * 900 registered recipes. Keep alternative sets small, or match by [Material]/[ItemStack]
         * directly, to avoid bloating the recipe registry.
         *
         * @param plugin owner of the recipe keys
         * @param name base recipe key (an index is appended per combination)
         * @throws IllegalArgumentException if an ingredient has an unsupported type
         * @throws ClassCastException if a [org.bukkit.Tag] does not contain [Material] values
         */
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
                            throw ClassCastException(Locale.getMessage("errors.tag_invalid_material", firstElement::class.simpleName ?: "unknown"))
                        }
                        @Suppress("UNCHECKED_CAST")
                        (ingredient as Tag<Material>).values.toList()
                    }
                    is List<*> -> {
                        if (ingredient.isEmpty()) {
                            throw IllegalArgumentException(Locale.getMessage("errors.ingredient_empty"))
                        }

                        val first = ingredient.first()!!

                        if (first !is Material && first !is ItemStack) {
                            throw IllegalArgumentException(Locale.getMessage("errors.list_invalid_material", first::class.simpleName ?: "unknown"))
                        }

                        @Suppress("UNCHECKED_CAST")
                        ingredient as List<Any>
                    }
                    else -> throw IllegalArgumentException(Locale.getMessage("errors.ingredient_invalid_type", ingredient::class.simpleName ?: "unknown"))
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