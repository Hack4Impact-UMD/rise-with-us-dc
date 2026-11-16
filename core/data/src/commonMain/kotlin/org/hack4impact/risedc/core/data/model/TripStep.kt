package org.hack4impact.risedc.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TripStep {
    @Serializable
    @SerialName("walk")
    data class Walk(val maneuver: Maneuver, val blocks: Int, val photo: PhotoRef? = null) : TripStep {
        init {
            require(blocks >= 0) { "Blocks must not be negative." }
        }
    }

    @Serializable
    @SerialName("waitRail")
    data class WaitRail(val station: Station, val line: RailLine, val toward: Station) : TripStep

    /** Stops in travel order, ending at [exitAt]. */
    @Serializable
    @SerialName("rideRail")
    data class RideRail(val stops: List<Station>, val exitAt: Station) : TripStep {
        init {
            require(stops.lastOrNull()?.code == exitAt.code) { "The last station must match the exit station." }
        }
    }

    @Serializable
    @SerialName("waitBus")
    data class WaitBus(val stop: BusStop, val route: String) : TripStep

    /** Stops in travel order, ending at [exitAt]. */
    @Serializable
    @SerialName("rideBus")
    data class RideBus(val stops: List<BusStop>, val exitAt: BusStop) : TripStep {
        init {
            require(stops.lastOrNull()?.id == exitAt.id) { "The last stop must match the exit stop." }
        }
    }
}

/** UNKNOWN gives distance only, or an empty string for zero blocks. */
fun TripStep.Walk.instruction(): String {
    val direction = when (maneuver) {
        Maneuver.DEPART -> "Start walking."
        Maneuver.STRAIGHT -> "Go straight."
        Maneuver.TURN_LEFT -> "Turn left."
        Maneuver.TURN_RIGHT -> "Turn right."
        Maneuver.TURN_SLIGHT_LEFT -> "Turn a little to the left."
        Maneuver.TURN_SLIGHT_RIGHT -> "Turn a little to the right."
        Maneuver.TURN_SHARP_LEFT -> "Make a sharp left turn."
        Maneuver.TURN_SHARP_RIGHT -> "Make a sharp right turn."
        Maneuver.UTURN_LEFT, Maneuver.UTURN_RIGHT -> "Turn around."
        Maneuver.UNKNOWN -> ""
    }
    val distance = when (blocks) {
        0 -> ""
        1 -> "Walk 1 block."
        else -> "Walk $blocks blocks."
    }
    return listOf(direction, distance).filter { it.isNotEmpty() }.joinToString(" ")
}
