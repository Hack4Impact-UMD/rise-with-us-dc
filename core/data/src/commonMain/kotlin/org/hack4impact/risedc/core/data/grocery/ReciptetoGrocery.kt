package org.hack4impact.risedc.core.data.grocery

import org.hack4impact.risedc.core.data.model.GroceryItem
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.model.Recipe

/*
 * Rules (explained in the PR):
 * - Items match by name, ignoring case and surrounding spaces.
 * - Amounts are free text, so matches join amounts ("2 cups + 1 cup") instead of adding numbers.
 * - A checked item goes back to "Still looking" when another recipe needs it.
 * - Adding a recipe that is already on the list does nothing.
 */

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
                    amount = "${old.amount} + ${newItem.amount}",
                    gotIt = false,
                    fromRecipeIds = old.fromRecipeIds + recipe.id,
                )
        }
    }
    return copy(items = result)
}
