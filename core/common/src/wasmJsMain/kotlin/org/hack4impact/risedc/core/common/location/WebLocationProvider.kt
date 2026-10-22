package org.hack4impact.risedc.core.common.location

import org.hack4impact.risedc.core.common.location.LocationResult.Unavailable.Reason

internal class WebLocationProvider : LocationProvider {
    override suspend fun currentLocation(): LocationResult = LocationResult.Unavailable(Reason.NotSupported)
}
