package ru.hukm.effectiveSpigot.minecraft.items.interfaces

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.Plugin
import ru.hukm.effectiveSpigot.utils.EffectiveAlphabets
import ru.hukm.effectiveSpigot.utils.EffectiveCombinator

interface EffectiveCraftable {
    companion object {
        private val materialVariants: HashMap<String, ArrayList<Material>>
            get() = hashMapOf<String, ArrayList<Material>>(
                "planks" to getMaterialsContainsChars("planks")
            )

        fun getMaterialsContainsChars(chars: String) = Material.values().filter { it.name.lowercase().contains(chars) } as ArrayList

        fun registerShapedCraft(
            result: ItemStack,
            shape: ArrayList<String>,
            ingredients: Map<Char, Material>,
            plugin: Plugin,
            name: String,
            useMaterialVariants: Boolean = true
        ) {
            if (!useMaterialVariants) {
                val shapedRecipe = ShapedRecipe(NamespacedKey(plugin, name), result)
                shapedRecipe.shape(*shape.toTypedArray())

                ingredients.forEach { (char, material) ->
                    shapedRecipe.setIngredient(char, material)
                }

                Bukkit.getServer().addRecipe(shapedRecipe)
                return
            }

            var lettersIndex = 0
            val variants: HashMap<Char, ArrayList<Material>> = hashMapOf();
            for (i in 0 until shape.size) {
                val str = shape[i]

                for (char in str) {
                    for (pair in materialVariants) {
                        if (ingredients[char] != null && ingredients[char]!!.name.lowercase().contains(pair.key)) {
                            while (true) {
                                if (!shape.all { it.contains(EffectiveAlphabets.UPPERCASE_ENGLISH[lettersIndex]) } ) {
                                    shape[i] = str.replace(char, EffectiveAlphabets.UPPERCASE_ENGLISH[lettersIndex])
                                    variants.put(EffectiveAlphabets.UPPERCASE_ENGLISH[i], pair.value)
                                    break;
                                } else lettersIndex++
                            }
                        }
                    }
                }
            }

            val newMaterialShape = arrayListOf<ArrayList<Material>>()

            shape.forEach {
                for (char in it) {
                    when {
                        ingredients[char] != null -> {
                            newMaterialShape.add(arrayListOf(ingredients[char]!!))
                        }
                        variants[char] != null -> {
                            newMaterialShape.add(variants[char]!!)
                        }
                        else -> {
                            newMaterialShape.add(arrayListOf(Material.AIR))
                        }
                    }
                }
            }

            val combinations = EffectiveCombinator.getAllCombinations(newMaterialShape)

            for (i in combinations.indices) {
                val combination = combinations[i]

                val newShape = arrayOf<String>(
                    "ABC",
                    "DEF",
                    "GHI"
                )

                val shapedRecipe = ShapedRecipe(NamespacedKey(plugin, name + i), result)
                shapedRecipe.shape(*newShape)

                for (j in 0 until 8) {
                    shapedRecipe.setIngredient(EffectiveAlphabets.UPPERCASE_ENGLISH[j], combination[j])
                }

                Bukkit.addRecipe(shapedRecipe)
            }

        }

        fun registerShapelessCraft(
            result: ItemStack,
            ingredients: List<Material>,
            plugin: Plugin,
            name: String,
            useMaterialVariants: Boolean = true
        ) {
            if(!useMaterialVariants) {
                val shapelessRecipe = ShapelessRecipe(NamespacedKey(plugin, name), result)

                ingredients.forEach { material ->
                    shapelessRecipe.addIngredient(material)
                }

                Bukkit.getServer().addRecipe(shapelessRecipe)
                return
            }

            val newIngredients = ArrayList(ingredients.filter {
                var pass = true
                for (pair in materialVariants) {
                    if (it.name.lowercase().contains(pair.key)) {
                        pass = false
                        break
                    }
                }

                pass
            } ).map { arrayListOf<Material>(it) } .toMutableList() as ArrayList<ArrayList<Material>>

            for (ingredient in ingredients) {
                for (pair in materialVariants) {
                    if (ingredient.name.lowercase().contains(pair.key)) {
                        newIngredients.add(pair.value)
                        break
                    }
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