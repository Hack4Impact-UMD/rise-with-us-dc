package org.hack4impact.risedc.recipes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.hack4impact.risedc.core.ui.placeholder.PlaceholderScreen

// Sample argument so the placeholder graph can be clicked through before real data exists.
private const val SAMPLE_RECIPE_ID = "sample"

/** The Recipes nav graph. Every destination is a placeholder; swap in real screens as they land. */
@Composable
fun RecipesNavHost(navController: NavHostController = rememberNavController()) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = RecipesRoute.Library) {
        composable<RecipesRoute.Library> {
            PlaceholderScreen(
                title = "Library",
                links = listOf(
                    "Get Ready" to { navController.navigate(RecipesRoute.GetReady(SAMPLE_RECIPE_ID)) },
                    "Snap & Cook" to { navController.navigate(RecipesRoute.Snap) },
                    "Grocery List" to { navController.navigate(RecipesRoute.GroceryList) },
                    "Staff" to { navController.navigate(RecipesRoute.StaffHome) },
                ),
            )
        }
        composable<RecipesRoute.GetReady> { entry ->
            val route = entry.toRoute<RecipesRoute.GetReady>()
            PlaceholderScreen(
                title = "Get Ready (${route.recipeId})",
                onBack = back,
                links = listOf(
                    "Start cooking" to { navController.navigate(RecipesRoute.Cook(route.recipeId, step = 0)) },
                ),
            )
        }
        composable<RecipesRoute.Cook> { entry ->
            val route = entry.toRoute<RecipesRoute.Cook>()
            PlaceholderScreen(
                title = "Cook (${route.recipeId}, step ${route.step})",
                onBack = back,
                links = listOf(
                    "Next step" to { navController.navigate(RecipesRoute.Cook(route.recipeId, route.step + 1)) },
                    "Finish" to { navController.navigate(RecipesRoute.Done(route.recipeId)) },
                ),
            )
        }
        composable<RecipesRoute.Done> {
            PlaceholderScreen(
                title = "Done",
                links = listOf(
                    "Back to library" to { navController.popBackStack<RecipesRoute.Library>(inclusive = false) },
                ),
            )
        }

        composable<RecipesRoute.Snap> {
            PlaceholderScreen(
                title = "Snap",
                onBack = back,
                links = listOf("What I See" to { navController.navigate(RecipesRoute.WhatISee) }),
            )
        }
        composable<RecipesRoute.WhatISee> {
            PlaceholderScreen(
                title = "What I See",
                onBack = back,
                links = listOf("Pick a Meal" to { navController.navigate(RecipesRoute.PickAMeal) }),
            )
        }
        composable<RecipesRoute.PickAMeal> {
            PlaceholderScreen(
                title = "Pick a Meal",
                onBack = back,
                links = listOf("Get Ready" to { navController.navigate(RecipesRoute.GetReady(SAMPLE_RECIPE_ID)) }),
            )
        }

        composable<RecipesRoute.GroceryList> {
            PlaceholderScreen(title = "Grocery List", onBack = back)
        }

        composable<RecipesRoute.StaffHome> {
            PlaceholderScreen(
                title = "Staff",
                onBack = back,
                links = listOf(
                    "Editor" to { navController.navigate(RecipesRoute.StaffEditor()) },
                    "Approval Queue" to { navController.navigate(RecipesRoute.ApprovalQueue) },
                    "Settings" to { navController.navigate(RecipesRoute.StaffSettings) },
                ),
            )
        }
        composable<RecipesRoute.StaffEditor> { entry ->
            val recipeId = entry.toRoute<RecipesRoute.StaffEditor>().recipeId
            PlaceholderScreen(title = "Editor (${recipeId ?: "new"})", onBack = back)
        }
        composable<RecipesRoute.ApprovalQueue> {
            PlaceholderScreen(title = "Approval Queue", onBack = back)
        }
        composable<RecipesRoute.StaffSettings> {
            PlaceholderScreen(title = "Settings", onBack = back)
        }
    }
}
