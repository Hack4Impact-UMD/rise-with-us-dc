package org.hack4impact.risedc.core.data.snap

import org.hack4impact.risedc.core.data.model.Ingredient
import org.hack4impact.risedc.core.data.model.Recipe
import org.hack4impact.risedc.core.data.model.RecipeSource
import org.hack4impact.risedc.core.data.model.RecipeStatus
import org.hack4impact.risedc.core.data.model.RecipeStep
import org.hack4impact.risedc.core.data.model.StepItem
import org.hack4impact.risedc.core.data.model.StoreSection
import org.hack4impact.risedc.core.data.model.Utensil
import org.hack4impact.risedc.core.data.sticker.StickerCatalog

/**
 * Turns a validated Gemini recipe into the app model. An image id that isn't in the matching catalog
 * list (including NO_IMAGE) becomes null: a missing picture shouldn't reject a safe recipe.
 */
internal fun GeneratedRecipe.toRecipe(id: String): Recipe {
    val ingredients = ingredients.map { generated ->
        val sticker = StickerCatalog.ingredients[generated.imageId.trim()]
        Ingredient(
            name = generated.name.trim(),
            amount = generated.amount.trim(),
            imageId = sticker?.id,
            storeSection = sticker?.storeSection?.takeIf { it != StoreSection.OTHER } ?: generated.storeSection,
        )
    }
    val utensils = utensils.map { Utensil(it.name.trim(), StickerCatalog.utensils[it.imageId.trim()]?.id) }
    val ingredientsByName = ingredients.associate { it.name.key() to StepItem(it.name, it.imageId) }
    val utensilsByName = utensils.associate { it.name.key() to StepItem(it.name, it.imageId) }
    return Recipe(
        id = id,
        title = title.trim(),
        mealTypes = listOf(mealType),
        minutes = minutes,
        servings = servings,
        imageId = StickerCatalog.dishes[imageId.trim()]?.id,
        ingredients = ingredients,
        utensils = utensils,
        steps = steps.map { step ->
            RecipeStep(
                instruction = step.instruction.trim(),
                imageId = StickerCatalog.steps[step.imageId.trim()]?.id,
                ingredients = step.ingredients.mapNotNull { ingredientsByName[it.key()] }.distinct(),
                utensils = step.utensils.mapNotNull { utensilsByName[it.key()] }.distinct(),
                safety = step.safety,
            )
        },
        source = RecipeSource.GENERATED,
        status = RecipeStatus.PENDING,
    )
}
