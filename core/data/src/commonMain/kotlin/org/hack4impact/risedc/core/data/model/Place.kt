package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0

/** Coordinates are in degrees. */
@Serializable
data class Place(val id: String, val name: String, val lat: Double, val lng: Double, val photo: PhotoRef? = null) {
    init {
        require(lat in -MAX_LATITUDE..MAX_LATITUDE && lng in -MAX_LONGITUDE..MAX_LONGITUDE) {
            "Place coordinates must be valid degrees."
        }
    }
}
