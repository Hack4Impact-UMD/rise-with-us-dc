package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

/**
 * A recipe in the participant's saved library.
 * [imageId] is the closest finished-dish sticker id from the catalog, or null when no sticker fits.
 */
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val mealTypes: List<MealType> = emptyList(),
    val minutes: Int,
    val servings: Int,
    val imageId: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val utensils: List<Utensil> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
    val source: RecipeSource,
    val status: RecipeStatus = RecipeStatus.DRAFT,
)

/**
 * [name] is the simple name shown on screen ("Oats"), not the catalog name. [amount] is a short
 * measure such as "1 cup", kept for the grocery list; steps also say it in their instruction.
 * [imageId] is the closest sticker id from the catalog, or null when no sticker fits.
 */
@Serializable
data class Ingredient(
    val name: String,
    val amount: String,
    val imageId: String? = null,
    val storeSection: StoreSection = StoreSection.OTHER,
)

/**
 * A utensil the recipe needs. [name] is the simple name shown on screen, not the catalog name.
 * [imageId] is the closest sticker id from the catalog, or null when no sticker fits.
 */
@Serializable
data class Utensil(val name: String, val imageId: String? = null)

/**
 * One instruction. [imageId] is the closest step-action sticker id ("Spread", "Pour"), or null.
 * It's the step's only picture, for cookbook and generated recipes alike.
 * [ingredients] and [utensils] are the recipe's items this step uses, copied with their pictures
 * so each step carries everything the screen needs.
 */
@Serializable
data class RecipeStep(
    val instruction: String,
    val imageId: String? = null,
    val ingredients: List<StepItem> = emptyList(),
    val utensils: List<StepItem> = emptyList(),
    val safety: List<String> = emptyList(),
)

/** An ingredient or utensil used in a step: its on-screen name and its sticker id, or null. */
@Serializable
data class StepItem(val name: String, val imageId: String? = null)

@Serializable
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, DESSERT }

/** Where a recipe came from. */
@Serializable
enum class RecipeSource { COOKBOOK, GENERATED }

/** Review state of a recipe. */
@Serializable
enum class RecipeStatus { DRAFT, PENDING, PUBLISHED }
