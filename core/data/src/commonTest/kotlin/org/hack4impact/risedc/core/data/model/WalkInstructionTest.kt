package org.hack4impact.risedc.core.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalkInstructionTest {
    @Test
    fun instructionsMatchManeuverAndBlocks() {
        val directions = mapOf(
            Maneuver.DEPART to "Start walking.",
            Maneuver.STRAIGHT to "Go straight.",
            Maneuver.TURN_LEFT to "Turn left.",
            Maneuver.TURN_RIGHT to "Turn right.",
            Maneuver.TURN_SLIGHT_LEFT to "Turn a little to the left.",
            Maneuver.TURN_SLIGHT_RIGHT to "Turn a little to the right.",
            Maneuver.TURN_SHARP_LEFT to "Make a sharp left turn.",
            Maneuver.TURN_SHARP_RIGHT to "Make a sharp right turn.",
            Maneuver.UTURN_LEFT to "Turn around.",
            Maneuver.UTURN_RIGHT to "Turn around.",
            Maneuver.UNKNOWN to "",
        )
        assertEquals(Maneuver.entries.toSet(), directions.keys)
        directions.forEach { (maneuver, direction) ->
            assertEquals(direction, TripStep.Walk(maneuver, 0).instruction())
            assertEquals(
                "$direction Walk 1 block.".trim(),
                TripStep.Walk(maneuver, 1).instruction(),
            )
            assertEquals(
                "$direction Walk 2 blocks.".trim(),
                TripStep.Walk(maneuver, 2).instruction(),
            )
        }
    }

    @Test
    fun rejectsNegativeBlocks() {
        assertFailsWith<IllegalArgumentException> { TripStep.Walk(Maneuver.STRAIGHT, -1) }
    }
}
