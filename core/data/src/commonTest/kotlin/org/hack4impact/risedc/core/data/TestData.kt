package org.hack4impact.risedc.core.data

import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.GroceryItem
import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.model.RecipeSource
import org.hack4impact.risedc.core.data.model.RecipeStatus
import org.hack4impact.risedc.core.data.model.RecipeStep
import org.hack4impact.risedc.core.data.model.StoreSection

internal object TestData {
    val json = Json { ignoreUnknownKeys = true }

    fun ingredient(name: String, amount: String = "1 cup", section: StoreSection = StoreSection.PANTRY) =
        Ingredient(name = name, amount = amount, storeSection = section)

    fun recipe(
        id: String = "recipe-1",
        ingredients: List<Ingredient> =
            listOf(
                ingredient("Rice", "1 cup", StoreSection.PANTRY),
                ingredient("Black beans", "1 can", StoreSection.PANTRY),
                ingredient("Onion", "1", StoreSection.PRODUCE),
            ),
    ) = Recipe(
        id = id,
        title = "Test rice and beans",
        mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
        minutes = 30,
        ingredients = ingredients,
        utensils = listOf("Pot", "Wooden spoon"),
        steps =
        listOf(
            RecipeStep(
                shortLabel = "Cook the rice",
                instruction = "Simmer the rice in two cups of water for 20 minutes.",
                safety = listOf("Use an oven mitt on the pot handle"),
            ),
            RecipeStep(shortLabel = "Add beans", instruction = "Stir in the beans.", photo = "steps/beans.png"),
        ),
        source = RecipeSource.COOKBOOK,
        status = RecipeStatus.PUBLISHED,
    )

    fun item(
        name: String,
        amount: String = "1",
        section: StoreSection = StoreSection.OTHER,
        gotIt: Boolean = false,
        fromRecipeIds: Set<String> = emptySet(),
    ) = GroceryItem(name, amount, section, gotIt, fromRecipeIds)
}
