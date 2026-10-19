@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.hack4impact.risedc.core.common.location

import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason
import platform.CoreLocation.CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val MILLIS_PER_SECOND = 1_000

internal class IosLocationProvider : LocationProvider {
    override suspend fun currentLocation(): LocationResult {
        // Keep this blocking system check off the main thread
        val servicesEnabled = withContext(Dispatchers.Default) { CLLocationManager.locationServicesEnabled() }
        if (!servicesEnabled) {
            return LocationResult.Unavailable(Reason.ServicesDisabled)
        }
        // Use the main thread to receive location updates
        return withContext(Dispatchers.Main) {
            val request = IosLocationRequest()
            try {
                request.currentLocation()
            } finally {
                request.close()
            }
        }
    }
}

private class IosLocationRequest :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var authorization: CancellableContinuation<Unit>? = null
    private var location: CancellableContinuation<LocationResult>? = null

    init {
        manager.delegate = this
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    suspend fun currentLocation(): LocationResult {
        // Exclude the permission prompt from the timeout
        return authorize() ?: withLocationTimeout {
            suspendCancellableCoroutine { continuation ->
                location = continuation
                manager.requestLocation()
            }
        }
    }

    // Ask for permission if the user has not decided yet
    private suspend fun authorize(): LocationResult? {
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            if (NSBundle.mainBundle.objectForInfoDictionaryKey("NSLocationWhenInUseUsageDescription") == null) {
                return LocationResult.Unavailable(Reason.Misconfigured, "missing NSLocationWhenInUseUsageDescription")
            }
            suspendCancellableCoroutine { continuation ->
                authorization = continuation
                manager.requestWhenInUseAuthorization()
            }
        }
        return permissionFailure()
    }

    // iOS calls this when location permission changes
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            val continuation = authorization
            authorization = null
            if (continuation?.isActive == true) {
                continuation.resume(Unit)
            }
        }
        permissionFailure()?.let { complete(it) }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        // Permission can change while waiting for a location
        permissionFailure()?.let {
            complete(it)
            return
        }
        val receivedLocation = didUpdateLocations.lastOrNull() as? CLLocation
        val result = receivedLocation?.coordinate?.useContents {
            LocationResult.Success(
                lat = latitude,
                lng = longitude,
                accuracyMeters = receivedLocation.horizontalAccuracy,
                timestampMillis = (receivedLocation.timestamp.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong(),
            ).validated()
        }
            ?: LocationResult.Unavailable(Reason.NoFix, "no location was returned")
        complete(result)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        complete(permissionFailure() ?: LocationResult.Unavailable(Reason.NoFix, "location request failed"))
    }

    fun close() {
        // This also cancels a pending requestLocation call
        manager.stopUpdatingLocation()
        manager.delegate = null
        authorization = null
        location = null
    }

    private fun permissionFailure(): LocationResult? = when {
        manager.authorizationStatus != kCLAuthorizationStatusAuthorizedAlways &&
            manager.authorizationStatus != kCLAuthorizationStatusAuthorizedWhenInUse -> LocationResult.PermissionDenied
        manager.accuracyAuthorization == CLAccuracyAuthorizationReducedAccuracy -> LocationResult.ApproximateOnly
        else -> null
    }

    private fun complete(result: LocationResult) {
        val continuation = location
        // Clear location after reading it in
        location = null
        if (continuation?.isActive == true) {
            continuation.resume(result)
        }
    }
}
