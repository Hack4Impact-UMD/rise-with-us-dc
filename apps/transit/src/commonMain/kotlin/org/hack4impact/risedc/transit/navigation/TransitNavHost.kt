package org.hack4impact.risedc.transit.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.hack4impact.risedc.core.ui.placeholder.PlaceholderScreen

// Sample arguments so the placeholder graph can be clicked through before real data exists.
private const val SAMPLE_TRIP_ID = "sample-trip"
private const val SAMPLE_PLACE_ID = "sample-place"

/** The Transit nav graph. Every destination is a placeholder; swap in real screens as they land. */
@Composable
fun TransitNavHost(navController: NavHostController = rememberNavController()) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = TransitRoute.MyTrips) {
        composable<TransitRoute.MyTrips> {
            PlaceholderScreen(
                title = "My Trips",
                links = listOf(
                    "Saved trip" to { navController.navigate(TransitRoute.TripOverview(tripId = SAMPLE_TRIP_ID)) },
                    "Places" to { navController.navigate(TransitRoute.Places) },
                    "Staff" to { navController.navigate(TransitRoute.StaffHome) },
                ),
            )
        }
        composable<TransitRoute.Places> {
            PlaceholderScreen(
                title = "Places",
                onBack = back,
                links = listOf(
                    "Go to a place" to { navController.navigate(TransitRoute.TripOverview(placeId = SAMPLE_PLACE_ID)) },
                ),
            )
        }
        composable<TransitRoute.TripOverview> { entry ->
            val route = entry.toRoute<TransitRoute.TripOverview>()
            val tripId = route.tripId ?: SAMPLE_TRIP_ID
            PlaceholderScreen(
                title = "Trip Overview (trip=${route.tripId}, place=${route.placeId})",
                onBack = back,
                links = listOf("Start trip" to { navController.navigate(TransitRoute.TripStep(tripId, index = 0)) }),
            )
        }
        composable<TransitRoute.TripStep> { entry ->
            val route = entry.toRoute<TransitRoute.TripStep>()
            PlaceholderScreen(
                title = "Step ${route.index} (walk | wait | ride)",
                onBack = back,
                links = listOf(
                    "Next step" to { navController.navigate(TransitRoute.TripStep(route.tripId, route.index + 1)) },
                    "Arrive" to { navController.navigate(TransitRoute.YouMadeIt(route.tripId)) },
                ),
            )
        }
        composable<TransitRoute.YouMadeIt> {
            PlaceholderScreen(
                title = "You Made It",
                links = listOf("Done" to { navController.popBackStack<TransitRoute.MyTrips>(inclusive = false) }),
            )
        }

        composable<TransitRoute.StaffHome> {
            PlaceholderScreen(
                title = "Staff",
                onBack = back,
                links = listOf(
                    "Trip Builder" to { navController.navigate(TransitRoute.TripBuilder()) },
                    "Places" to { navController.navigate(TransitRoute.StaffPlaces) },
                ),
            )
        }
        composable<TransitRoute.TripBuilder> { entry ->
            val tripId = entry.toRoute<TransitRoute.TripBuilder>().tripId
            PlaceholderScreen(title = "Trip Builder (${tripId ?: "new"})", onBack = back)
        }
        composable<TransitRoute.StaffPlaces> {
            PlaceholderScreen(title = "Staff Places", onBack = back)
        }
    }
}
