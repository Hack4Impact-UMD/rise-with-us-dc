package org.hack4impact.risedc.core.data.grocery

import org.hack4impact.risedc.core.data.model.GroceryItem
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.model.Recipe


fun Recipe.ingredientList(): List<GroceryItem> =
    ingredients.map { GroceryItem(it.name.trim(), it.amount, it.storeSection, fromRecipeIds = setOf(id)) }

fun GroceryList.addRecipe(recipe: Recipe): GroceryList {
    if (items.any { recipe.id in it.fromRecipeIds }) return this

    val result = items.toMutableList()
    for (newItem in recipe.ingredientList()) {
        val index = result.indexOfFirst { it.name.trim().equals(newItem.name, ignoreCase = true) }
        if (index < 0) {
            result += newItem
        } else {
            val old = result[index]
            result[index] =
                old.copy(
                     amount = listOf(old.amount, newItem.amount).filter { it.isNotBlank() }.joinToString(" + "),
                      gotIt = false,
                       fromRecipeIds = old.fromRecipeIds + recipe.id,
                )
        }
    }
    return copy(items = result)
}
