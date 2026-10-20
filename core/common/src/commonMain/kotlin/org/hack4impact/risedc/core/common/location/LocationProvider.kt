package org.hack4impact.risedc.core.common.location

interface LocationProvider {
    /**
     * Gets the current location with a 30-second timeout.
     *
     * Permission: on iOS this shows the system prompt if the user hasn't decided yet
     * (prompt time is not counted toward the timeout). On Android it never prompts;
     * request ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION first. With neither granted,
     * this returns [LocationResult.PermissionDenied], which also means "never asked" on Android.
     * Coarse-only access returns [LocationResult.ApproximateOnly].
     */
    suspend fun currentLocation(): LocationResult
}

sealed interface LocationResult {
    /** Coordinates in degrees (latitude, longitude), accuracy in meters, and a timestamp (ms) */
    data class Success(val lat: Double, val lng: Double, val accuracyMeters: Double, val timestampMillis: Long) :
        LocationResult
    data object PermissionDenied : LocationResult
    data object ApproximateOnly : LocationResult

    /** Use reason for UI decisions and detail for debugging */
    data class Unavailable(val reason: Reason, val detail: String? = null) : LocationResult {
        enum class Reason { ServicesDisabled, TimedOut, NoFix, NotSupported, Misconfigured }
    }
}
