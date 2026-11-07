package org.hack4impact.risedc.core.data.grocery

import org.hack4impact.risedc.core.data.model.GroceryItem
import org.hack4impact.risedc.core.data.model.GroceryList
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.model.StoreSection

fun Recipe.ingredientList(): List<GroceryItem> =
    ingredients.map { GroceryItem(it.name.trim(), it.amount, it.storeSection, fromRecipeIds = setOf(id)) }

fun GroceryList.addRecipe(recipe: Recipe): GroceryList {
    // Ingredients this recipe already put on the list are skipped, so adding it twice changes nothing,
    // but ingredients added to the recipe since then still come through.
    val alreadyAdded = items.filter { recipe.id in it.fromRecipeIds }.map { it.name.trim().lowercase() }.toSet()

    val result = items.toMutableList()
    for (newItem in recipe.ingredientList()) {
        if (newItem.name.lowercase() in alreadyAdded) continue
        val index = result.indexOfFirst { it.name.trim().equals(newItem.name, ignoreCase = true) }
        if (index < 0) {
            result += newItem
        } else {
            val old = result[index]
            result[index] =
                old.copy(
                    amount = listOf(old.amount, newItem.amount).filter { it.isNotBlank() }.joinToString(" + "),
                    storeSection = old.storeSection.takeUnless { it == StoreSection.OTHER } ?: newItem.storeSection,
                    // Only something more to buy puts a checked item back to Still looking.
                    gotIt = old.gotIt && newItem.amount.isBlank(),
                    fromRecipeIds = old.fromRecipeIds + recipe.id,
                )
        }
    }
    return copy(items = result)
}
