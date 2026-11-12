package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

/** A recipe in the participant's saved library. */
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val mealTypes: List<MealType> = emptyList(),
    val minutes: Int,
    val ingredients: List<Ingredient> = emptyList(),
    val utensils: List<String> = emptyList(),
    val steps: List<RecipeStep> = emptyList(),
    val source: RecipeSource,
    val status: RecipeStatus = RecipeStatus.DRAFT,
)

@Serializable
data class Ingredient(val name: String, val amount: String, val storeSection: StoreSection = StoreSection.OTHER)

@Serializable
data class RecipeStep(
    val shortLabel: String,
    val instruction: String,
    val photo: String? = null,
    val safety: List<String> = emptyList(),
)

@Serializable
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, DESSERT }

/** Where a recipe came from. */
@Serializable
enum class RecipeSource { COOKBOOK, GENERATED }

/** Review state of a recipe. */
@Serializable
enum class RecipeStatus { DRAFT, PENDING, PUBLISHED }
