package org.hack4impact.risedc.recipes.navigation

import kotlinx.serialization.Serializable

/**
 * Every screen in the Recipes app (spec: Screens). Participant flow:
 *   Library → GetReady → Cook(step 0..n) → Done
 *   Snap → WhatISee → PickAMeal → GetReady
 *   GroceryList
 * Staff: StaffHome → StaffEditor / ApprovalQueue / StaffSettings
 */
sealed interface RecipesRoute {
    @Serializable
    data object Library : RecipesRoute

    @Serializable
    data class GetReady(val recipeId: String) : RecipesRoute

    /** One destination per step, so system Back goes to the previous step. [step] is zero-based. */
    @Serializable
    data class Cook(val recipeId: String, val step: Int) : RecipesRoute

    @Serializable
    data class Done(val recipeId: String) : RecipesRoute

    @Serializable
    data object Snap : RecipesRoute

    @Serializable
    data object WhatISee : RecipesRoute

    @Serializable
    data object PickAMeal : RecipesRoute

    @Serializable
    data object GroceryList : RecipesRoute

    @Serializable
    data object StaffHome : RecipesRoute

    /** [recipeId] null = new recipe. */
    @Serializable
    data class StaffEditor(val recipeId: String? = null) : RecipesRoute

    @Serializable
    data object ApprovalQueue : RecipesRoute

    @Serializable
    data object StaffSettings : RecipesRoute
}
