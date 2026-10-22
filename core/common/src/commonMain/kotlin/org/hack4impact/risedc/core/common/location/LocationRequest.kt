package org.hack4impact.risedc.core.common.location

import kotlinx.coroutines.withTimeoutOrNull
import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason
import kotlin.time.Duration.Companion.milliseconds

internal const val LOCATION_TIMEOUT_MILLIS = 30_000L // 30s
private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0

internal suspend fun withLocationTimeout(request: suspend () -> LocationResult): LocationResult =
    withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS.milliseconds) { request() }
        ?: LocationResult.Unavailable(Reason.TimedOut)

internal fun LocationResult.Success.validated(): LocationResult {
    // Check that latitude and longitude coordinates are valid and that reported accuracy is valid
    val validCoordinates = lat in -MAX_LATITUDE..MAX_LATITUDE && lng in -MAX_LONGITUDE..MAX_LONGITUDE
    val validAccuracy = accuracyMeters.isFinite() && accuracyMeters >= 0
    return if (validCoordinates && validAccuracy) {
        this
    } else {
        LocationResult.Unavailable(Reason.NoFix, "invalid location")
    }
}
