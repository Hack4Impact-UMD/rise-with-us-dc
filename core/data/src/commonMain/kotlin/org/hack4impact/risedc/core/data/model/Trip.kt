package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String,
    val name: String,
    val steps: List<TripStep>,
    val source: TripSource,
    val coverPhoto: PhotoRef? = null,
) {
    init {
        require(steps.isNotEmpty()) { "A trip must have at least one step." }
    }
}

/** SAVED is a predefined route; AD_HOC is planned after the user enters a destination. */
@Serializable
enum class TripSource { SAVED, AD_HOC }
