package org.hack4impact.risedc.core.data.model

import org.hack4impact.risedc.core.data.network.RiseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransitModelsTest {
    private val station = Station("A01", "Metro Center")
    private val nextStation = Station("B01", "Gallery Place")
    private val busStop = BusStop("00123", "Practice stop")
    private val nextBusStop = BusStop("00456", "Practice destination")

    @Test
    fun tripJsonFormatIsStable() {
        val json = """
            {"id":"practice","name":"Practice trip","steps":[
            {"type":"walk","maneuver":"TURN_RIGHT","blocks":2,"photo":{"path":"landmark"}},
            {"type":"waitRail","station":{"code":"A01","name":"Metro Center"},"line":"RD",
            "toward":{"code":"B01","name":"Gallery Place"}},
            {"type":"rideRail","stops":[{"code":"A01","name":"Metro Center"},
            {"code":"B01","name":"Gallery Place"}],"exitAt":{"code":"B01","name":"Gallery Place"}},
            {"type":"waitBus","stop":{"id":"00123","name":"Practice stop"},"route":"practice"},
            {"type":"rideBus","stops":[{"id":"00123","name":"Practice stop"},
            {"id":"00456","name":"Practice destination"}],
            "exitAt":{"id":"00456","name":"Practice destination"}}],
            "source":"SAVED","coverPhoto":{"path":"practice-cover"}}
        """.trimIndent().lines().joinToString("") {
            it.trim()
        }
        val expected = Trip(
            id = "practice",
            name = "Practice trip",
            steps = listOf(
                TripStep.Walk(Maneuver.TURN_RIGHT, 2, PhotoRef("landmark")),
                TripStep.WaitRail(station, RailLine.RD, nextStation),
                TripStep.RideRail(listOf(station, nextStation), nextStation),
                TripStep.WaitBus(busStop, "practice"),
                TripStep.RideBus(listOf(busStop, nextBusStop), nextBusStop),
            ),
            source = TripSource.SAVED,
            coverPhoto = PhotoRef("practice-cover"),
        )
        assertEquals(expected, RiseJson.decodeFromString<Trip>(json))
        assertEquals(json, RiseJson.encodeToString(expected))
    }

    @Test
    fun tripsMustHaveSteps() {
        assertFailsWith<IllegalArgumentException> {
            Trip("empty", "Empty trip", emptyList(), TripSource.SAVED)
        }
        assertFailsWith<IllegalArgumentException> {
            RiseJson.decodeFromString<Trip>("""{"id":"empty","name":"Empty trip","steps":[],"source":"SAVED"}""")
        }
    }

    @Test
    fun maneuverJsonRoundTripsWithUnknownFallback() {
        Maneuver.entries.forEach { maneuver ->
            assertEquals(maneuver, RiseJson.decodeFromString<Maneuver>(RiseJson.encodeToString(maneuver)))
        }
        listOf("FUTURE_MANEUVER", "MANEUVER_UNSPECIFIED").forEach { value ->
            val step = RiseJson.decodeFromString<TripStep>(
                """{"type":"walk","maneuver":"$value","blocks":2}""",
            )
            assertEquals(TripStep.Walk(Maneuver.UNKNOWN, 2), step)
        }
    }

    @Test
    fun rideExitMustMatchLastStopId() {
        val rail = TripStep.RideRail(listOf(station, nextStation), nextStation.copy(name = "Updated station label"))
        val bus = TripStep.RideBus(listOf(busStop, nextBusStop), nextBusStop.copy(name = "Updated stop label"))
        val trip = Trip("labels", "Practice ride", listOf(rail, bus), TripSource.SAVED)
        assertEquals(trip, RiseJson.decodeFromString<Trip>(RiseJson.encodeToString(trip)))
        assertFailsWith<IllegalArgumentException> {
            TripStep.RideRail(listOf(station), station.copy(code = "B01"))
        }
        assertFailsWith<IllegalArgumentException> {
            TripStep.RideBus(listOf(busStop), busStop.copy(id = "00456"))
        }
        assertFailsWith<IllegalArgumentException> {
            TripStep.RideRail(listOf(station, nextStation), station)
        }
        assertFailsWith<IllegalArgumentException> {
            TripStep.RideBus(listOf(busStop, nextBusStop), busStop)
        }
    }

    @Test
    fun invalidStepsFailToDecode() {
        assertFailsWith<IllegalArgumentException> {
            RiseJson.decodeFromString<TripStep>("""{"type":"walk","maneuver":"STRAIGHT","blocks":-1}""")
        }
        assertFailsWith<IllegalArgumentException> {
            RiseJson.decodeFromString<TripStep>(
                """{"type":"rideRail","stops":[],"exitAt":{"code":"A01","name":"Metro Center"}}""",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RiseJson.decodeFromString<TripStep>(
                """{"type":"rideBus","stops":[],"exitAt":{"id":"00123","name":"Practice stop"}}""",
            )
        }
    }

    @Test
    fun placeCoordinatesMustBeValid() {
        listOf(-90.0 to -180.0, 90.0 to 180.0).forEach { (lat, lng) ->
            val place = Place("boundary", "Boundary", lat, lng)
            assertEquals(place, RiseJson.decodeFromString<Place>(RiseJson.encodeToString(place)))
        }
        listOf(
            90.1 to 0.0,
            -90.1 to 0.0,
            0.0 to 180.1,
            0.0 to -180.1,
            Double.NaN to 0.0,
            0.0 to Double.POSITIVE_INFINITY,
        ).forEach { (lat, lng) ->
            assertFailsWith<IllegalArgumentException> { Place("invalid", "Invalid", lat, lng) }
        }
    }
}
