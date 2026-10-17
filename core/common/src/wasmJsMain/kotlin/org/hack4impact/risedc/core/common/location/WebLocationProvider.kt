package org.hack4impact.risedc.core.common.location

internal class WebLocationProvider : LocationProvider {
    override suspend fun currentLocation(): LocationResult = LocationResult.Unavailable("not supported on web")
}
