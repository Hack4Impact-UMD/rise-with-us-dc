package org.hack4impact.risedc.core.data

import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.GroceryItem
import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.MealType
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.model.RecipeSource
import org.hack4impact.risedc.core.data.model.RecipeStatus
import org.hack4impact.risedc.core.data.model.RecipeStep
import org.hack4impact.risedc.core.data.model.StepItem
import org.hack4impact.risedc.core.data.model.StoreSection
import org.hack4impact.risedc.core.data.model.Utensil

internal object TestData {
    val json = Json { ignoreUnknownKeys = true }

    fun ingredient(
        name: String,
        amount: String = "1 bag",
        section: StoreSection = StoreSection.PANTRY,
        imageId: String? = null,
    ) = Ingredient(name = name, amount = amount, imageId = imageId, storeSection = section)

    fun recipe(
        id: String = "recipe-1",
        ingredients: List<Ingredient> =
            listOf(
                ingredient("Rice", "1 bag", StoreSection.PANTRY, imageId = "351"),
                ingredient("Black beans", "1 can", StoreSection.PANTRY, imageId = "287"),
                ingredient("Onion", "1", StoreSection.PRODUCE),
            ),
    ) = Recipe(
        id = id,
        title = "Test rice and beans",
        mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
        minutes = 30,
        servings = 4,
        ingredients = ingredients,
        utensils = listOf(Utensil("Pot", "K07"), Utensil("Wooden spoon", "K17")),
        steps =
        listOf(
            RecipeStep(
                instruction = "Simmer 1 cup of rice in the pot for 20 minutes.",
                imageId = "STEP20",
                ingredients = listOf(StepItem("Rice", "351")),
                utensils = listOf(StepItem("Pot", "K07")),
                safety = listOf("HOT"),
            ),
            RecipeStep(
                instruction = "Stir 1 can of beans into the pot.",
                imageId = "STEP09",
                ingredients = listOf(StepItem("Black beans", "287")),
                utensils = listOf(StepItem("Pot", "K07"), StepItem("Wooden spoon", "K17")),
            ),
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
