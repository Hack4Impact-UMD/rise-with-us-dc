package org.hack4impact.risedc.core.data.grocery

import org.hack4impact.risedc.core.data.TestData.ingredient
import org.hack4impact.risedc.core.data.TestData.item
import org.hack4impact.risedc.core.data.TestData.recipe
import org.hack4impact.risedc.core.data.model.GroceryList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeToGroceryTest {
    @Test
    fun recipeWithNoIngredientsAddsNothing() {
        val list = GroceryList(id = "list", items = listOf(item("Bread")))
        assertEquals(list.items, list.addRecipe(recipe(ingredients = emptyList())).items)
    }

    @Test
    fun addingToEmptyListAddsEveryIngredientAsStillLooking() {
        val items = GroceryList(id = "list").addRecipe(recipe()).items

        assertEquals(listOf("Rice", "Black beans", "Onion"), items.map { it.name })
        assertTrue(items.none { it.gotIt })
    }

    @Test
    fun allNewItemsAreAddedAfterExistingOnes() {
        val list = GroceryList(id = "list", items = listOf(item("Bread")))
        assertEquals(listOf("Bread", "Rice", "Black beans", "Onion"), list.addRecipe(recipe()).items.map { it.name })
    }

    @Test
    fun manyRecipesCombineSharedIngredients() {
        val list =
            GroceryList(id = "list")
                .addRecipe(recipe(id = "r1", ingredients = listOf(ingredient("Eggs", "2"))))
                .addRecipe(recipe(id = "r2", ingredients = listOf(ingredient("eggs", "3"))))
                .addRecipe(recipe(id = "r3", ingredients = listOf(ingredient("EGGS ", "1"))))

        val eggs = list.items.single()
        assertEquals("2 + 3 + 1", eggs.amount)
        assertEquals(setOf("r1", "r2", "r3"), eggs.fromRecipeIds)
    }

    @Test
    fun duplicateIngredientsInsideOneRecipeAreMerged() {
        val salty = recipe(ingredients = listOf(ingredient("Salt", "1 tsp"), ingredient("salt", "1 pinch")))
        assertEquals("1 tsp + 1 pinch", GroceryList(id = "list").addRecipe(salty).items.single().amount)
    }

    @Test
    fun blankAmountsAreSkippedWhenCombining() {
        val list = GroceryList(id = "list", items = listOf(item("Salt", amount = "")))
        val salt = list.addRecipe(recipe(ingredients = listOf(ingredient("Salt", "1 tsp")))).items.single()
        assertEquals("1 tsp", salt.amount)
    }

    @Test
    fun alreadyCheckedItemGoesBackToStillLooking() {
        val list = GroceryList(id = "list", items = listOf(item("Onion", "2", gotIt = true)))
        val onion = list.addRecipe(recipe()).items.single { it.name == "Onion" }

        assertFalse(onion.gotIt)
        assertEquals("2 + 1", onion.amount)
    }

    @Test
    fun addingTheSameRecipeTwiceChangesNothing() {
        val once = GroceryList(id = "list").addRecipe(recipe())
        assertEquals(once, once.addRecipe(recipe()))
    }

    @Test
    fun inputListIsNotChanged() {
        val list = GroceryList(id = "list", items = listOf(item("Rice", "2 cups")))
        val before = list.copy()
        list.addRecipe(recipe())
        assertEquals(before, list)
    }
}
