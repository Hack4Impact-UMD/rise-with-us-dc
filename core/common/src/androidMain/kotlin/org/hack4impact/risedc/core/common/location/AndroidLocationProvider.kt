package org.hack4impact.risedc.core.common.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason
import kotlin.coroutines.resume

internal class AndroidLocationProvider(context: Context) : LocationProvider {
    private val context = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this.context) }

    override suspend fun currentLocation(): LocationResult {
        // Request location permission in the caller
        permissionFailure()?.let { return it }
        return try {
            if (isLocationEnabled()) {
                requestLocation()
            } else {
                LocationResult.Unavailable(Reason.ServicesDisabled)
            }
        } catch (_: SecurityException) {
            permissionFailure() ?: LocationResult.PermissionDenied
        }
    }

    @SuppressLint("MissingPermission") // Permission is checked before this call
    private suspend fun requestLocation(): LocationResult = withLocationTimeout {
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            // Cancel the location request if this call is canceled
            continuation.invokeOnCancellation { cancellation.cancel() }
            if (!continuation.isActive) {
                return@suspendCancellableCoroutine
            }
            // Get best estimate of location (might be cached if recent enough location exists)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                .build()
            client.getCurrentLocation(request, cancellation.token).addOnCompleteListener { task ->
                if (continuation.isActive) {
                    // Recheck permission after the request
                    val result = permissionFailure() ?: when {
                        task.isCanceled -> LocationResult.Unavailable(Reason.NoFix, "location request was cancelled")
                        task.isSuccessful -> task.result?.toResult()
                            ?: LocationResult.Unavailable(Reason.NoFix, "no location was returned")
                        task.exception is SecurityException -> LocationResult.PermissionDenied
                        else -> LocationResult.Unavailable(Reason.NoFix, "location request failed")
                    }
                    continuation.resume(result)
                }
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            // The passive provider cannot request locations
            manager.getProviders(true).any { it != LocationManager.PASSIVE_PROVIDER }
        }
    }

    private fun permissionFailure(): LocationResult? = when {
        context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED -> null
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ->
            LocationResult.ApproximateOnly
        else -> LocationResult.PermissionDenied
    }

    private fun Location.toResult(): LocationResult {
        if (!hasAccuracy()) {
            return LocationResult.Unavailable(Reason.NoFix, "location accuracy is unavailable")
        }
        return LocationResult.Success(
            lat = latitude,
            lng = longitude,
            accuracyMeters = accuracy.toDouble(),
            timestampMillis = time,
        ).validated()
    }
}
