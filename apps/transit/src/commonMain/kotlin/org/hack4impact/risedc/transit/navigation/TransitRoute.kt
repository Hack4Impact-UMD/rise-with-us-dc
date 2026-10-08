package org.hack4impact.risedc.transit.navigation

import kotlinx.serialization.Serializable

/**
 * Every screen in the Transit app (spec: Screens). Participant flow:
 *   MyTrips (saved) or Places (ad hoc) → TripOverview → TripStep(0..n) → YouMadeIt
 * Staff: StaffHome → TripBuilder / StaffPlaces
 */
sealed interface TransitRoute {
    @Serializable
    data object MyTrips : TransitRoute

    @Serializable
    data object Places : TransitRoute

    /** Either a saved trip ([tripId]) or a new route to a place ([placeId]). */
    @Serializable
    data class TripOverview(val tripId: String? = null, val placeId: String? = null) : TransitRoute

    /** One destination per step (walk / wait / ride). [index] is zero-based. */
    @Serializable
    data class TripStep(val tripId: String, val index: Int) : TransitRoute

    @Serializable
    data class YouMadeIt(val tripId: String) : TransitRoute

    @Serializable
    data object StaffHome : TransitRoute

    /** [tripId] null = new trip. */
    @Serializable
    data class TripBuilder(val tripId: String? = null) : TransitRoute

    @Serializable
    data object StaffPlaces : TransitRoute
}
