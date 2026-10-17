package org.hack4impact.risedc.core.common.location

interface LocationProvider {
    /** Gets the current location with a 30-second timeout */
    suspend fun currentLocation(): LocationResult
}

sealed interface LocationResult {
    /** Coordinates in degrees (latitude, longitude), accuracy in meters, and a timestamp (ms) */
    data class Success(val lat: Double, val lng: Double, val accuracyMeters: Double, val timestampMillis: Long) :
        LocationResult
    data object PermissionDenied : LocationResult
    data object ApproximateOnly : LocationResult
    data class Unavailable(val reason: String) : LocationResult
}
